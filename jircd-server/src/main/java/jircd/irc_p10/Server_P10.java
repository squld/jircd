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

package jircd.irc_p10;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jircd.irc.ConnectedEntity;
import jircd.irc.Message;
import jircd.irc.Network;
import jircd.irc.Server;
import jircd.irc.UnregisteredEntity;
import jircd.irc.User;

/**
 * A P10 server on a server.
 * @author markhale
 */
public class Server_P10 extends Server {
    /** (Integer token, User_P10 user) */
    private final Map userTokens = new ConcurrentHashMap();
    
    public Server_P10(String name, int hopcount, int token, String description, Server_P10 route) {
        super(name, hopcount, token, description, route);
    }
    public Server_P10(UnregisteredEntity unk, int token, String description) {
        super(unk, token, description);
    }
    public Server_P10(String name, int token, String description, Network network) {
        super(name, token, description, network);
    }

    /** User_P10 hook */
    void addUserToken(User_P10 user) {
        userTokens.put(new Integer(user.getToken()), user);
    }
    public User_P10 getUser(int token) {
        return (User_P10) userTokens.get(new Integer(token));
    }
    protected void removeUser(User usr) {
        super.removeUser(usr);
        User_P10 user = (User_P10) usr;
        userTokens.remove(new Integer(user.getToken()));
    }
    
    /** ID */
    public String toString() {
        return Util.toBase64Token(this);
    }
    public void send(Message msg) {
        msg = Util.transcode(network, msg);
        
        StringBuffer buf = new StringBuffer();
        // append prefix
        ConnectedEntity sender = msg.getSender();
        if(sender != null)
            buf.append(sender.toString()).append(' ');
        
        // append command
        buf.append(msg.getCommand());
        
        // append parameters
        final int paramCount = msg.getParameterCount();
        if(paramCount > 0) {
            final int lastParamIndex = paramCount - 1;
            for(int i=0; i<lastParamIndex; i++)
                buf.append(' ').append(msg.getParameter(i));
            if(msg.hasLastParameter())
                buf.append(" :").append(msg.getParameter(lastParamIndex));
            else
                buf.append(' ').append(msg.getParameter(lastParamIndex));
        }
        handler.sendMessage(buf.toString());
    }
}
