package jp.kurusugawa.jircd;

import java.util.Properties;

import jp.kurusugawa.jircd.project.ProjectUser;

public class FreedomSecurityManager implements SecurityManager {
	@Override
	public boolean authenticate(ProjectUser aUser) {
		return true;
	}

	@Override
	public boolean isProjectMember(ProjectUser aUser, String aProjectName) {
		return true;
	}

	@Override
	public void startup(Properties aSettings) {
	}

	@Override
	public String getName() {
		return SecurityManager.SERVICE_NAME;
	}

	@Override
	public void shutdown() {
	}
}
