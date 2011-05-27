package jp.kurusugawa.jircd.project;

import java.util.List;

import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;

import org.apache.log4j.Logger;

class MuticastListener extends AbstractProjectListener {
	private static final Logger LOG = Logger.getLogger(MuticastListener.class);

	private final List<ProjectListener> mListeners;

	MuticastListener(List<ProjectListener> aTargets) {
		mListeners = aTargets;
	}

	@Override
	protected Logger getLogger() {
		return LOG;
	}

	@Override
	public void initialize(ProjectChannel aChannel) {
		super.initialize(aChannel);
	}

	@Override
	public void createChannel(ProjectChannel aChannel) {
		for (ProjectListener tListener : mListeners) {
			tListener.createChannel(aChannel);
		}
	}

	@Override
	public void destroyChannel(ProjectChannel aChannel) {
		for (ProjectListener tListener : mListeners) {
			tListener.destroyChannel(aChannel);
		}
	}

	@Override
	public void addUser(ProjectChannel aChannel, User aUser) {
		for (ProjectListener tListener : mListeners) {
			tListener.addUser(aChannel, aUser);
		}
	}

	@Override
	public void removeUser(ProjectChannel aChannel, User aUser) {
		for (ProjectListener tListener : mListeners) {
			tListener.removeUser(aChannel, aUser);
		}
	}

	@Override
	public void setTopic(ProjectChannel aChannel, User aUser, String aNewTopic) {
		for (ProjectListener tListener : mListeners) {
			tListener.setTopic(aChannel, aUser, aNewTopic);
		}
	}

	@Override
	public void sendMessage(ProjectChannel aChannel, Message aMessage, RegisteredEntity aExcluded) {
		for (ProjectListener tListener : mListeners) {
			tListener.sendMessage(aChannel, aMessage, aExcluded);
		}
	}
}
