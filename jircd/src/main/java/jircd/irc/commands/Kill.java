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

import jircd.irc.Command;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.Server;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class Kill implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        if(src instanceof User) {
            User user = (User) src;
            try {
                Util.checkOperatorPermission(user);
            } catch(SecurityException se) {
                Util.sendNoPrivilegesError(src);
                return;
            }
        }
        final String nick = params[0];
        if(Util.isNickName(nick)) {
            Server server = src.getServer();
            User target = server.getNetwork().getUser(nick);
            if(target == null) {
                Util.sendNoSuchNickError(src, nick);
            } else {
                if(target.isLocal()) {
                    target.disconnect("Kill by " + src.getName() + ": " + params[1]);
                } else {
                    // forward on
                    Message msg = new Message(src, "KILL", target).appendLastParameter(params[1]);
                    target.send(msg);
                }
            }
        }
    }
    public String getName() {
        return "KILL";
    }
    public int getMinimumParameterCount() {
        return 2;
    }
}
