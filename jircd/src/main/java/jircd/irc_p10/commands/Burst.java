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

import jircd.irc.Channel;
import jircd.irc.Command;
import jircd.irc.Network;
import jircd.irc.RegisteredEntity;
import jircd.irc.Util;
import jircd.irc_p10.Server_P10;
import jircd.irc_p10.User_P10;

/**
 * @author markhale
 */
public class Burst implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        if (src instanceof Server_P10) {
            Server_P10 server = (Server_P10) src;
            Network network = server.getNetwork();
            String channame = params[0];
            if (Util.isChannelIdentifier(channame)) {
                Channel chan = network.getChannel(channame);
                if (chan == null) {
                    chan = new Channel(channame, network);
                }
                String timestamp = params[1];
                int n=2;
                if(params[2].charAt(0) == '+') {
                    n++;
                }
                String members[] = Util.split(params[n], ',');
                for(int i=0; i<members.length; i++)
                    addUser(network, chan, members[i]);
            } else {
                Util.sendNoSuchChannelError(src, channame);
            }
        } else {
            Util.sendUnknownCommandError(src, getName());
        }
    }
    private static void addUser(Network network, Channel chan, String nick) {
        String tokenB64;
        String modes;
        int pos = nick.indexOf(':');
        if(pos != -1) {
            tokenB64 = nick.substring(0, pos);
            modes = nick.substring(pos+1);
        } else {
            tokenB64 = nick;
            modes = "";
        }
        User_P10 user = jircd.irc_p10.Util.findUser(network, tokenB64);
        chan.addUser(user);
        // todo: modes string
    }
    public String getName() {
        return "B";
    }
    public int getMinimumParameterCount() {
        return 3;
    }
}
