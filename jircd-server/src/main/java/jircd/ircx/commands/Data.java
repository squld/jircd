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
import jircd.irc.User;
import jircd.irc.Util;
import jircd.ircx.UserX;

/**
 * @author markhale
 */
public class Data implements Command {
	public void invoke(RegisteredEntity src, String[] params) {
		UserX user = (UserX) src;
		if(!user.isIRCX())
			Util.sendUnknownCommandError(src, getName());
		String[] msgdest = Util.split(params[0], ',');
		if(params.length == 4) {
			// channel + list of nicks
			Channel chan = src.getServer().getNetwork().getChannel(msgdest[0]);
			if (chan == null) {
				Util.sendNoSuchChannelError(src, msgdest[0]);
			} else {
				String[] nickList = Util.split(params[1], ',');
				String tag = params[2];
				String msg = params[3];
				for(int i=0; i<nickList.length; i++)
					sendToChannelMember(user, chan, nickList[i], tag, msg);
			}
		} else {
			String tag = params[1];
			String msg = params[2];
			if(msgdest[0].charAt(0) == '#') {
				for(int i=0; i<msgdest.length; i++)
					sendToChannel(user, msgdest[i], tag, msg);
			} else {
				for(int i=0; i<msgdest.length; i++)
					sendToUser(user, msgdest[i], tag, msg);
			}
		}
	}
	private void sendToChannelMember(User src, Channel chan, String nick, String tag, String msg) {
		User target = src.getServer().getNetwork().getUser(nick);
		if (target == null) {
			Util.sendNoSuchNickError(src, nick);
		} else {
			if (chan.isOn(target)) {
				Message message = new Message(src, getName(), chan);
				message.appendParameter(nick);
				message.appendParameter(tag);
				message.appendLastParameter(msg);
				target.send(message);
			} else {
				Util.sendUserNotInChannelError(src, nick, chan.getName());
			}
		}
	}
	private void sendToChannel(User src, String chan, String tag, String msg) {
		Channel target = src.getServer().getNetwork().getChannel(chan);
		if (target == null) {
			Util.sendNoSuchChannelError(src, chan);
		} else {
			Message message = new Message(src, getName(), target);
			message.appendParameter(tag);
			message.appendLastParameter(msg);
			target.send(message, src);
		}
	}
	private void sendToUser(User src, String nick, String tag, String msg) {
		User target = src.getServer().getNetwork().getUser(nick);
		if (target == null) {
			Util.sendNoSuchNickError(src, nick);
		} else {
			Message message = new Message(src, getName(), target);
			message.appendParameter(tag);
			message.appendLastParameter(msg);
			target.send(message);
		}
	}
	public String getName() {
		return "DATA";
	}
	public int getMinimumParameterCount() {
		return 3;
	}
}
