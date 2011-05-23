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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * @author markhale
 */
public class PassiveCallbackHandler implements CallbackHandler {
	private final String name;
	private final char[] password;
	private final String hostname;
	private final String hostaddr;
	private final int port;

	public PassiveCallbackHandler(String name, char[] password) {
		this(name, password, null, null, -1);
	}
	public PassiveCallbackHandler(String name, char[] password, String hostname, String hostaddr, int port) {
		this.name = name;
		this.password = password;
		this.hostname = hostname;
		this.hostaddr = hostaddr;
		this.port = port;
	}
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for(int i=0; i<callbacks.length; i++) {
			Callback callback = callbacks[i];
			if(callback instanceof NameCallback) {
				NameCallback nc = (NameCallback) callback;
				nc.setName(name);
			} else if(callback instanceof PasswordCallback) {
				PasswordCallback pc = (PasswordCallback) callback;
				pc.setPassword(password);
			} else if(callback instanceof ConnectionCallback) {
				ConnectionCallback cc = (ConnectionCallback) callback;
				cc.setHost(hostname);
				cc.setAddress(hostaddr);
				cc.setPort(port);
			} else {
				throw new UnsupportedCallbackException(callback, "Unrecognized callback");
			}
		}
	}
}
