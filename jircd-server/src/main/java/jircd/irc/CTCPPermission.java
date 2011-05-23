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

import java.security.Permission;

/**
 * @author markhale
 */
public final class CTCPPermission extends Permission {
        static final long serialVersionUID = 744459141601570658L;
	private final String action;

	/**
	 * @param name CTCP message type, can be the wildcard "*".
	 */
	public CTCPPermission(String name) {
		this(name, "");
	}
	/**
	 * @param name CTCP message type, can be the wildcard "*".
	 * @param action DCC action, can be the wildcard "*";
	 */
	public CTCPPermission(String name, String action) {
		super(name);
		if(action == null)
			throw new NullPointerException("Action cannot be null");
		this.action = action;
	}
	public boolean implies(Permission permission) {
		if(permission != null && permission.getClass() == getClass()) {
			return (getName().equals("*") || getName().equals(permission.getName()))
				&& (getActions().equals("*") || getActions().equals(permission.getActions()));
		} else {
			return false;
		}
	}
	public String getActions() {
		return action;
	}
	public boolean equals(Object obj) {
		if(obj != null && obj.getClass() == getClass()) {
			CTCPPermission p = (CTCPPermission) obj;
			return getName().equals(p.getName()) && getActions().equals(p.getActions());
		} else {
			return false;
		}
	}
	public int hashCode() {
		return getName().hashCode() ^ getActions().hashCode();
	}
}
