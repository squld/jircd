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

import jircd.irc.Command;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc_p10.Server_P10;
import jircd.irc_p10.User_P10;
import jircd.irc_p10.Util;

/**
 * @author markhale
 */
public class Nick implements Command {
    private static final int MINIMUM_PARAM_COUNT_USER = 2;
    private static final int MINIMUM_PARAM_COUNT_SERVER = 8;
    
    public void invoke(RegisteredEntity src, String[] params) {
        if(src instanceof Server_P10 && params.length >= MINIMUM_PARAM_COUNT_SERVER) {
            invoke((Server_P10) src, params);
        } else {
            invoke((User_P10) src, params);
        }
    }
    private void invoke(User_P10 user, String[] params) {
        String newNick = params[0];
        String timestamp = params[1];
        user.changeNick(newNick);
    }
    private void invoke(Server_P10 server, String[] params) {
        String nick = params[0];
        int hopcount = Integer.parseInt(params[1]);
        String timestamp = params[2];
        String ident = params[3];
        String host = params[4];
        String ipB64 = params[params.length-3];
        String tokenB64 = params[params.length-2];
        String desc = params[params.length-1];
        String serverTokenB64 = tokenB64.substring(0, 2);
        Server_P10 userServer = Util.findServer(server.getNetwork(), serverTokenB64);
        String userTokenB64 = tokenB64.substring(2);
        int userToken = Util.parseBase64(userTokenB64);
        User user = new User_P10(nick, hopcount, userToken, ident, host, desc, userServer);
    }
    public String getName() {
        return "N";
    }
    public int getMinimumParameterCount() {
        return MINIMUM_PARAM_COUNT_USER;
    }
}
