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

/**
 * @author markhale
 */
public class UserHost implements Command {
	public void invoke(RegisteredEntity src, String[] params) {
		StringBuffer reply = new StringBuffer();
		for (int i = 0; i < params.length; i++) {
			User luser = src.getServer().getNetwork().getUser(params[i]);
			if (luser != null) {
				reply.append(' ').append(luser.getNick());
				if (luser.isModeSet(User.UMODE_OPER))
					reply.append('*');
				reply.append('=');
				if (luser.getAwayMessage() != null)
					reply.append('-');
				else
					reply.append('+');
				reply.append(luser.getIdent()).append('@');
				if (luser.equals(src))
					reply.append(luser.getHostName());
				else
					reply.append(luser.getDisplayHostName());
			}
		}
		Message message = new Message(Constants.RPL_USERHOST, src);
		if (reply.length() > 0)
			message.appendLastParameter(reply.substring(1)); // get rid of leading space ' '
		src.send(message);
	}
	public String getName() {
		return "USERHOST";
	}
	public int getMinimumParameterCount() {
		return 1;
	}
}
