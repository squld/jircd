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

package jircd.ircx.commands;

import jircd.irc.Command;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.Util;
import jircd.ircx.ChannelX;
import jircd.ircx.Constants;
import jircd.ircx.UserX;

/**
 * @author markhale
 */
public class Prop implements Command {
	public void invoke(RegisteredEntity src, String[] params) {
		UserX user = (UserX) src;
		String channame = params[0];
		ChannelX chan = (ChannelX) src.getServer().getNetwork().getChannel(channame);
		if(chan == null) {
			Util.sendNoSuchChannelError(src, channame);
		} else {
			if(params.length == 3) {
				String prop = params[1];
				String data = params[2];
				chan.setProperty(user, prop, data);
				Message message = new Message(src.getServer(), "PROP", chan);
				message.appendParameter(prop);
				message.appendLastParameter(data);
				src.send(message);
			} else {
				String[] props = Util.split(params[1], ',');
				for(int i=0; i<props.length; i++) {
					Message message = new Message(src.getServer(), Constants.IRCRPL_PROPLIST, chan);
					message.appendParameter(props[i]);
					message.appendLastParameter(chan.getProperty(props[i]));
					src.send(message);
				}
				Message message = new Message(src.getServer(), Constants.IRCRPL_PROPEND, chan);
				message.appendLastParameter("End of properties");
				src.send(message);
			}
		}
	}
	public String getName() {
		return "PROP";
	}
	public int getMinimumParameterCount() {
		return 2;
	}
}
