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

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executors;

import jircd.jIRCdMBean;
import jircd.irc.Command;
import jircd.irc.ConnectedEntity;
import jircd.irc.Connection;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.StreamConnection;
import jircd.irc.UnregisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class Connect implements Command {
    private final jIRCdMBean jircd;
    
    public Connect(jIRCdMBean jircd) {
        this.jircd = jircd;
    }
    public final void invoke(RegisteredEntity src, String[] params) {
        final String host = params[0];
        final int port = params.length>1 ? Integer.parseInt(params[1]) : Constants.DEFAULT_PORT;
        User user = (User) src;
        try {
            Util.checkOperatorPermission(user);
            try {
                StreamConnection connection = new StreamConnection(new Socket(host, port), jircd.getLinks(), Executors.newSingleThreadExecutor());
                Connection.Handler handler = newConnectionHandler(jircd, connection);
                connection.setHandler(handler);
                connection.start();
                UnregisteredEntity entity = (UnregisteredEntity) handler.getEntity();
                String linkPassword = jircd.getProperty("jircd.connect."+connection.getRemoteAddress()+'#'+connection.getRemotePort());
                sendLogin(entity, linkPassword);
            } catch(IOException e) {
                Message message = new Message(Constants.ERR_NOSUCHSERVER, src);
                message.appendParameter(host);
                message.appendLastParameter("No such server");
                src.send(message);
            }
        } catch(SecurityException se) {
            Util.sendNoPrivilegesError(src);
        }
    }
    protected Connection.Handler newConnectionHandler(jIRCdMBean jircd, Connection connection) {
        return new Connection.Handler(jircd, connection);
    }
    protected void sendLogin(UnregisteredEntity entity, String linkPassword) {
        sendPass(entity, linkPassword);
        sendServer(entity, jircd);
        entity.setParameters(new String[0]); // so that we know we sent PASS & SERVER
    }
    protected void sendPass(ConnectedEntity to, String password) {
        Util.sendPass(to, password);
    }
    protected void sendServer(ConnectedEntity to, jIRCdMBean jircd) {
        Util.sendServer(to);
    }
    
    public String getName() {
        return "CONNECT";
    }
    public int getMinimumParameterCount() {
        return 1;
    }
}
