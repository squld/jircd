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
import jircd.irc.Util;

/**
 * @author markhale
 */
public class Info implements Command {
	private String[] info;

	public Info(jIRCdMBean jircd) throws IOException {
		info = Util.loadTextFile(jircd.getProperty("jircd.info", "info.txt"), 100);
	}
	public void invoke(RegisteredEntity src, String[] params) {
		for(int i=0; i<info.length; i++) {
			Message message = new Message(Constants.RPL_INFO, src);
			message.appendLastParameter(info[i]);
			src.send(message);
		}
		Message message = new Message(Constants.RPL_ENDOFINFO, src);
		message.appendLastParameter(Util.getResourceString(src, "RPL_ENDOFINFO"));
		src.send(message);
	}
	public String getName() {
		return "INFO";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
