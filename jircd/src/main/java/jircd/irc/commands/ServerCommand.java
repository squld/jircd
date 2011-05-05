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

import jircd.jIRCdMBean;
import jircd.irc.Connection;
import jircd.irc.RegisteredEntity;
import jircd.irc.RegistrationCommand;
import jircd.irc.Server;
import jircd.irc.UnregisteredEntity;
import jircd.irc.Util;

import org.apache.log4j.Logger;

/**
 * @author markhale
 */
public class ServerCommand implements RegistrationCommand {
    private static final Logger logger = Logger.getLogger(Error.class);
    protected final jIRCdMBean jIRCd;
    
    public ServerCommand(jIRCdMBean jircd) {
        this.jIRCd = jircd;
    }
    public void invoke(RegisteredEntity src, String[] params) {
        String name = params[0];
        int hopcount = Integer.parseInt(params[1]);
        int token = Integer.parseInt(params[2]);
        String desc = params[3];
        Server server = new Server(name, hopcount, token, desc, (Server) src);
    }
    public final void invoke(final UnregisteredEntity src, String[] params) {
        Connection.Handler handler = src.getHandler();
        final Connection connection = handler.getConnection();
        if(checkPass(connection, src.getPass())) {
            login(src, params);
        } else {
            logger.warn("Invalid password");
            src.disconnect("Invalid password");
        }
    }
    private boolean checkPass(Connection connection, String[] passParams) {
        String expectedPassword = jIRCd.getProperty("jircd.accept."+connection.getRemoteAddress()+'#'+connection.getLocalPort());
        if(expectedPassword != null) {
            String password = (passParams != null && passParams.length > 0) ? passParams[0] : null;
            return expectedPassword.equals(password);
        } else {
            return false;
        }
    }
    protected void login(final UnregisteredEntity src, String[] params) {
        Connection.Handler handler = src.getHandler();
        final Connection connection = handler.getConnection();
        String name = params[0];
        int hopcount = Integer.parseInt(params[1]);
        if(hopcount != 1)
            Util.sendError(src, "The hop count must be 1 for a peer server: "+hopcount);
        int token = Integer.parseInt(params[2]);
        String desc = params[3];
        src.setName(name);
        Server server = new Server(src, token, desc);
        handler.login(server);
        
        String linkPassword = jIRCd.getProperty("jircd.connect."+connection.getRemoteAddress());
        if(src.getParameters() == null) {
            Util.sendPass(server, linkPassword);
            Util.sendServer(server);
        }
        Util.sendNetSync(server);
    }
    public String getName() {
        return "SERVER";
    }
    public int getMinimumParameterCount() {
        return 4;
    }
}
