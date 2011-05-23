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
public class Part implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        String[] chanNames = Util.split(params[0], ',');
        String partMsg = (params.length > 1 ? params[1] : null);
        User user = (User) src;
        for(int i=0; i<chanNames.length; i++) {
            part(chanNames[i], user, partMsg);
        }
    }
    private void part(String channame, User user, String partMsg) {
        if (Util.isChannelIdentifier(channame)) {
            Network network = user.getServer().getNetwork();
            Channel chan = network.getChannel(channame);
            if (chan == null) {
                Util.sendNoSuchChannelError(user, channame);
            } else {
                if(chan.isOn(user)) {
                    Message message = new Message(user, "PART", chan);
                    if(partMsg != null)
                        message.appendLastParameter(partMsg);
                    chan.sendLocal(message);
                    network.send(message, user.getServer());
                    chan.removeUser(user);
                } else {
                    Util.sendNotOnChannelError(user, channame);
                }
            }
        } else {
            Util.sendNoSuchChannelError(user, channame);
        }
    }
    public String getName() {
        return "PART";
    }
    public int getMinimumParameterCount() {
        return 1;
    }
}
