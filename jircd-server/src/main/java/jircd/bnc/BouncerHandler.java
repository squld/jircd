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

package jircd.bnc;

import java.util.Iterator;

import jircd.jIRCdMBean;
import jircd.irc.Channel;
import jircd.irc.ConnectedEntity;
import jircd.irc.Connection;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.Network;
import jircd.irc.Server;
import jircd.irc.UnregisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class BouncerHandler extends Connection.Handler {
    public BouncerHandler(jIRCdMBean jircd, Connection connection) {
        super(jircd, connection);
        setMessageFactory(new BouncerMessageFactory(getEntity().getServer().getNetwork()));
    }
    protected void handleReply(Message msg) {
        String cmd = msg.getCommand();
        if(Constants.RPL_MYINFO.equals(cmd)) {
            ConnectedEntity src = getEntity();
            if(src instanceof UnregisteredEntity) {
                loginBouncer((UnregisteredEntity) src, msg);
            }
        } else if(Constants.RPL_NAMREPLY.equals(cmd)) {
            String channame = msg.getParameter(2);
            String nickList = msg.getParameter(msg.getParameterCount()-1);
            BouncerServer server = (BouncerServer) getEntity();
            Network network = server.getNetwork();
            Channel channel = network.getChannel(channame);
            if(channel == null) {
                channel = new Channel(channame, network);
            }
            String nicks[] = Util.split(nickList, ' ');
            for(int i=0; i<nicks.length; i++) {
                String nick = nicks[i];
                if(Util.isUserModeIdentifier(nick.charAt(0)))
                    nick = nick.substring(1);
                User usr = network.getUser(nick);
                if(usr == null) {
                    usr = new User(nick, 1, "ToDo", "ToDo", "ToDo", server);
                }
                channel.addUser(usr);
            }
        }
    }
    protected void handleCommand(Message msg) {
        ConnectedEntity entity = getEntity();
        if(entity instanceof UnregisteredEntity) {
            if("PING".equals(msg.getCommand())) {
                Message pongMsg = new Message("PONG").appendLastParameter(msg.getParameter(0));
                entity.send(pongMsg);
            }
        } else {
            super.handleCommand(msg);
        }
    }
    private void loginBouncer(UnregisteredEntity src, Message loginMsg) {
        String nick = src.getName();
        String[] params = src.getParameters();
        BouncerUser user = new BouncerUser(src, params[0], params[3]);
        
        String serverName = loginMsg.getParameter(0);
        String serverDesc = loginMsg.getParameter(1);
        int token = serverName.hashCode();
        src.setName(serverName);
        BouncerServer server = new BouncerServer(src, token, serverDesc);
        login(server);
        
        final Server thisServer = server.getServer();
        for(Iterator iter = thisServer.getNetwork().getChannels().iterator(); iter.hasNext(); ) {
            Channel chan = (Channel) iter.next();
            Message message = new Message(thisServer, "JOIN");
            message.appendParameter(chan.getName());
            server.send(message);
        }
    }
}
