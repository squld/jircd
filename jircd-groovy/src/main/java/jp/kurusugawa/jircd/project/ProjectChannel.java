package jp.kurusugawa.jircd.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jircd.irc.Channel;
import jircd.irc.Message;
import jircd.irc.Network;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;

public class ProjectChannel extends Channel implements ProjectEntity {
	private final List<ProjectListener> mListeners;

	private final MuticastListener mMulticaster;

	private final ProjectEntityContext mEntityContext;

	private final String mProjectName;

	public static class ChannelName {
		private final char mType;
		private final String mName;
		private final String mProjectName;
		private final String mSubName;

		public ChannelName(String aChannelName) {
			mName = aChannelName;
			mType = aChannelName.charAt(0);
			String tChannelNameWithoutType = aChannelName.substring(1);
			int tSeparatorIndex = tChannelNameWithoutType.indexOf('.');
			if (tSeparatorIndex > 0) {
				mProjectName = tChannelNameWithoutType.substring(0, tSeparatorIndex);
				mSubName = tChannelNameWithoutType.substring(tSeparatorIndex + 1);
			} else {
				mProjectName = tChannelNameWithoutType;
				mSubName = null;
			}
		}

		public char getType() {
			return mType;
		}

		public String getProjectName() {
			return mProjectName;
		}

		public String getSubName() {
			return mSubName;
		}

		@Override
		public String toString() {
			return mName;
		}

		@Override
		public boolean equals(Object aObj) {
			return mName.equals(aObj);
		}

		@Override
		public int hashCode() {
			return mName.hashCode();
		}
	}

	public ProjectChannel(ChannelName aName, Network aNetwork) {
		super(aName.toString(), aNetwork);

		ProjectEntityContext tProjectEntityContext = null;
		try {
			tProjectEntityContext = new ProjectEntityContext(this);
			mProjectName = aName.getProjectName();
			mEntityContext = tProjectEntityContext;
			mListeners = new ProjectListenerList();
			mMulticaster = new MuticastListener(mListeners);
			mMulticaster.initialize(this);
			mMulticaster.createChannel(this);
			mListeners.add(new ShellManager(this));

			mEntityContext.getVariables().put("name", getName());
			mEntityContext.getVariables().put("projectName", getProjecName());
			mEntityContext.getVariables().put("listeners", mListeners);
			mEntityContext.getVariables().put("type", "project");
			mEntityContext.fireInitialize();
		} catch (Throwable e) {
			this.remove();
			if (e instanceof Error) {
				throw (Error) e;
			}

			if (tProjectEntityContext == null) {
				throw new RuntimeException("channel(=" + aName + ")", e);
			}
			throw new RuntimeException("channel(=" + aName + ")", e);
		}
	}

	private class ProjectListenerList extends ArrayList<ProjectListener> {
		private static final long serialVersionUID = 4241292002327805899L;

		@Override
		public void add(int aIndex, ProjectListener aElement) {
			super.add(aIndex, aElement);
			aElement.initialize(ProjectChannel.this);
		}

		@Override
		public boolean add(ProjectListener aElement) {
			boolean tResult = super.add(aElement);
			aElement.initialize(ProjectChannel.this);
			return tResult;
		}

		@Override
		public boolean addAll(Collection<? extends ProjectListener> aListeners) {
			boolean tResult = super.addAll(aListeners);
			for (ProjectListener tListener : aListeners) {
				tListener.initialize(ProjectChannel.this);
			}
			return tResult;
		}

		@Override
		public boolean addAll(int aIndex, Collection<? extends ProjectListener> aListeners) {
			boolean tResult = super.addAll(aIndex, aListeners);
			for (ProjectListener tListener : aListeners) {
				tListener.initialize(ProjectChannel.this);
			}
			return tResult;
		}

		@Override
		public String toString() {
			StringBuilder tBuilder = new StringBuilder();
			for (int i = 0; i < this.size(); i++) {
				tBuilder.append(i).append(" -> ");
				ProjectListener tProjectListener = this.get(i);
				tBuilder.append(tProjectListener);
				tBuilder.append("\n");
			}
			return new String(tBuilder);
		}
	}

	/**
	 * Sends a message to this channel, excluding a specified user.
	 */
	@Override
	public void send(Message message, RegisteredEntity excluded) {
		// jirc
		if (excluded == null) {
			sendLocal(message, excluded);
		} else {
			super.send(message, excluded);
		}
	}

	@Override
	protected void remove() {
		super.remove();
		mMulticaster.destroyChannel(this);
	}

	@Override
	public void addUser(User aUser) {
		super.addUser(aUser);
		mMulticaster.addUser(this, aUser);
	}

	@Override
	public void removeUser(User aUser) {
		super.removeUser(aUser);
		mMulticaster.removeUser(this, aUser);
	}

	@Override
	public void setTopic(User aUser, String aNewTopic) {
		super.setTopic(aUser, aNewTopic);
		mMulticaster.setTopic(this, aUser, aNewTopic);
	}

	@Override
	public void sendLocal(Message aMessage, RegisteredEntity aExcluded) {
		super.sendLocal(aMessage, aExcluded);
		mMulticaster.sendMessage(this, aMessage, aExcluded);
	}

	public ProjectEntityContext getEntityContext() {
		return mEntityContext;
	}

	public String getProjecName() {
		return mProjectName;
	}
}
