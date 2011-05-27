package jp.kurusugawa.jircd.project;

import groovy.lang.GroovyShell;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import jircd.irc.Channel;
import jircd.irc.ConnectedEntity;
import jircd.irc.Entity;
import jircd.irc.Message;
import jp.kurusugawa.jircd.IRCDaemon;

public class ProjectEntityContext implements ProjectEntity.Context {
	private static final Map<ConnectedEntity, ProjectEntityContext> ENTITY_CONTEXTES;
	private static final ReentrantLock ENTITY_CONTEXTES_LOCK;

	static {
		ENTITY_CONTEXTES_LOCK = new ReentrantLock();
		ENTITY_CONTEXTES = new WeakHashMap<ConnectedEntity, ProjectEntityContext>();
	}

	public static ProjectEntity.Context getCurrentEntityContext() {
		try {
			ENTITY_CONTEXTES_LOCK.lock();
			return ENTITY_CONTEXTES.get(IRCDaemon.getCurrentConnectedEntity());
		} finally {
			ENTITY_CONTEXTES_LOCK.unlock();
		}
	}

	private final Map<String, Object> mVariables;
	private final EntityWriter mWriter;

	private final EntityWriter mErrorWriter;

	public ProjectEntityContext(Entity aEntity) {
		mVariables = new ConcurrentHashMap<String, Object>();
		mWriter = new EntityWriter(System.out, aEntity) {
			Message createMessage(Entity aTargetEntity, boolean aTargetIsChannel) {
				Message tMessage;
				if (aTargetIsChannel) {
					tMessage = new Message(IRCDaemon.getCurrentConnectedEntity(), "NOTICE", (Channel) aTargetEntity);
				} else {
					tMessage = new Message(IRCDaemon.getCurrentConnectedEntity(), "NOTICE", (ConnectedEntity) aTargetEntity);
				}
				return tMessage;
			}
		};
		mErrorWriter = new EntityWriter(System.err, aEntity) {
			Message createMessage(Entity aTargetEntity, boolean aTargetIsChannel) {
				Message tMessage;
				tMessage = new Message(IRCDaemon.getCurrentConnectedEntity(), "PRIVMSG", IRCDaemon.getCurrentConnectedEntity());
				return tMessage;
			}
		};
		if (aEntity instanceof ConnectedEntity) {
			try {
				ENTITY_CONTEXTES_LOCK.lock();
				ENTITY_CONTEXTES.put((ConnectedEntity) aEntity, this);
			} finally {
				ENTITY_CONTEXTES_LOCK.unlock();
			}
		}
	}

	private static class FastURLClassLoader extends ClassLoader {
		private static class ProxyURLClassLoader extends URLClassLoader {
			ProxyURLClassLoader(URL[] aURLs) {
				super(aURLs);
			}

			public Class<?> findClass(String aName) throws ClassNotFoundException {
				return super.findClass(aName);
			}

			public String findLibrary(String aLibname) {
				return super.findLibrary(aLibname);
			}
		}

		private final ProxyURLClassLoader mSlowClassLoader;

		FastURLClassLoader(URL[] aURLs, ClassLoader aParent) {
			super(aParent);
			mSlowClassLoader = new ProxyURLClassLoader(aURLs);
		}

		@Override
		protected Class<?> findClass(String aName) throws ClassNotFoundException {
			Class<?> tClass = super.findClass(aName);
			if (tClass != null) {
				return tClass;
			}
			return mSlowClassLoader.findClass(aName);
		}

		@Override
		protected String findLibrary(String aLibraryName) {
			String tLibrary = super.findLibrary(aLibraryName);
			if (tLibrary != null) {
				return tLibrary;
			}
			return mSlowClassLoader.findLibrary(aLibraryName);
		}

		@Override
		protected URL findResource(String aName) {
			URL tResource = super.findResource(aName);
			if (tResource != null) {
				return tResource;
			}
			if (aName.endsWith(".groovy")) {
				int tLastIndexOfDot = aName.lastIndexOf('/');
				if (Character.isLowerCase(aName.charAt(tLastIndexOfDot + 1))) {
					return null;
				}
			}
			return mSlowClassLoader.findResource(aName);
		}

		@Override
		protected Enumeration<URL> findResources(String aName) throws IOException {
			Enumeration<URL> tResources = super.findResources(aName);
			if (tResources.hasMoreElements()) {
				return tResources;
			}
			return mSlowClassLoader.findResources(aName);
		}
	}

	private GroovyShell getBehaviorShell() throws MalformedURLException {
		URL tScriptRepositoryBaseURL = new URL(IRCDaemon.get().getProperty("groovy.baseurl"));
		ClassLoader tClassLoader = new FastURLClassLoader(new URL[] { tScriptRepositoryBaseURL }, ProjectEntityContext.class.getClassLoader());
		GroovyShell tGroovyShell = new GroovyShell(tClassLoader);
		tGroovyShell.setVariable("shell", tGroovyShell);
		tGroovyShell.setVariable("entity", mVariables);
		tGroovyShell.setProperty("out", mWriter);
		tGroovyShell.setProperty("err", mErrorWriter);
		return tGroovyShell;
	}

	public void fireInitialize() {
		try {
			GroovyShell tGroovyShell = getBehaviorShell();
			tGroovyShell.evaluate("Behavior.initialize(shell, entity);");
		} catch (Exception e) {
			throw new RuntimeException(getVariables().toString(), e);
		}
	}

	public void fireLoadPlugins() {
		try {
			GroovyShell tGroovyShell = getBehaviorShell();
			tGroovyShell.evaluate("Behavior.loadPlugins(shell, entity);");
		} catch (Exception e) {
			throw new RuntimeException(getVariables().toString(), e);
		}
	}

	public void fireFinalize() {
		try {
			GroovyShell tGroovyShell = getBehaviorShell();
			tGroovyShell.evaluate("Behavior.finalize(shell, entity);");
		} catch (Exception e) {
			throw new RuntimeException(getVariables().toString(), e);
		}
	}

	public Map<String, Object> getVariables() {
		return mVariables;
	}

	public PrintWriter getErrorWriter() {
		return mErrorWriter;
	}

	public PrintWriter getWriter() {
		return mWriter;
	}

	private static abstract class EntityWriter extends PrintWriter {
		private final Entity mTargetEntity;
		private final boolean mTargetIsChannel;
		private final boolean mTargetIsConnectedEntity;

		public EntityWriter(PrintStream aAdapteeStream, Entity aTargetEntity) {
			super(aAdapteeStream);
			mTargetEntity = aTargetEntity;

			mTargetIsChannel = aTargetEntity instanceof Channel;
			mTargetIsConnectedEntity = aTargetEntity instanceof ConnectedEntity;
			if (!(mTargetIsChannel || mTargetIsConnectedEntity)) {
				throw new IllegalArgumentException("entity(=" + aTargetEntity + ") is not a instance of Channel or Entity.");
			}
		}

		@Override
		public void write(String aString, int aOffset, int aLength) {
			super.write(aString, aOffset, aLength);
			write(aString.substring(aOffset, aOffset + aLength));
		}

		@Override
		public void write(char[] aBuffer, int aOffset, int aLength) {
			super.write(aBuffer, aOffset, aLength);
			write(new String(aBuffer, aOffset, aLength));
		}

		@Override
		public void write(String aText) {
			String[] tLines = aText.split("\n");
			for (String tLine : tLines) {
				Message tMessage = createMessage(mTargetEntity, mTargetIsChannel);
				tMessage.appendLastParameter(tLine);
				mTargetEntity.send(tMessage);
			}
		}

		abstract Message createMessage(Entity aTargetEntity, boolean aTargetIsChannel);
	}
}
