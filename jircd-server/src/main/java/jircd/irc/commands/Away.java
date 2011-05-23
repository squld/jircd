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

import jircd.irc.Command;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class Away implements Command {
	public void invoke(RegisteredEntity src, String[] params) {
		if(src instanceof User) {
			User user = (User) src;
			invoke(user, params);
		}
	}
	private void invoke(User user, String[] params) {
		if(params.length > 0) {
			user.setAwayMessage(params[0]);
			Message message = new Message(Constants.RPL_NOWAWAY, user);
			message.appendLastParameter(Util.getResourceString(user, "RPL_NOWAWAY"));
			user.send(message);
		} else {
			user.setAwayMessage(null);
			Message message = new Message(Constants.RPL_UNAWAY, user);
			message.appendLastParameter(Util.getResourceString(user, "RPL_UNAWAY"));
			user.send(message);
		}
	}
	public String getName() {
		return "AWAY";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
