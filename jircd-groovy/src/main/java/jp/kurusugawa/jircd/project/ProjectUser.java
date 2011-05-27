package jp.kurusugawa.jircd.project;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import jircd.auth.PassiveCallbackHandler;
import jircd.irc.Connection;
import jircd.irc.UnregisteredEntity;
import jircd.irc_p10.Server_P10;
import jircd.irc_p10.User_P10;

public class ProjectUser extends User_P10 implements ProjectEntity {
	private final ProjectEntityContext mEntityContext;

	private final CallbackHandler mAuthenticationCallbackHandler;

	private static class AutenticationCallbackHandler implements CallbackHandler {
		private final String mUsername;
		private final char[] mPassword;

		private AutenticationCallbackHandler(UnregisteredEntity aUnregisteredEntity, String aUsername) {
			mUsername = aUsername;
			String[] tPasswords = aUnregisteredEntity.getPass();
			if (tPasswords != null && tPasswords.length > 0) {
				mPassword = tPasswords[0].toCharArray();
			} else {
				mPassword = new char[0];
			}
			aUnregisteredEntity.setPass(new String[] {});
		}

		public void handle(Callback[] aCallbacks) throws IOException, UnsupportedCallbackException {
			for (Callback tCallback : aCallbacks) {
				if (tCallback instanceof NameCallback) {
					NameCallback tNameCallback = (NameCallback) tCallback;
					tNameCallback.setName(mUsername);
				} else if (tCallback instanceof PasswordCallback) {
					PasswordCallback tPasswordCallback = (PasswordCallback) tCallback;
					tPasswordCallback.setPassword(mPassword);
				} else {
					throw new UnsupportedCallbackException(tCallback, "Unrecognized callback");
				}
			}
		}
	}

	public ProjectUser(UnregisteredEntity aUnregisteredEntity, String aUsername, String aDescription) throws LoginException {
		super(aUnregisteredEntity, aUsername, aDescription);

		Connection conn = aUnregisteredEntity.getHandler().getConnection();

		char[] mPassword;
		String[] tPasswords = aUnregisteredEntity.getPass();
		if (tPasswords != null && tPasswords.length > 0) {
			mPassword = tPasswords[0].toCharArray();
		} else {
			mPassword = new char[0];
		}

		PassiveCallbackHandler pch = new PassiveCallbackHandler(aUsername, mPassword, conn.getRemoteHost(), conn.getRemoteAddress(), conn.getLocalPort());
		mAuthenticationCallbackHandler = pch;
		mEntityContext = createEntityContext();
	}

	public ProjectUser(String aNickname, int aHopcount, int aToken, String aIdent, String aHostname, String aDescription, Server_P10 aServer) {
		super(aNickname, aHopcount, aToken, aIdent, aHostname, aDescription, aServer);
		throw new AssertionError();
	}

	private ProjectEntityContext createEntityContext() {
		ProjectEntityContext tEntityContext = new ProjectEntityContext(this);
		Map<String, Object> tVariables = tEntityContext.getVariables();
		tVariables.put("name", getName());
		tVariables.put("id", getIdent());
		tVariables.put("description", getDescription());
		tVariables.put("locale", getLocale());
		tVariables.put("type", "user");

		return tEntityContext;
	}

	public ProjectEntityContext getEntityContext() {
		return mEntityContext;
	}

	public CallbackHandler getAuthenticationCallbackHandler() {
		return mAuthenticationCallbackHandler;
	}
}