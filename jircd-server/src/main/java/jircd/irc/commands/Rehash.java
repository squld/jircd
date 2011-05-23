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

package jircd.irc.commands;

import java.io.IOException;

import jircd.jIRCdMBean;
import jircd.irc.Command;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * This REHASH command will reload all Command plugins from the plugins directory.
 * Plugins can thus be hot-swapped while the IRC server is running.
 * @author markhale
 */
public class Rehash implements Command {
	private final jIRCdMBean jircd;

	public Rehash(jIRCdMBean jircd) {
		this.jircd = jircd;
	}
	public void invoke(RegisteredEntity src, String[] params) {
		User user = (User) src;
		try {
			Util.checkOperatorPermission(user);
			Message msg = new Message(Constants.RPL_REHASHING, user);
			msg.appendParameter(jircd.getProperty("jircd.configURL"));
			msg.appendParameter(Util.getResourceString(user, "RPL_REHASHING"));
			user.send(msg);
			jircd.reloadPolicy();
			try {
				jircd.reloadConfiguration();
			} catch(IOException ioe) {
				msg = new Message("ERROR", user);
				msg.appendLastParameter(ioe.toString());
				user.send(msg);
			}
			jircd.reloadPlugins();
		} catch(SecurityException se) {
			Util.sendNoPrivilegesError(user);
		}
	}
	public String getName() {
		return "REHASH";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
