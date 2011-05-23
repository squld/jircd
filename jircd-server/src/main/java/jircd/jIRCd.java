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

package jircd;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Policy;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
import jircd.irc_p10.Server_P10;

import org.apache.log4j.Logger;

/**
 * @author thaveman
 * @author markhale
 */
public class jIRCd implements jIRCdMBean {
    private static final Logger logger = Logger.getLogger(jIRCd.class);
    
    // version information
    public static final int VERSION_MAJOR = 0;
    public static final int VERSION_MINOR = 7;
    public static final int VERSION_PATCH = 0;
    public static final String VERSION_URL = "http://j-ircd.sourceforge.net/";
    
    private static final String PLUGIN_PATH = "plugins";
    
    protected final Network network;
    /** this server. */
    protected final Server thisServer;
    
    /** set of server socket Listeners. */
    protected final Set listeners = Collections.synchronizedSet(new HashSet());
    protected final ConnectionManager links = new ConnectionManager();
    /** commands */
    private final Map cmdCtxs = Collections.synchronizedMap(new HashMap(131));
    
    // configuration and informational information
    private long startTime = -1;
    private final String configURL;
    private final Properties settings = new Properties();
    
    private final ExecutorService listenerThreadPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture pingFuture;
    
    public static void main(String[] args) {
        // program must be executed using: jircd.jIRCd <configuration file>
        if ((args == null) || (args.length < 1)) {
            System.err.println("Usage: jircd.jIRCd <configuration file>");
            System.exit(1);
        }
        final String configURL = args[0];
        
        printBanner(System.out);
        
        jIRCd jircd = null;
        // attempt to read the specified configuration file
        try {
            jircd = new jIRCd(configURL);
        } catch (IOException ioe) {
            System.err.println(ioe + " occured while reading configuration file.");
            System.exit(1);
        }
        
        jircd.start();
        
        // now just hang out forever
        System.out.println("Press enter to terminate.");
        try {
            System.in.read();
        } catch (IOException e) {
            System.err.println(e + " occured while waiting for program termination.");
            System.exit(1);
        }
        
        System.out.println("Shutting down...");
        jircd.stop();
        jircd.listenerThreadPool.shutdown();
        jircd.timer.shutdown();
        //System.exit(0);
    }
    
    public static void printBanner(PrintStream out) {
        out.println();
        out.println("Welcome to jIRCd: The world's first full-featured multiplatform Java-powered IRC"
                + " server. Created and maintained by Tyrel L. Haveman and Mark Hale.");
        out.println("jIRCd uses a TCP protocol based on the Internet Relay Chat Protocol (RFC 1459), "
                + "by Jarkko Oikarinen (May 1993). Portions may also be based on the IRC version 2 "
                + "protocol (RFC 2810, RFC 2811, RFC 2812, RFC 2813) by C. Kalt (April 2000).");
        out.println("Please visit "+VERSION_URL+" for the latest information and releases.");
        out.println();
    }
    
    public jIRCd(String configURL) throws IOException {
        this.configURL = configURL;
        settings.setProperty("jircd.configURL", configURL);
        settings.setProperty("jircd.version.name", getVersion());
        settings.setProperty("jircd.version.url", VERSION_URL);
        
        loadConfiguration();
        String networkName = settings.getProperty("jircd.networkName", "dumbnet");
        if(Util.isIRCString(networkName)) {
            logger.info("Network name: "+networkName);
        } else {
            logger.warn("Invalid network name");
            networkName = networkName.replace(' ', '-');
            logger.info("Generated network name: "+networkName);
        }
        network = new Network(networkName);
        String serverName = settings.getProperty("jircd.serverName", "dumb.admin");
        if(serverName.indexOf('.') != -1) {
            logger.info("Server name: "+serverName);
        } else {
            logger.warn("The server name should contain at least one dot, e.g. irc."+serverName);
            serverName = "irc."+serverName;
            logger.info("Generated server name: "+serverName);
        }
        final String desc = settings.getProperty("jircd.description", "dumb.admin");
        String tokenProperty = settings.getProperty("jircd.token");
        int token = 0;
        if(tokenProperty != null) {
            try {
                token = Integer.parseInt(tokenProperty);
            } catch(NumberFormatException nfe) {
                logger.warn("Invalid server token", nfe);
                tokenProperty = null;
            }
        }
        if(tokenProperty == null) {
            token = jircd.irc_p10.Util.randomServerToken();
            logger.info("Generated server token: "+token);
        }
        thisServer = new Server_P10(serverName, token, desc, network);
    }
    private void loadConfiguration() throws IOException {
        logger.info("Reading configuration file...");
        InputStream in = new URL(configURL).openStream();
        try {
            settings.load(in);
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
        cmdCtxs.clear();
        File pluginDir = new File(PLUGIN_PATH);
        File[] jarFiles = pluginDir.listFiles(new ExtensionFilenameFilter("jar"));
        if(jarFiles == null) {
            logger.warn("Plugin directory does not exist: "+pluginDir);
        } else {
            logger.info("Found "+jarFiles.length+" plugins in directory "+pluginDir);
            for(int i=0; i<jarFiles.length; i++) {
                File jarFile = jarFiles[i];
                try {
                    // create class loader for plugin
                    URLClassLoader loader = URLClassLoader.newInstance(new URL[] {jarFile.toURL()});
                    loadPlugin(new JarFile(jarFile), loader);
                } catch(IOException ioe) {
                    logger.warn("Could not load plugin "+jarFile, ioe);
                }
            }
        }
    }
    protected void loadPlugin(JarFile jar, ClassLoader loader) {
        logger.info("Searching plugin "+jar.getName()+"...");
        Enumeration entries = jar.entries();
        while(entries.hasMoreElements()) {
            JarEntry entry = (JarEntry) entries.nextElement();
            String name = entry.getName();
            if(name.endsWith(".class")) {
                final String className = name.substring(0, name.length()-6).replace('/', '.');
                try {
                    Class cls = loader.loadClass(className);
                    if(Command.class.isAssignableFrom(cls)) {
                        Command command;
                        try {
                            command = (Command) cls.newInstance();
                        } catch(InstantiationException ie) {
                            Constructor cnstr = cls.getConstructor(new Class[] {jIRCdMBean.class});
                            command = (Command) cnstr.newInstance(new Object[] {this});
                        }
                        final String cmdName = command.getName();
                        if(!Util.isCommandIdentifier(cmdName))
                            throw new RuntimeException("Invalid command name: "+cmdName);
                        CommandContext oldCtx = (CommandContext) cmdCtxs.get(cmdName.toUpperCase());
                        cmdCtxs.put(cmdName.toUpperCase(), new CommandContext(command));
                        logger.info("...installed "+cmdName+" ("+className+")");
                        if(oldCtx != null)
                            logger.info("......replaced "+oldCtx.getCommand().getClass().getName());
                    }
                } catch(Exception ex) {
                    logger.warn("Could not load class "+className, ex);
                }
            }
        }
    }
    public synchronized void reloadPolicy() {
        logger.info("Refreshing security policy");
        Policy.getPolicy().refresh();
    }
    private void startPings() {
        if(pingFuture == null || pingFuture.isDone()) {
            PingTask pingTask = new PingTask();
            final long pingInterval = Integer.parseInt(settings.getProperty("jircd.ping.interval", "5"));
            pingFuture = timer.scheduleWithFixedDelay(pingTask, 0, pingInterval, TimeUnit.SECONDS);
        }
    }
    private void stopPings() {
        if(pingFuture != null) {
            pingFuture.cancel(true);
            pingFuture = null;
        }
    }
    public synchronized void start() {
        logger.info(getVersion()+" starting...");
        startTime = System.currentTimeMillis();
        
        reloadPlugins();
        initListeners();
        startListeners();
        startPings();
    }
    private void initListeners() {
        for(Iterator iter = settings.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            if(key.startsWith("jircd.bind.")) {
                String addressPort = key.substring("jircd.bind.".length());
                int pos = addressPort.indexOf('#');
                String address;
                int port = Constants.DEFAULT_PORT;
                if(pos != -1) {
                    address = addressPort.substring(0, pos);
                    try {
                        port = Integer.parseInt(addressPort.substring(pos+1));
                    } catch(NumberFormatException nfe) {
                        logger.warn("Invalid port: "+port, nfe);
                        continue;
                    }
                } else {
                    address = addressPort;
                }
                String listenerName;
                String listenerParams;
                String value = (String) entry.getValue();
                pos = value.indexOf(',');
                if(pos != -1) {
                    listenerName = value.substring(0, pos);
                    listenerParams = value.substring(pos+1);
                } else {
                    listenerName = value;
                    listenerParams = "";
                }
                try {
                    Class listenerClass = Class.forName(listenerName);
                    Class[] signature = {jIRCdMBean.class, String.class, Integer.TYPE, ExecutorService.class, String.class};
                    Constructor cnstr = listenerClass.getConstructor(signature);
                    Object[] args = {this, address, new Integer(port), listenerThreadPool, listenerParams};
                    Listener listener = (Listener) cnstr.newInstance(args);
                    listeners.add(listener);
                } catch(ClassNotFoundException cnfe) {
                    logger.warn("Unrecognised listener class", cnfe);
                } catch(NoSuchMethodException nsme) {
                    logger.warn("Incompatible listener class", nsme);
                } catch(Exception e) {
                    logger.warn("Could not instantiate listener", e);
                }
            }
        }
    }
    private void startListeners() {
        logger.info("Binding to port(s)...");
        for(Iterator iter = listeners.iterator(); iter.hasNext();) {
            Listener listener = (Listener) iter.next();
            if (listener.bind()) {
                listener.start();
                logger.info("..." + listener.toString() + "...");
            } else {
                iter.remove();
                logger.warn("..." + listener.toString() + " (FAILED)...");
            }
        }
        logger.info("...complete");
    }
    /**
     * Stops this server.
     * All clients and server links are disconnected.
     */
    public synchronized void stop() {
        logger.info("Stopping...");
        stopPings();
        
        // prevent new incoming connections
        logger.info("Stopping all listener connections...");
        for(Iterator iter = listeners.iterator(); iter.hasNext();) {
            Listener listener = (Listener) iter.next();
            listener.stop();
        }
        
        // broadcast shutdown notice
        for(Iterator iter = thisServer.getUsers().iterator(); iter.hasNext(); ) {
            User user = (User) iter.next();
            Message message = new Message(thisServer, "NOTICE", user);
            message.appendLastParameter("WARNING: Server shut down by local console.");
            user.send(message);
        }
        
        // close all connections
        logger.info("Closing all listener connections...");
        for(Iterator iter = listeners.iterator(); iter.hasNext();) {
            Listener listener = (Listener) iter.next();
            disconnect(listener.getConnections());
            listener.close();
        }
        listeners.clear();
        logger.info("Closing all link connections...");
        disconnect(links.getConnections());
        
        startTime = -1;
    }
    private void disconnect(Set connections) {
        for(Iterator iter = connections.iterator(); iter.hasNext();) {
            Connection conn = (Connection) iter.next();
            conn.getHandler().getEntity().disconnect("Server shutdown");
        }
    }
    
    public final Set getListeners() {
        return listeners;
    }
    public final ConnectionManager getLinks() {
        return links;
    }
    
    /**
     * Security checks are performed on all commands.
     */
    public final void invokeCommand(final Message message) {
        final ConnectedEntity from = message.getSender();
        final String cmdName = message.getCommand();
        // find command
        final CommandContext ctx = (CommandContext) cmdCtxs.get(cmdName.toUpperCase());
        if (ctx == null) {
            // unknown command
            Util.sendUnknownCommandError(from, cmdName);
            logger.debug("Unknown command: " + message.toString());
            return;
        }
        
        final Command command = ctx.getCommand();
        
        if (message.getParameterCount() < command.getMinimumParameterCount()) {
            // too few parameters
            Util.sendNeedMoreParamsError(from, cmdName);
            return;
        }
        
        String[] params = new String[message.getParameterCount()];
        for(int i=0; i<params.length; i++)
            params[i] = message.getParameter(i);
        
        // HERE WE GO!!!!!!!!!
        try {
            Util.checkCommandPermission(command);
            if(from instanceof UnregisteredEntity) {
                if(command instanceof RegistrationCommand) {
                    ((RegistrationCommand) command).invoke((UnregisteredEntity) from, params);
                    ctx.commandInvoked();
                } else {
                    Util.sendNotRegisteredError((UnregisteredEntity) from);
                    logger.debug("Unregistered user "+from+" attempted to use command "+message.toString());
                }
            } else {
                command.invoke((RegisteredEntity) from, params);
                ctx.commandInvoked();
            }
        } catch (RuntimeException e) {
            logger.warn("Error invoking method in " + command.getClass() + " for command " + cmdName, e);
        }
    }
    
    public final CommandContext getCommandContext(String name) {
        return (CommandContext) cmdCtxs.get(name.toUpperCase());
    }
    public final Set getCommandContexts() {
        return new HashSet(cmdCtxs.values());
    }
    
    public final String getProperty(String key) {
        return settings.getProperty(key);
    }
    public final String getProperty(String key, String defaultValue) {
        return settings.getProperty(key, defaultValue);
    }
    
    /**
     * Returns the server uptime in milliseconds.
     */
    public final long getUptimeMillis() {
        return (startTime == -1) ? 0 : (System.currentTimeMillis() - startTime);
    }
    public final long getStartTimeMillis() {
        return startTime;
    }
    public Server getServer() {
        return thisServer;
    }
    public int getVisibleUserCount() {
        return thisServer.getUserCount(User.UMODE_INVISIBLE, false);
    }
    public int getInvisibleUserCount() {
        return thisServer.getUserCount(User.UMODE_INVISIBLE, true);
    }
    /**
     * Returns the number of visible users on the network.
     */
    public int getNetworkVisibleUserCount() {
        return network.getUserCount(User.UMODE_INVISIBLE, false);
    }
    /**
     * Returns the number of invisible users on the network.
     */
    public int getNetworkInvisibleUserCount() {
        return network.getUserCount(User.UMODE_INVISIBLE, true);
    }
    public int getChannelCount() {
        return network.getChannels().size();
    }
    public int getServerCount() {
        return network.getServers().size();
    }
    
    public String getVersion() {
        return "jIRCd-" + VERSION_MAJOR + '.' + VERSION_MINOR + '.' + VERSION_PATCH;
    }
    
    public String toString() {
        return "jIRCd";
    }
    
    class PingTask implements Runnable {
        public void run() {
            // PING? PONG!
            for(Iterator iter = listeners.iterator(); iter.hasNext();) {
                Listener listener = (Listener) iter.next();
                ping(listener.getConnections());
            }
            ping(links.getConnections());
        }
        private void ping(Set connections) {
            for(Iterator iter = connections.iterator(); iter.hasNext(); ) {
                Connection connection = (Connection) iter.next();
                Connection.Handler handler = connection.getHandler();
                if(!handler.ping()) {
                    // should have had PONG a long time ago, timeout please!
                    handler.getEntity().disconnect("Ping timeout");
                }
            }
        }
    }
    
    protected static class ExtensionFilenameFilter implements FilenameFilter {
        private final String extension;
        public ExtensionFilenameFilter(String ext) {
            extension = "."+ext;
        }
        public boolean accept(File dir, String name) {
            return name.endsWith(extension);
        }
    }
}
