package jp.kurusugawa.jircd.project;

import java.util.Map;

import jircd.irc.Network;
import jircd.irc.UnregisteredEntity;
import jircd.irc_p10.Server_P10;

public class ProjectServer extends Server_P10 implements ProjectEntity {
	private final ProjectEntityContext mEntityContext;

	public ProjectServer(String aName, int aToken, String aDescription, Network aNetwork) {
		super(aName, aToken, aDescription, aNetwork);
		mEntityContext = createEntityContext();
	}

	public ProjectServer(UnregisteredEntity aUnk, int aToken, String aDescription) {
		super(aUnk, aToken, aDescription);
		mEntityContext = createEntityContext();
	}

	public ProjectServer(String aName, int aHopcount, int aToken, String aDescription, Server_P10 aRoute) {
		super(aName, aHopcount, aToken, aDescription, aRoute);
		mEntityContext = createEntityContext();
	}

	private ProjectEntityContext createEntityContext() {
		ProjectEntityContext tEntityContext = new ProjectEntityContext(this);
		Map<String, Object> tVariables = tEntityContext.getVariables();
		tVariables.put("name", getName());
		tVariables.put("locale", getLocale());
		tVariables.put("type", "server");

		return tEntityContext;
	}

	public ProjectEntityContext getEntityContext() {
		return mEntityContext;
	}
}
