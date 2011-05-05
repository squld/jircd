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

import jircd.jIRCdMBean;
import jircd.irc.Server;
import jircd.irc.UnregisteredEntity;
import jircd.irc.User;
import jircd.irc_p10.Server_P10;
import jircd.irc_p10.User_P10;
import jircd.irc_p10.Util;

/**
 * @author markhale
 */
public class UserCommand extends jircd.irc.commands.UserCommand {
    public UserCommand(jIRCdMBean jircd) {
        super(jircd);
    }
    protected User createUser(UnregisteredEntity unk, String username, String desc) {
        return new User_P10(unk, username, desc);
    }
    protected void sendUser(Server server, User user) {
        if(server instanceof Server_P10) {
            sendUser_P10((Server_P10) server, (User_P10) user);
        } else {
            // fallback to older protocol
            super.sendUser(server, user);
        }
    }
    private void sendUser_P10(Server_P10 server, User_P10 user) {
        Util.sendUser(server, (Server_P10) user.getServer(), user);
    }
}
