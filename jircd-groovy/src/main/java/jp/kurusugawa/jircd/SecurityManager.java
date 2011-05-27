package jp.kurusugawa.jircd;

import jp.kurusugawa.jircd.IRCDaemon.Service;
import jp.kurusugawa.jircd.project.ProjectUser;

public interface SecurityManager extends Service {
	public static final String SERVICE_NAME = "SecurityManager";

	public boolean authenticate(ProjectUser aUser);

	public boolean isProjectMember(ProjectUser aUser, String aProjectName);
}