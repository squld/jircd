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

import java.util.Iterator;

import jircd.jIRCdMBean;
import jircd.irc.Command;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.Server;
import jircd.irc.User;

/**
 * @author markhale
 */
public class Wallops implements Command {
    private final jIRCdMBean jircd;
    
    public Wallops(jIRCdMBean jircd) {
        this.jircd = jircd;
    }
    public void invoke(RegisteredEntity src, String[] params) {
        if(src instanceof Server) {
            Server server = (Server) src;
            String msg = params[0];
            for(Iterator iter = jircd.getServer().getUsers().iterator(); iter.hasNext(); ) {
                User user = (User) iter.next();
                if(user.isModeSet(User.UMODE_WALLOPS)) {
                    Message message = new Message(server, "WALLOPS");
                    message.appendLastParameter(msg);
                    user.send(message);
                }
            }
        }
    }
    public String getName() {
        return "WALLOPS";
    }
    public int getMinimumParameterCount() {
        return 1;
    }
}
