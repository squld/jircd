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
import jircd.irc.Network;
import jircd.irc.RegisteredEntity;
import jircd.ircx.ChannelX;
import jircd.ircx.UserX;

/**
 * @author markhale
 */
public class Create implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        UserX user = (UserX) src;
        String channame = params[0];
        Network network = src.getServer().getNetwork();
        ChannelX chan = (ChannelX) network.getChannel(channame);
        if(chan == null) {
            chan = new ChannelX(channame, network);
            Message message = new Message(src.getServer(), "CREATE");
            message.appendParameter(channame);
            message.appendParameter("0");
            src.send(message);
            if(params.length > 1) {
                String modeStr = params[1];
                String[] modeParams = new String[params.length-2];
                for(int i=0; i<modeParams.length; i++)
                    modeParams[i] = params[i+2];
                chan.processModes(user, modeStr, modeParams);
            }
            chan.joinUser(user, null);
        }
    }
    public String getName() {
        return "CREATE";
    }
    public int getMinimumParameterCount() {
        return 1;
    }
}
