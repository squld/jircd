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

import jircd.irc.Channel;
import jircd.irc.Command;
import jircd.irc.Message;
import jircd.irc.Network;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class Kick implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        final String channame = params[0];
        final String nickname = params[1];
        if (Util.isChannelIdentifier(channame)) {
            // check channel exists
            Network network = src.getServer().getNetwork();
            Channel chan = network.getChannel(channame);
            if (chan == null) {
                Util.sendNoSuchChannelError(src, channame);
            } else {
                if(Util.isNickName(nickname)) {
                    // check nick exists
                    User user = network.getUser(nickname);
                    if(user == null) {
                        Util.sendNoSuchNickError(src, nickname);
                    } else {
                        if(chan.isOp((User)src)) {
                            Message message = new Message(src, "KICK", chan);
                            message.appendParameter(user.getNick());
                            if(params.length == 3)
                                message.appendLastParameter(params[2]);
                            chan.sendLocal(message);
                            network.send(message, src.getServer());
                            chan.removeUser(user);
                        } else {
                            Util.sendChannelOpPrivilegesNeededError(src, channame);
                        }
                    }
                }
            }
        } else {
            Util.sendNoSuchChannelError(src, channame);
        }
    }
    public String getName() {
        return "KICK";
    }
    public int getMinimumParameterCount() {
        return 2;
    }
}
