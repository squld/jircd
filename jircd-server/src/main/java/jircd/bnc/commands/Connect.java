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

package jircd.bnc.commands;

import jircd.jIRCdMBean;
import jircd.bnc.BouncerHandler;
import jircd.irc.Connection;
import jircd.irc.Message;
import jircd.irc.Server;
import jircd.irc.UnregisteredEntity;

/**
 * @author markhale
 */
public class Connect extends jircd.irc.commands.Connect {
    public Connect(jIRCdMBean jircd) {
        super(jircd);
    }
    protected Connection.Handler newConnectionHandler(jIRCdMBean jircd, Connection connection) {
        return new BouncerHandler(jircd, connection);
    }
    protected void sendLogin(UnregisteredEntity entity, String linkPassword) {
        Server thisServer = entity.getServer();
        if(linkPassword != null) {
            Message message = new Message(thisServer, "PASS");
            message.appendParameter(linkPassword);
            entity.send(message);
        }
        String nick = "Anon"+Long.toString(System.currentTimeMillis()/1000L);
        String[] params = {thisServer.getName(), "8", "*", thisServer.getDescription()};
        entity.setName(nick);
        entity.setParameters(params);
        Message message = new Message(thisServer, "NICK");
        message.appendParameter(nick);
        entity.send(message);
        message = new Message(thisServer, "USER");
        message.appendParameter(params[0]);
        message.appendParameter(params[1]);
        message.appendParameter(params[2]);
        message.appendLastParameter(params[3]);
        entity.send(message);
    }
    public String getName() {
        return "BCONNECT";
    }
}
