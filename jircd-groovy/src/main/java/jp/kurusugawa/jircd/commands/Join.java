package jp.kurusugawa.jircd.commands;

import jircd.irc.Channel;
import jircd.irc.Network;
import jircd.irc.RegisteredEntity;
import jircd.irc.Util;
import jp.kurusugawa.jircd.IRCDaemon;
import jp.kurusugawa.jircd.SecurityManager;
import jp.kurusugawa.jircd.project.ProjectChannel;
import jp.kurusugawa.jircd.project.ProjectUser;

import org.apache.log4j.Logger;

/**
 * @author squld
 */
public class Join extends jircd.irc.commands.Join {
	private static final Logger LOG = Logger.getLogger(Join.class);

	public void invoke(RegisteredEntity aSourceEntity, String[] aParameters) {
		String[] tChannelNames = Util.split(aParameters[0], ',');
		String[] tKeys = (aParameters.length > 1 ? Util.split(aParameters[1], ',') : null);
		ProjectUser tProjectUser = (ProjectUser) aSourceEntity;
		for (int i = 0; i < tChannelNames.length; i++) {
			String tChannelName = tChannelNames[i];
			String tKey = (tKeys != null && i < tKeys.length ? tKeys[i] : null);
			join(tChannelName, tProjectUser, tKey);
		}
	}

	private void join(String aChannelName, ProjectUser aUser, String aKey) {
		if (!Util.isChannelIdentifier(aChannelName)) {
			Util.sendNoSuchChannelError(aUser, aChannelName);
			return;
		}

		ProjectChannel.ChannelName tChannelName = new ProjectChannel.ChannelName(aChannelName);
		if (!((SecurityManager) IRCDaemon.get().getService(SecurityManager.SERVICE_NAME)).isProjectMember(aUser, tChannelName.getProjectName())) {
			Util.sendNoSuchChannelError(aUser, aChannelName);
			return;
		}

		Network tNetwork = aUser.getServer().getNetwork();
		Channel tChannel = tNetwork.getChannel(aChannelName);
		if (tChannel == null) {
			tChannel = createChannel(tChannelName, tNetwork, aUser);
			if (tChannel == null) {
				return;
			}
		}
		tChannel.joinUser(aUser, aKey);
	}

	protected Channel createChannel(ProjectChannel.ChannelName aName, Network aNetwork, ProjectUser aUser) {
		try {
			return new ProjectChannel(aName, aNetwork);
		} catch (Exception e) {
			LOG.warn("name(=" + aName + ")", e);
			Util.sendNoSuchChannelError(aUser, aName.toString());
			e.printStackTrace(aUser.getEntityContext().getErrorWriter());
			return null;
		}
	}
}
