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
import jircd.irc.Network;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class Join implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        String[] chanNames = Util.split(params[0], ',');
        String[] keys = (params.length > 1 ? Util.split(params[1], ',') : null);
        User user = (User) src;
        for(int i=0; i<chanNames.length; i++) {
            final String channame = chanNames[i];
            String key = (keys != null && i < keys.length ? keys[i] : null);
            join(chanNames[i], user, key);
        }
    }
    private void join(String channame, User user, String key) {
        if (Util.isChannelIdentifier(channame)) {
            Network network = user.getServer().getNetwork();
            Channel chan = network.getChannel(channame);
            if (chan == null) {
                chan = createChannel(channame, network);
            }
            chan.joinUser(user, key);
        } else {
            Util.sendNoSuchChannelError(user, channame);
        }
    }
    protected Channel createChannel(String name, Network network) {
        return new Channel(name, network);
    }
    public String getName() {
        return "JOIN";
    }
    public int getMinimumParameterCount() {
        return 1;
    }
}
