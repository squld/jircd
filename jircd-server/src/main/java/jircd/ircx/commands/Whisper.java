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

import jircd.irc.Channel;
import jircd.irc.Command;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.Util;
import jircd.ircx.UserX;

/**
 * @author markhale
 */
public class Whisper implements Command {
	public void invoke(RegisteredEntity src, String[] params) {
		UserX user = (UserX) src;
		if(!user.isIRCX())
			Util.sendUnknownCommandError(src, getName());
		String channame = params[0];
		Channel chan = src.getServer().getNetwork().getChannel(channame);
		if (chan == null) {
			Util.sendNoSuchChannelError(src, channame);
		} else {
			if (chan.isOn(user)) {
				String[] nickList = Util.split(params[1], ',');
				String msg = params[2];
				for(int i=0; i<nickList.length; i++)
					whisper(user, chan, nickList[i], msg);
			} else {
				Util.sendNotOnChannelError(src, channame);
			}
		}
	}
	private void whisper(UserX src, Channel chan, String nick, String msg) {
		UserX target = (UserX) src.getServer().getNetwork().getUser(nick);
		if (target == null) {
			Util.sendNoSuchNickError(src, nick);
		} else {
			if (chan.isOn(target)) {
				if(target.isIRCX()) {
					Message message = new Message(src, "WHISPER", chan);
					message.appendParameter(nick);
					message.appendLastParameter(msg);
					target.send(message);
				} else {
					Message message = new Message(src, "PRIVMSG", target);
					message.appendLastParameter(msg);
					target.send(message);
				}
			} else {
				Util.sendUserNotInChannelError(src, nick, chan.getName());
			}
		}
	}
	public String getName() {
		return "WHISPER";
	}
	public int getMinimumParameterCount() {
		return 3;
	}
}
