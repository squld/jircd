/*
 * jIRCd - Java Internet Relay Chat Daemon
 * Copyright 2003 Tyrel L. Haveman <tyrel@haveman.net>
 *
 * This file is part of jIRCd.
 *
 * jIRCd is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * jIRCd is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with jIRCd; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package jp.kurusugawa.jircd;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.Policy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jircd.jIRCdMBean;
import jircd.irc.Command;
import jircd.irc.CommandContext;
import jircd.irc.ConnectedEntity;
import jircd.irc.Connection;
import jircd.irc.ConnectionManager;
import jircd.irc.Constants;
import jircd.irc.Listener;
import jircd.irc.Message;
import jircd.irc.Network;
import jircd.irc.RegisteredEntity;
import jircd.irc.RegistrationCommand;
import jircd.irc.Server;
import jircd.irc.UnregisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;
import jircd.irc.commands.Admin;
import jircd.irc.commands.Away;
import jircd.irc.commands.Connect;
import jircd.irc.commands.Die;
import jircd.irc.commands.Info;
import jircd.irc.commands.Invite;
import jircd.irc.commands.IsOn;
import jircd.irc.commands.Kick;
import jircd.irc.commands.Kill;
import jircd.irc.commands.LUsers;
import jircd.irc.commands.Links;
import jircd.irc.commands.Mode;
import jircd.irc.commands.Motd;
import jircd.irc.commands.NJoin;
import jircd.irc.commands.Names;
import jircd.irc.commands.Nick;
import jircd.irc.commands.Notice;
import jircd.irc.commands.Oper;
import jircd.irc.commands.Part;
import jircd.irc.commands.Pass;
import jircd.irc.commands.Ping;
import jircd.irc.commands.Pong;
import jircd.irc.commands.Quit;
import jircd.irc.commands.Rehash;
import jircd.irc.commands.Restart;
import jircd.irc.commands.SQuit;
import jircd.irc.commands.ServerCommand;
import jircd.irc.commands.Stats;
import jircd.irc.commands.Time;
import jircd.irc.commands.Topic;
import jircd.irc.commands.UserHost;
import jircd.irc.commands.Version;
import jircd.irc.commands.Wallops;
import jircd.irc.commands.Who;
import jp.kurusugawa.jircd.commands.Join;
import jp.kurusugawa.jircd.commands.PrivMsg;
import jp.kurusugawa.jircd.commands.WhoIs;
import jp.kurusugawa.jircd.project.ProjectServer;

import org.apache.log4j.Logger;

@SuppressWarnings("unchecked")
public class IRCDaemon implements jIRCdMBean {
	private static final Logger LOG = Logger.getLogger(IRCDaemon.class);
	private static final WeakHashMap<Thread, ConnectedEntity> THREAD_CONNECTED_ENTITY;
	private static final ReentrantLock THREAD_CONNECTED_ENTITY_LOCK;

	public static ConnectedEntity getCurrentConnectedEntity() {
		THREAD_CONNECTED_ENTITY_LOCK.lock();
		try {
			return THREAD_CONNECTED_ENTITY.get(Thread.currentThread());
		} finally {
			THREAD_CONNECTED_ENTITY_LOCK.unlock();
		}
	}

	public static void setCurrentConnectedEntity(ConnectedEntity aConnectedEntity) {
		setCurrentConnectedEntity(Thread.currentThread(), aConnectedEntity);
	}

	static void setCurrentConnectedEntity(Thread aTargetThread, ConnectedEntity aConnectedEntity) {
		THREAD_CONNECTED_ENTITY_LOCK.lock();
		try {
			THREAD_CONNECTED_ENTITY.put(aTargetThread, aConnectedEntity);
		} finally {
			THREAD_CONNECTED_ENTITY_LOCK.unlock();
		}
	}

	static {
		THREAD_CONNECTED_ENTITY = new WeakHashMap<Thread, ConnectedEntity>();
		THREAD_CONNECTED_ENTITY_LOCK = new ReentrantLock();
		System.out.println("start base path: " + new File(".").getAbsolutePath());
	}

	private static IRCDaemon INSTANCE;

	public static IRCDaemon get() {
		return INSTANCE;
	}

	// version information
	public static final int VERSION_MAJOR = 0;

	public static final int VERSION_MINOR = 7;

	public static final int VERSION_PATCH = 0;

	public static final String VERSION_URL = "http://j-ircd.sourceforge.net/";

	protected final Network mNetwork;

	/** this server. */
	protected final ProjectServer mThisServer;

	/** set of server socket Listeners. */
	protected final Set<Listener> mListeners = Collections.synchronizedSet(new HashSet<Listener>());

	protected final ConnectionManager mLinks = new ConnectionManager();

	/** commands */
	private final Map<String, CommandContext> mCommands = new ConcurrentHashMap<String, CommandContext>(131);

	private final Map<String, Service> mServices = new HashMap<String, Service>();

	// configuration and informational information
	private long mStartTime = -1;

	private final String mConfigURL;

	private final Properties mSettings = new Properties();

	private final ExecutorService mListenerThreadPool;

	private final ScheduledExecutorService mTimer = Executors.newSingleThreadScheduledExecutor();

	private ScheduledFuture mPingFuture;

	public static void main(String[] args) {
		// program must be executed using: jircd.ircd <configuration file>
		if ((args == null) || (args.length < 1)) {
			LOG.fatal("Usage: jircd.ircd <configuration file>");
			return;
		}

		try {
			startup(args[0]);
		} catch (Exception e) {
			LOG.fatal(e + " occured while reading configuration file.", e);
			return;
		}

		// now just hang out forever
		System.out.println("Press enter to terminate.");
		try {
			System.in.read();
		} catch (IOException e) {
			System.err.println(e + " occured while waiting for program termination.");
			System.exit(1);
		}

		shutdown();
	}

	public static void startup(final String aConfigURL) throws IOException {
		printBanner();
		INSTANCE = null;

		// attempt to read the specified configuration file
		INSTANCE = new IRCDaemon(aConfigURL);
		try {
			INSTANCE.mThisServer.getEntityContext().fireInitialize();
		} catch (RuntimeException aCause) {
			throw new RuntimeException("May be Behavior.groovy is not found. please, check groovy.baseurl property.", aCause);
		}
		INSTANCE.start();
	}

	public static void shutdown() {
		LOG.warn("Shutting down...");
		try {
			INSTANCE.mThisServer.getEntityContext().fireFinalize();
		} catch (Exception e) {
			LOG.warn(e);
		}
		INSTANCE.stop();
		INSTANCE.mListenerThreadPool.shutdown();
		INSTANCE.mTimer.shutdown();
	}

	public static void printBanner() {
		LOG.info("Welcome to ircd: The world's first full-featured multiplatform Java-powered IRC" + " server. Created and maintained by Tyrel L. Haveman and Mark Hale.");
		LOG.info("ircd uses a TCP protocol based on the Internet Relay Chat Protocol (RFC 1459), " + "by Jarkko Oikarinen (May 1993). Portions may also be based on the IRC version 2 " + "protocol (RFC 2810, RFC 2811, RFC 2812, RFC 2813) by C. Kalt (April 2000).");
		LOG.info("Please visit " + VERSION_URL + " for the latest information and releases.");
	}

	public IRCDaemon(String configURL) throws IOException {
		this.mConfigURL = configURL;
		mSettings.setProperty("jircd.configURL", configURL);
		mSettings.setProperty("jircd.version.name", getVersion());
		mSettings.setProperty("jircd.version.url", VERSION_URL);

		loadConfiguration();
		String networkName = mSettings.getProperty("jircd.networkName", "dumbnet");
		if (Util.isIRCString(networkName)) {
			LOG.info("Network name: " + networkName);
		} else {
			LOG.warn("Invalid network name");
			networkName = networkName.replace(' ', '-');
			LOG.info("Generated network name: " + networkName);
		}
		mNetwork = new Network(networkName);
		String serverName = mSettings.getProperty("jircd.serverName", "dumb.admin");
		if (serverName.indexOf('.') != -1) {
			LOG.info("Server name: " + serverName);
		} else {
			LOG.warn("The server name should contain at least one dot, e.g. irc." + serverName);
			serverName = "irc." + serverName;
			LOG.info("Generated server name: " + serverName);
		}
		final String desc = mSettings.getProperty("jircd.description", "dumb.admin");
		String tokenProperty = mSettings.getProperty("jircd.token");
		int token = 0;
		if (tokenProperty != null) {
			try {
				token = Integer.parseInt(tokenProperty);
			} catch (NumberFormatException nfe) {
				LOG.warn("Invalid server token", nfe);
				tokenProperty = null;
			}
		}
		if (tokenProperty == null) {
			token = jircd.irc_p10.Util.randomServerToken();
			LOG.info("Generated server token: " + token);
		}

		int tCorePoolSize = Integer.parseInt(mSettings.getProperty("jircd.listener.executor.corePoolSize", "1"));
		int tMaxPoolSize = Integer.parseInt(mSettings.getProperty("jircd.listener.executor.maxPoolSize", "10"));
		int tKeepAliveTime = Integer.parseInt(mSettings.getProperty("jircd.listener.executor.keepAliveTimeMillis", "60000"));

		mListenerThreadPool = new ThreadPoolExecutor(tCorePoolSize, tMaxPoolSize, tKeepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		mThisServer = new ProjectServer(serverName, token, desc, mNetwork);
	}

	private void loadConfiguration() throws IOException {
		LOG.info("Reading configuration file...");
		InputStream in = new URL(mConfigURL).openStream();
		try {
			mSettings.load(in);
		} finally {
			in.close();
		}
	}

	public synchronized void reloadConfiguration() throws IOException {
		loadConfiguration();
		// update ping interval
		stopPings();
		startPings();
	}

	public synchronized void reloadPlugins() {
		unloadPlugins();

		INSTANCE.mThisServer.getEntityContext().fireLoadPlugins();

		// @formatter:off
		java.util.List<Class<?>> aClasses = Arrays.asList(
				Admin.class,
				Away.class,
				Connect.class,
				Die.class,
				Error.class,
				Info.class,
				Invite.class,
				IsOn.class,
				Join.class,
				Kick.class,
				Kill.class,
				Links.class,
				List.class,
				LUsers.class,
				Mode.class,
				Motd.class,
				Names.class,
				Nick.class,
				NJoin.class,
				Notice.class,
				Oper.class,
				Part.class,
				Pass.class,
				Ping.class,
				Pong.class,
				PrivMsg.class,
				Quit.class,
				Rehash.class,
				Restart.class,
				ServerCommand.class,
				SQuit.class,
				Stats.class,
				Time.class,
				Topic.class,
				jp.kurusugawa.jircd.commands.User.class,
				UserHost.class,
				Version.class,
				Wallops.class,
				Who.class,
				WhoIs.class
		);
		// @formatter:on

		for (Class<?> tClass : aClasses) {
			try {
				loadCommand(tClass);
			} catch (Exception e) {
				LOG.warn("Could not load class " + tClass.getCanonicalName(), e);
			}
		}
	}

	public synchronized void unloadPlugins() {
		for (Map.Entry<String, Service> tEntry : mServices.entrySet()) {
			LOG.info("unload: " + tEntry.getKey());
			try {
				tEntry.getValue().shutdown();
			} catch (Throwable t) {
				LOG.warn("ignore: ", t);
			}
		}
		mServices.clear();
		mCommands.clear();
	}

	public void loadService(Class<?> aClass) throws IllegalAccessException, NoSuchMethodException, InstantiationException, InvocationTargetException {
		if (!Service.class.isAssignableFrom(aClass)) {
			return;
		}

		Service tService;
		try {
			tService = (Service) aClass.newInstance();
		} catch (InstantiationException ie) {
			Constructor<?> tConstructor = aClass.getConstructor(new Class[] { jIRCdMBean.class });
			tService = (Service) tConstructor.newInstance(new Object[] { this });
		}

		final String tServiceName = tService.getName();

		Service tOldService = mServices.get(tServiceName);
		try {
			tService.startup(mSettings);
		} catch (Throwable t) {
			LOG.warn("...initialize failed " + tServiceName, t);
			return;
		}

		mServices.put(tServiceName, tService);
		LOG.info("...installed " + tServiceName + " (" + aClass.getCanonicalName() + ")");
		if (tOldService != null) {
			LOG.info("......replaced " + tOldService.getClass().getName());
		}
	}

	protected void loadPlugin(JarFile jar, ClassLoader loader) {
		LOG.info("Searching plugin " + jar.getName() + "...");
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = (JarEntry) entries.nextElement();
			String name = entry.getName();
			if (name.endsWith(".class")) {
				final String className = name.substring(0, name.length() - 6).replace('/', '.');
				try {
					Class<?> cls = loader.loadClass(className);
					loadCommand(cls);
				} catch (Exception ex) {
					LOG.warn("Could not load class " + className, ex);
				}
			}
		}
	}

	public void loadCommand(Class<?> aClass) throws IllegalAccessException, NoSuchMethodException, InstantiationException, InvocationTargetException {
		if (!Command.class.isAssignableFrom(aClass)) {
			return;
		}

		Command tCommand;
		try {
			tCommand = (Command) aClass.newInstance();
		} catch (InstantiationException ie) {
			Constructor<?> tConstructor = aClass.getConstructor(new Class[] { jIRCdMBean.class });
			tCommand = (Command) tConstructor.newInstance(new Object[] { this });
		}

		final String tCommandName = tCommand.getName();
		if (!Util.isCommandIdentifier(tCommandName)) {
			throw new RuntimeException("Invalid command name: " + tCommandName);
		}
		CommandContext tOldCommandContext = (CommandContext) mCommands.get(tCommandName.toUpperCase());
		mCommands.put(tCommandName.toUpperCase(), new CommandContext(tCommand));
		LOG.info("...installed " + tCommandName + " (" + aClass.getCanonicalName() + ")");
		if (tOldCommandContext != null) {
			LOG.info("......replaced " + tOldCommandContext.getCommand().getClass().getName());
		}
	}

	public synchronized void reloadPolicy() {
		LOG.info("Refreshing security policy");
		Policy.getPolicy().refresh();
	}

	private void startPings() {
		if (mPingFuture == null || mPingFuture.isDone()) {
			PingTask pingTask = new PingTask();
			final long pingInterval = Integer.parseInt(mSettings.getProperty("jircd.ping.interval", "5"));
			mPingFuture = mTimer.scheduleWithFixedDelay(pingTask, 0, pingInterval, TimeUnit.SECONDS);
		}
	}

	private void stopPings() {
		if (mPingFuture != null) {
			mPingFuture.cancel(true);
			mPingFuture = null;
		}
	}

	static interface Service {
		public void startup(Properties aSettings);

		public String getName();

		public void shutdown();
	}

	public synchronized void start() {
		LOG.info(getVersion() + " starting...");
		mStartTime = System.currentTimeMillis();

		reloadPlugins();
		initListeners();
		startListeners();
		startPings();
	}

	private void initListeners() {
		for (Iterator<Map.Entry<Object, Object>> iter = mSettings.entrySet().iterator(); iter.hasNext();) {
			Map.Entry<Object, Object> entry = iter.next();
			String key = (String) entry.getKey();
			if (key.startsWith("jircd.bind.")) {
				String addressPort = key.substring("jircd.bind.".length());
				int pos = addressPort.indexOf('#');
				String address;
				int port = Constants.DEFAULT_PORT;
				if (pos != -1) {
					address = addressPort.substring(0, pos);
					try {
						port = Integer.parseInt(addressPort.substring(pos + 1));
					} catch (NumberFormatException nfe) {
						LOG.warn("Invalid port: " + port, nfe);
						continue;
					}
				} else {
					address = addressPort;
				}
				String listenerName;
				String listenerParams;
				String value = (String) entry.getValue();
				pos = value.indexOf(',');
				if (pos != -1) {
					listenerName = value.substring(0, pos);
					listenerParams = value.substring(pos + 1);
				} else {
					listenerName = value;
					listenerParams = "";
				}
				try {
					LOG.info("load listener: " + listenerName);
					Class<?> listenerClass = Class.forName(listenerName);
					Class<?>[] signature = { jIRCdMBean.class, String.class, Integer.TYPE, ExecutorService.class, String.class };
					Constructor<?> cnstr = listenerClass.getConstructor(signature);
					Object[] args = { this, address, new Integer(port), mListenerThreadPool, listenerParams };
					Listener listener = (Listener) cnstr.newInstance(args);
					mListeners.add(listener);
				} catch (ClassNotFoundException cnfe) {
					LOG.warn("Unrecognised listener class", cnfe);
				} catch (NoSuchMethodException nsme) {
					LOG.warn("Incompatible listener class", nsme);
				} catch (Exception e) {
					LOG.warn("Could not instantiate listener", e);
				}
			}
		}
	}

	private void startListeners() {
		LOG.info("Binding to port(s)...");
		for (Iterator<Listener> iter = mListeners.iterator(); iter.hasNext();) {
			Listener listener = (Listener) iter.next();
			if (listener.bind()) {
				listener.start();
				LOG.info("..." + listener.toString() + "...");
			} else {
				iter.remove();
				LOG.warn("..." + listener.toString() + " (FAILED)...");
			}
		}
		LOG.info("...complete");
	}

	/**
	 * Stops this server. All clients and server links are disconnected.
	 */
	public synchronized void stop() {
		LOG.info("Stopping...");
		stopPings();

		unloadPlugins();

		// prevent new incoming connections
		LOG.info("Stopping all listener connections...");
		for (Iterator iter = mListeners.iterator(); iter.hasNext();) {
			Listener listener = (Listener) iter.next();
			listener.stop();
		}

		// broadcast shutdown notice
		for (Iterator iter = mThisServer.getUsers().iterator(); iter.hasNext();) {
			User user = (User) iter.next();
			Message message = new Message(mThisServer, "NOTICE", user);
			message.appendLastParameter("WARNING: Server shut down by local console.");
			user.send(message);
		}

		// close all connections
		LOG.info("Closing all listener connections...");
		for (Iterator iter = mListeners.iterator(); iter.hasNext();) {
			Listener listener = (Listener) iter.next();
			disconnect(listener.getConnections());
			listener.close();
		}
		mListeners.clear();
		LOG.info("Closing all link connections...");
		disconnect(mLinks.getConnections());

		mStartTime = -1;
	}

	private void disconnect(Set connections) {
		for (Iterator iter = connections.iterator(); iter.hasNext();) {
			Connection conn = (Connection) iter.next();
			conn.getHandler().getEntity().disconnect("Server shutdown");
		}
	}

	public final Set<Listener> getListeners() {
		return mListeners;
	}

	public final ConnectionManager getLinks() {
		return mLinks;
	}

	/**
	 * Security checks are performed on all commands.
	 */
	public final void invokeCommand(final Message aMessage) {
		final ConnectedEntity tFrom = aMessage.getSender();
		IRCDaemon.setCurrentConnectedEntity(tFrom);
		try {
			final String tCommandName = aMessage.getCommand();
			// find command
			final CommandContext tCommandContext = (CommandContext) mCommands.get(tCommandName.toUpperCase());
			if (tCommandContext == null) {
				// unknown command
				Util.sendUnknownCommandError(tFrom, tCommandName);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Unknown command: " + aMessage);
				}
				return;
			}

			final Command tCommand = tCommandContext.getCommand();

			if (aMessage.getParameterCount() < tCommand.getMinimumParameterCount()) {
				// too few parameters
				Util.sendNeedMoreParamsError(tFrom, tCommandName);
				return;
			}

			String[] tParams = new String[aMessage.getParameterCount()];
			for (int i = 0; i < tParams.length; i++) {
				tParams[i] = aMessage.getParameter(i);
			}

			// HERE WE GO!!!!!!!!!
			try {
				Util.checkCommandPermission(tCommand);
				if (tFrom instanceof UnregisteredEntity) {
					if (tCommand instanceof RegistrationCommand) {
						((RegistrationCommand) tCommand).invoke((UnregisteredEntity) tFrom, tParams);
						tCommandContext.commandInvoked();
					} else {
						Util.sendNotRegisteredError((UnregisteredEntity) tFrom);
						if (LOG.isDebugEnabled()) {
							LOG.debug("Unregistered user " + tFrom + " attempted to use command " + aMessage.toString());
						}
					}
				} else {
					tCommand.invoke((RegisteredEntity) tFrom, tParams);
					tCommandContext.commandInvoked();
				}
			} catch (RuntimeException e) {
				LOG.error("Exception invoking method in " + tCommand.getClass() + " for command " + tCommandName, e);
			} catch (Error e) {
				LOG.fatal("Error invoking method in " + tCommand.getClass() + " for command " + tCommandName, e);
			}
		} finally {
			IRCDaemon.setCurrentConnectedEntity(null);
		}
	}

	public final CommandContext getCommandContext(String name) {
		return (CommandContext) mCommands.get(name.toUpperCase());
	}

	public final Set<CommandContext> getCommandContexts() {
		return new HashSet<CommandContext>(mCommands.values());
	}

	public final Service getService(String aServiceName) {
		return mServices.get(aServiceName);
	}

	public final String getProperty(String key) {
		return mSettings.getProperty(key);
	}

	public final String getProperty(String key, String defaultValue) {
		return mSettings.getProperty(key, defaultValue);
	}

	/**
	 * Returns the server uptime in milliseconds.
	 */
	public final long getUptimeMillis() {
		return (mStartTime == -1) ? 0 : (System.currentTimeMillis() - mStartTime);
	}

	public final long getStartTimeMillis() {
		return mStartTime;
	}

	public Server getServer() {
		return mThisServer;
	}

	public int getVisibleUserCount() {
		return mThisServer.getUserCount(User.UMODE_INVISIBLE, false);
	}

	public int getInvisibleUserCount() {
		return mThisServer.getUserCount(User.UMODE_INVISIBLE, true);
	}

	/**
	 * Returns the number of visible users on the network.
	 */
	public int getNetworkVisibleUserCount() {
		return mNetwork.getUserCount(User.UMODE_INVISIBLE, false);
	}

	/**
	 * Returns the number of invisible users on the network.
	 */
	public int getNetworkInvisibleUserCount() {
		return mNetwork.getUserCount(User.UMODE_INVISIBLE, true);
	}

	public int getChannelCount() {
		return mNetwork.getChannels().size();
	}

	public int getServerCount() {
		return mNetwork.getServers().size();
	}

	public String getVersion() {
		return "ircd-" + VERSION_MAJOR + '.' + VERSION_MINOR + '.' + VERSION_PATCH;
	}

	public String toString() {
		return "ircd";
	}

	class PingTask implements Runnable {
		public void run() {
			// PING? PONG!
			for (Iterator iter = mListeners.iterator(); iter.hasNext();) {
				Listener listener = (Listener) iter.next();
				ping(listener.getConnections());
			}
			ping(mLinks.getConnections());
		}

		private void ping(Set connections) {
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				Connection connection = (Connection) iter.next();
				Connection.Handler handler = connection.getHandler();
				if (!handler.ping()) {
					// should have had PONG a long time ago, timeout please!
					handler.getEntity().disconnect("Ping timeout");
				}
			}
		}
	}

	protected static class ExtensionFilenameFilter implements FilenameFilter {
		private final String extension;

		public ExtensionFilenameFilter(String ext) {
			extension = "." + ext;
		}

		public boolean accept(File dir, String name) {
			return name.endsWith(extension);
		}
	}
}
