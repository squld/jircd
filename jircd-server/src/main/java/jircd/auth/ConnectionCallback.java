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

import java.io.Serializable;

import javax.security.auth.callback.Callback;

/**
 * @author markhale
 */
public final class ConnectionCallback implements Callback, Serializable {
        static final long serialVersionUID = 4417835498079046992L;
	private String host;
	private String addr;
	private int port = -1;

	public String getHost() {
		return host;
	}
	public void setHost(String name) {
		this.host = name;
	}
	public String getAddress() {
		return addr;
	}
	public void setAddress(String addr) {
		this.addr = addr;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int n) {
		this.port = n;
	}
}
