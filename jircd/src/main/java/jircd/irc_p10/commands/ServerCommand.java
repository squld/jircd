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
import jircd.irc.Connection;
import jircd.irc.RegisteredEntity;
import jircd.irc.UnregisteredEntity;
import jircd.irc_p10.MessageFactory_P10;
import jircd.irc_p10.Server_P10;
import jircd.irc_p10.Util;

/**
 * @author markhale
 */
public class ServerCommand extends jircd.irc.commands.ServerCommand {
    private static final int MINIMUM_PARAM_COUNT_P10 = 7;
    
    public ServerCommand(jIRCdMBean jircd) {
        super(jircd);
    }
    public void invoke(RegisteredEntity src, String[] params) {
        Connection.Handler handler = src.getHandler();
        String name = params[0];
        int hopcount = Integer.parseInt(params[1]);
        String startTime = params[2];
        String linkTime = params[3];
        String protocol = params[4];
        String tokenB64 = params[5].substring(0, 2);
        String mask = params[5].substring(2);
        String desc = params[6];
        Server_P10 server = new Server_P10(name, hopcount, Util.parseBase64(tokenB64), desc, (Server_P10) src);
    }
    protected void login(final UnregisteredEntity src, String[] params) {
        if(params.length >= MINIMUM_PARAM_COUNT_P10) {
            login_P10(src, params);
        } else {
            // fallback to older protocol
            super.login(src, params);
        }
    }
    private void login_P10(final UnregisteredEntity src, String[] params) {
        Connection.Handler handler = src.getHandler();
        final Connection connection = handler.getConnection();
        String name = params[0];
        int hopcount = Integer.parseInt(params[1]);
        if(hopcount != 1)
            jircd.irc.Util.sendError(src, "The hop count must be 1 for a peer server: "+hopcount);
        String startTime = params[2];
        String linkTime = params[3];
        String protocol = params[4];
        String tokenB64 = params[5].substring(0, 2);
        String mask = params[5].substring(2);
        String desc = params[6];
        src.setName(name);
        Server_P10 server = new Server_P10(src, Util.parseBase64(tokenB64), desc);
        handler.login(server);
        handler.setMessageFactory(new MessageFactory_P10(src.getServer().getNetwork()));
        
        if(src.getParameters() == null) {
            String linkPassword = jIRCd.getProperty("jircd.connect."+connection.getRemoteAddress());
            Util.sendPass(server, linkPassword);
            Util.sendServer(server, jIRCd);
        }
        Util.sendNetSync(server);
    }
}
