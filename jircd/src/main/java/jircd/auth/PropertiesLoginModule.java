/*
 * jIRCd - Java Internet Relay Chat Daemon
 * Copyright 2003 Tyrel L. Haveman <tyrel@haveman.net>
 *
 * This file is part of jIRCd.
 *
 * jIRCd is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * jIRCd is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with jIRCd; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package jircd.auth;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * Supported JAAS configuration settings:
 * debug
 * url
 * principal
 * digest
 * try_first_pass
 * use_first_pass
 * store_first_pass
 * clear_first_pass
 *
 * Property file format:
 * <name>=<password>
 * [<name>.host=<host pattern>]
 * [<name>.port=<port>]
 * @author markhale
 */
public class PropertiesLoginModule implements LoginModule {
	private static final String SHARED_NAME = "javax.security.auth.login.name";
	private static final String SHARED_PASSWORD = "javax.security.auth.login.password";

	// configuration
	private Subject subject;
	private CallbackHandler callbackHandler;
	private Map sharedState;
	private boolean debug = false;
	private boolean tryFirstPass = false;
	private boolean useFirstPass = false;
	private boolean storeFirstPass = false;
	private boolean clearFirstPass = false;
	private String url;
	private String principalClassName;
	private String digest;

	// state
	private String name;
	private boolean loginSucceeded = false;
	private boolean commitSucceeded = false;
	private Principal principal;

	public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = sharedState;
		debug = "true".equalsIgnoreCase((String) options.get("debug"));
		tryFirstPass = "true".equalsIgnoreCase((String) options.get("try_first_pass"));
		useFirstPass = "true".equalsIgnoreCase((String) options.get("use_first_pass"));
		storeFirstPass = "true".equalsIgnoreCase((String) options.get("store_first_pass"));
		clearFirstPass = "true".equalsIgnoreCase((String) options.get("clear_first_pass"));
		url = (String) options.get("url");
		principalClassName = (String) options.get("principal");
		digest = (String) options.get("digest");
	}
	public boolean login() throws LoginException {
		if(tryFirstPass) {
			name = (String) sharedState.get(SHARED_NAME);
			char[] passChars = (char[]) sharedState.get(SHARED_PASSWORD);
			try {
				authenticate(name, passChars);
				return true;
			} catch(LoginException le) {
				// shared authentication failed
				// fallback to using the callback handler
				log(le.toString());
			}
		} else if(useFirstPass) {
			name = (String) sharedState.get(SHARED_NAME);
			char[] passChars = (char[]) sharedState.get(SHARED_PASSWORD);
			authenticate(name, passChars);
			return true;
		}

		Callback[] callbacks = new Callback[3];
		callbacks[0] = new NameCallback("Enter username: ");
		callbacks[1] = new PasswordCallback("Enter password: ", false);
		callbacks[2] = new ConnectionCallback();
		try {
			callbackHandler.handle(callbacks);
		} catch(IOException ioe) {
			throw new LoginException(ioe.toString());
		} catch(UnsupportedCallbackException uce) {
			throw new LoginException(uce.toString());
		}

		name = ((NameCallback)callbacks[0]).getName();
		char[] passChars = ((PasswordCallback)callbacks[1]).getPassword();
		((PasswordCallback) callbacks[1]).clearPassword();
		String host = ((ConnectionCallback)callbacks[2]).getHost();
		String addr = ((ConnectionCallback)callbacks[2]).getAddress();
		int port = ((ConnectionCallback)callbacks[2]).getPort();
		authenticate(name, passChars, host, addr, port);
		return true;
	}
	private void authenticate(String name, char[] passChars) throws LoginException {
		authenticate(name, passChars, null, null, -1);
	}
	private void authenticate(final String name, char[] passChars, final String host, final String addr, final int port) throws LoginException {
		if(digest != null) {
			byte[] passBytes = charsToBytes(passChars);
			Arrays.fill(passChars, '\0');
			try {
				byte[] digestBytes = MessageDigest.getInstance(digest).digest(passBytes);
				passChars = bytesToHexChars(digestBytes);
				Arrays.fill(digestBytes, (byte) 0);
			} catch(NoSuchAlgorithmException nsae) {
				if(debug) log("No such digest algorithm "+digest+": "+nsae.toString());
				throw new LoginException(nsae.toString());
			} finally {
				Arrays.fill(passBytes, (byte) 0);
			}
		}

		Properties users = new Properties();
		try {
			InputStream in = new URL(url).openStream();
			try {
				users.load(in);
			} finally {
				in.close();
			}
		} catch(IOException ioe) {
			if(debug) log("Could not load "+url+": "+ioe.toString());
			throw new LoginException(ioe.toString());
		}

		String userLine = users.getProperty(name);
		users.clear();
		if(userLine == null) {
			Arrays.fill(passChars, '\0');
			if(debug) log("No such user");
			throw new FailedLoginException("No such user");
		}
		String[] userInfo = jircd.irc.Util.split(userLine, ' ');
		userLine = null;
		char[] userPassChars = userInfo[0].toCharArray();
		userInfo[0] = null;
		boolean passSucceeded = Arrays.equals(passChars, userPassChars);
		Arrays.fill(passChars, '\0');

		boolean hostSucceeded = jircd.irc.Util.match(userInfo[1], host);
		boolean ipSucceeded = jircd.irc.Util.match(userInfo[2], userInfo[3], addr);
		int userPort = Integer.parseInt(userInfo[4]);
		boolean portSucceeded = (userPort == -1) || (userPort == port);
		loginSucceeded = passSucceeded & hostSucceeded & ipSucceeded & portSucceeded;
		if(loginSucceeded) {
			if(debug) log("Authentication succeeded");
			if(storeFirstPass && !sharedState.containsKey(SHARED_NAME) && !sharedState.containsKey(SHARED_PASSWORD)) {
				sharedState.put(SHARED_NAME, name);
				sharedState.put(SHARED_PASSWORD, userPassChars);
			}
			Arrays.fill(userPassChars, '\0');
		} else {
			if(debug) log("Authentication failed");
			Arrays.fill(userPassChars, '\0');
			cleanState();
			throw new FailedLoginException("Invalid password");
		}
	}
	private static final Class[] PRNCPL_CNSTR_SIG = new Class[] {String.class};
	public boolean commit() throws LoginException {
		if(loginSucceeded) {
			try {
				Class principalClass = Class.forName(principalClassName);
				Constructor cnstr = principalClass.getConstructor(PRNCPL_CNSTR_SIG);
				principal = (Principal) cnstr.newInstance(new Object[] {name});
			} catch(Exception e) {
				if(debug) log("Could not create instance of "+principalClassName+": "+e.toString());
				throw new LoginException(e.toString());
			}
			subject.getPrincipals().add(principal);
			if(debug) log("Added principal "+principal+" to subject "+subject);
			cleanState();
			commitSucceeded = true;
			return true;
		} else {
			return false;
		}
	}
	public boolean abort() throws LoginException {
		if(debug) log("Aborting authentication");
		if(loginSucceeded) {
			if(commitSucceeded) {
				logout();
			} else {
				loginSucceeded = false;
				cleanState();
				principal = null;
			}
			return true;
		} else {
			return false;
		}
	}
	public boolean logout() throws LoginException {
		subject.getPrincipals().remove(principal);
		loginSucceeded = false;
		cleanState();
		principal = null;
		return true;
	}
	private void cleanState() {
		name = null;

		if(clearFirstPass) {
			sharedState.remove(SHARED_NAME);
			sharedState.remove(SHARED_PASSWORD);
		}
	}

	private static byte[] charsToBytes(char[] chs) {
		byte[] bs = new byte[2*chs.length];
		for(int i=0; i<chs.length; i++) {
			bs[2*i] = (byte)(chs[i]>>>8);
			bs[2*i+1] = (byte)(chs[i]);
		}
		return bs;
	}
	private static char[] bytesToHexChars(byte b[]) {
		char[] hex = new char[2*b.length];
                for(int i=0; i<b.length; i++) {
                        hex[2*i] = Character.forDigit(b[i]>>>4 & 0xF, 16);
                        hex[2*i+1] = Character.forDigit(b[i] & 0xF, 16);
                }
                return hex;
        }
	private static void log(String msg) {
		System.err.println("PropertiesLoginModule: "+msg);
	}
}
