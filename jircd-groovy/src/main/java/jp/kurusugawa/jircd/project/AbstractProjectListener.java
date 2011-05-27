package jp.kurusugawa.jircd.project;

import java.io.PrintWriter;

import jircd.irc.ConnectedEntity;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;

import org.apache.log4j.Logger;

public abstract class AbstractProjectListener implements ProjectListener {
	private static final Logger LOG = Logger.getLogger(AbstractProjectListener.class);

	private ProjectChannel mChannel;

	public void initialize(ProjectChannel aChannel) {
		mChannel = aChannel;
	}

	protected Logger getLogger() {
		return LOG;
	}

	public void addUser(ProjectChannel aChannel, User aUser) {
	}

	public void createChannel(ProjectChannel aChannel) {
	}

	public void destroyChannel(ProjectChannel aChannel) {
	}

	public void removeUser(ProjectChannel aChannel, User aUser) {
	}

	public void sendMessage(ProjectChannel aChannel, Message aMessage, RegisteredEntity aExcluded) {
	}

	public void setTopic(ProjectChannel aChannel, User aUser, String aNewTopic) {
	}

	protected PrintWriter getWriter(final ConnectedEntity aEntity) {
		return new PrintWriter(System.out) {
			@Override
			public void write(char[] aBuf, int aOff, int aLen) {
				super.write(aBuf, aOff, aLen);
				responseMessage(aEntity, new String(aBuf, aOff, aLen));
			}
		};
	}

	protected void responseMessage(ConnectedEntity aEntity, String aText) {
		Message tMessage = new Message(aEntity, "PRIVMSG", mChannel);
		tMessage.appendLastParameter(aText);
		aEntity.send(tMessage);
	}

	protected void responseError(ConnectedEntity aEntity, String aDescription, Throwable aThrowable) {
		getLogger().error(aDescription, aThrowable);
		responseNoticeMessage(aEntity, aDescription);
		responseNoticeMessage(aEntity, aThrowable.toString());

		StackTraceElement[] tStackTrace = aThrowable.getStackTrace();
		for (StackTraceElement tElement : tStackTrace) {
			responseInfoMessage(aEntity, tElement.toString());
		}
	}

	protected void responseNoticeMessage(ConnectedEntity aEntity, String aText) {
		Message tMessage = new Message(aEntity, "NOTICE", mChannel);
		tMessage.appendLastParameter(aText);
		aEntity.send(tMessage);
	}

	protected void responseError(ConnectedEntity aEntity, String aDescription) {
		getLogger().error(aDescription);
		responseInfoMessage(aEntity, aDescription);
	}

	protected void responseInfoMessage(ConnectedEntity aEntity, String aText) {
		Message tMessage = new Message(Constants.RPL_INFO, aEntity);
		tMessage.appendLastParameter(aText);
		aEntity.send(tMessage);
	}
}
