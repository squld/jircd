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

package jircd.irc;

import java.util.Locale;

/**
 * ConnectedEntity is just created for a commonality between users and servers.
 * @author thaveman
 * @author markhale
 */
public abstract class ConnectedEntity implements Entity {
	protected final Connection.Handler handler;
	private Locale locale = Locale.getDefault();
        
	protected ConnectedEntity(Connection.Handler handler) {
		this.handler = handler;
	}
        /**
         * Returns the Handler (if any) used to send/pass messages to this entity.
         */
	public final Connection.Handler getHandler() {
		return handler;
	}
	public final void setLocale(Locale loc) {
		locale = loc;
	}
	public final Locale getLocale() {
		return locale;
	}
	/**
	 * Returns the server this entity is connected to.
	 * This method should never return null, to always ensure a reference to the network
	 * can be obtained with <code>ConnectedEntity.getServer().getNetwork()</code>.
	 */
	public abstract Server getServer();
	/**
	 * Sends a message to this entity.
	 */
	public abstract void send(Message msg);
        public void disconnect(String reason) {
                handler.disconnect();
        }
}
