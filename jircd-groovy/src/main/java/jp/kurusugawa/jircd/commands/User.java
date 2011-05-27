package jp.kurusugawa.jircd.commands;

import jircd.jIRCdMBean;
import jircd.irc.ConnectedEntity;
import jircd.irc.Message;
import jircd.irc.UnregisteredEntity;
import jircd.irc.Util;
import jircd.irc_p10.commands.UserCommand;
import jp.kurusugawa.jircd.IRCDaemon;
import jp.kurusugawa.jircd.SecurityManager;
import jp.kurusugawa.jircd.project.ProjectEntityContext;
import jp.kurusugawa.jircd.project.ProjectUser;

import org.apache.log4j.Logger;

public class User extends UserCommand {
	private static Logger LOG = Logger.getLogger(User.class);

	public User(jIRCdMBean aJircd) {
		super(aJircd);
	}

	@Override
	protected jircd.irc.User createUser(final UnregisteredEntity aUnregisteredEntity, final String aUsername, String aDescription) {
		ProjectUser tProjectUser = null;
		try {
			tProjectUser = new ProjectUser(aUnregisteredEntity, aUsername, aDescription);
			IRCDaemon.setCurrentConnectedEntity(tProjectUser);
			if (((SecurityManager) IRCDaemon.get().getService(SecurityManager.SERVICE_NAME)).authenticate(tProjectUser)) {
				tProjectUser.getEntityContext().fireInitialize();
				return tProjectUser;
			} else {
				Message tMessage = new Message("464", tProjectUser);
				tMessage.appendLastParameter(Util.getResourceString(tProjectUser, "ERR_PASSWDMISMATCH"));
				tProjectUser.send(tMessage);
				tProjectUser.disconnect(Util.getResourceString(tProjectUser, "ERR_PASSWDMISMATCH"));
				return null;
			}
		} catch (Exception e) {
			ConnectedEntity tConnectedEntity = tProjectUser == null ? aUnregisteredEntity : tProjectUser;
			LOG.warn("aUserName(=" + aUsername + ")", e);
			Message tMessage = new Message("465", tConnectedEntity);
			tMessage.appendParameter(aUsername);
			tMessage.appendLastParameter(Util.getResourceString(tConnectedEntity, "ERR_YOUREBANNEDCREEP"));
			tConnectedEntity.send(tMessage);
			e.printStackTrace(new ProjectEntityContext(tConnectedEntity).getErrorWriter());
			tConnectedEntity.disconnect(Util.getResourceString(tConnectedEntity, "ERR_YOUREBANNEDCREEP"));
			return null;
		}
	}
}