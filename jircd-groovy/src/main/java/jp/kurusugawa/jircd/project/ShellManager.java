package jp.kurusugawa.jircd.project;

import groovy.lang.Binding;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jircd.irc.ConnectedEntity;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jp.kurusugawa.jircd.IRCDaemon;
import jp.kurusugawa.jircd.JobManager;
import jp.kurusugawa.jircd.JobManager.Job;
import jp.kurusugawa.jircd.groovy.Shell;

import org.apache.log4j.Logger;

public class ShellManager extends AbstractProjectListener {
	private static final Logger LOG = Logger.getLogger(ShellManager.class);

	private final Map<ConnectedEntity, StringBuilder> mScripts;

	ShellManager(ProjectChannel aChannel) {
		mScripts = new ConcurrentHashMap<ConnectedEntity, StringBuilder>();
	}

	@Override
	public void initialize(ProjectChannel aChannel) {
		super.initialize(aChannel);
	}

	@Override
	public void sendMessage(ProjectChannel aChannel, Message aMessage, RegisteredEntity aExcluded) {
		if (aMessage.getParameterCount() != 2) {
			return;
		}

		String tText = aMessage.getParameter(1);

		ConnectedEntity tConnectedEntity = IRCDaemon.getCurrentConnectedEntity();
		if (!(tConnectedEntity instanceof ProjectUser)) {
			return;
		}

		ProjectUser tUser = (ProjectUser) tConnectedEntity;
		synchronized (tUser) {
			StringBuilder tBuilder = mScripts.get(tUser);
			if (tBuilder == null) {
				if (tText.startsWith(".")) {
					evaluate(aChannel, tText.substring(1));
				} else if (tText.startsWith("{{{")) {
					mScripts.put(tUser, new StringBuilder());
					tUser.getEntityContext().getWriter().println("multi statement mode");
				}
			} else if (tText.equals("}}}")) {
				evaluate(aChannel, new String(tBuilder));
				mScripts.remove(tUser);
			} else {
				if (tBuilder.length() + tText.length() > 65535) {
					mScripts.remove(tUser);
					throw new TooLargeScriptException("too large script size(=" + tBuilder.length() + ")");
				}
				tBuilder.append(tText).append('\n');
			}
		}
	}

	private static class TooLargeScriptException extends RuntimeException {
		private static final long serialVersionUID = -9145902794279324247L;

		private TooLargeScriptException(String aMessage) {
			super(aMessage);
		}
	}

	public static class ScriptJob extends JobManager.Job {
		private final String mScript;
		private final Shell mShell;

		private ScriptJob(ProjectUser aOwner, Shell aShell, String aScript) {
			super(aOwner);
			mShell = aShell;
			mScript = aScript;
		}

		protected void process() throws Throwable {
			try {
				Object tResult = mShell.evaluate(mScript);
				if (tResult != null) {
					mShell.getWriter().println(tResult.toString());
					mShell.getWriter().flush();
				}
			} catch (Throwable t) {
				mShell.getWriter().println(t.toString());
				mShell.getWriter().flush();
				t.printStackTrace(mShell.getErrorWriter());
				mShell.getErrorWriter().flush();
				throw t;
			}
		}

		@Override
		public String toString() {
			StringBuilder tBuffer = new StringBuilder(super.toString());
			tBuffer.append('\t');
			String tScript = mScript;
			if (tScript.length() > 64) {
				tScript = tScript.substring(0, 63);
			}
			tBuffer.append(tScript.replaceAll("[\r\n\t]", " "));
			tBuffer.append("\r\n");
			return new String(tBuffer);
		}

		public Shell getShell() {
			return mShell;
		}

		public String getScript() {
			return mScript;
		}
	}

	private void evaluate(ProjectChannel aChannel, String aScript) {
		ConnectedEntity tConnectedEntity = IRCDaemon.getCurrentConnectedEntity();
		ProjectUser tProjectUser = (ProjectUser) tConnectedEntity;
		Shell tShell = new Shell();
		tShell.getContext().setVariable("user", tProjectUser.getEntityContext().getVariables());
		Binding tContext = tShell.getContext();
		tContext.setVariable("project", aChannel.getEntityContext().getVariables());
		tShell.setWriter(aChannel.getEntityContext().getWriter());
		tShell.setErrorWriter(ProjectEntityContext.getCurrentEntityContext().getErrorWriter());

		Job tJob = new ScriptJob(tProjectUser, tShell, aScript);
		((JobManager) IRCDaemon.get().getService(JobManager.class.getSimpleName())).execute(tJob);
	}
}