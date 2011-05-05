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

import jircd.irc.ConnectedEntity;
import jircd.irc.Message;
import jircd.irc.MessageFactory;
import jircd.irc.Network;
import jircd.irc.User;

/**
 * Message factory.
 * @author markhale
 */
public class BouncerMessageFactory extends MessageFactory {
    public BouncerMessageFactory(Network network) {
        super(network);
    }
    
    /**
     * Parses a message string.
     */
    public Message createMessage(ConnectedEntity from, String str) {
        int startPos = 0;
        
        // parse prefix
        if(str.charAt(0) == ':') {
            int endPos = str.indexOf(' ', 2);
            String sender = str.substring(1, endPos);
            startPos = endPos + 1;
            
            int hostPos = sender.indexOf('@');
            int identPos = sender.indexOf('!');
            if(hostPos != -1 || identPos != -1 || sender.indexOf('.') == -1) { // lastly if no '.', then assume a user
                String name;
                String ident = null;
                String host = null;
                if(hostPos != -1) {
                    if(identPos != -1) {
                        name = sender.substring(0, identPos);
                        ident = sender.substring(identPos+1, hostPos);
                    } else {
                        name = sender.substring(0, hostPos);
                    }
                    host = sender.substring(hostPos+1);
                } else {
                    name = sender;
                }
                User user = network.getUser(name);
                if(user == null) {
                    user = new User(name, 1, ident, host, "ToDo", (BouncerServer) from);
                }
                from = user;
            }
            if(from == null)
                throw new IllegalArgumentException("Unknown sender: "+sender);
        }
        return parseMessage(from, str, startPos);
    }
}
