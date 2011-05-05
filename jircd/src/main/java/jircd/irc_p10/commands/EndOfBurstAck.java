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

package jircd.irc_p10.commands;

import jircd.irc.Command;
import jircd.irc.RegisteredEntity;
import jircd.irc.Util;
import jircd.irc_p10.Server_P10;

/**
 * @author markhale
 */
public class EndOfBurstAck implements Command {
	public void invoke(RegisteredEntity src, String[] params) {
		if (src instanceof Server_P10) {
		} else {
                        Util.sendUnknownCommandError(src, getName());
		}
	}
	public String getName() {
		return "EA";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
