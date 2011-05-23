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

import jircd.jIRCdMBean;
import jircd.irc.Command;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;

/**
 * @author markhale
 */
public class Version implements Command {
	private final String version;
	private final String serverName;
	private final String comments;

	public Version(jIRCdMBean jircd) {
		version = jircd.getVersion();
		serverName = jircd.getProperty("jircd.serverName");
		comments = jircd.getProperty("jircd.version.url");
	}
	public void invoke(RegisteredEntity src, String[] params) {
		Message message = new Message(Constants.RPL_VERSION, src);
		message.appendParameter(version);
		message.appendParameter(serverName);
		message.appendLastParameter(comments);
		src.send(message);
	}
	public String getName() {
		return "VERSION";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
