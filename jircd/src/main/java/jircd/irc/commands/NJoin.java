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
import jircd.irc.Server;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class NJoin implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        if (src instanceof Server) {
            Server server = (Server) src;
            Network network = server.getNetwork();
            final String channame = params[0];
            if (Util.isChannelIdentifier(channame)) {
                Channel chan = network.getChannel(channame);
                if (chan == null) {
                    chan = new Channel(channame, network);
                }
                String members[] = Util.split(params[1], ',');
                for(int i=0; i<members.length; i++)
                    addUser(network, chan, members[i]);
            } else {
                Util.sendNoSuchChannelError(src, channame);
            }
        }
    }
    private void addUser(Network network, Channel chan, String nick) {
        if(nick.startsWith("@@"))
            nick = nick.substring(2);
        else if(nick.charAt(0) == '@')
            nick = nick.substring(1);
        else if(nick.charAt(0) == '+')
            nick = nick.substring(1);
        chan.addUser(network.getUser(nick));
    }
    public String getName() {
        return "NJOIN";
    }
    public int getMinimumParameterCount() {
        return 2;
    }
}
