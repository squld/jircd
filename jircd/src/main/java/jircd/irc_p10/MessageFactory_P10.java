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

import jircd.irc.ConnectedEntity;
import jircd.irc.Message;
import jircd.irc.MessageFactory;
import jircd.irc.Network;

/**
 * P10 Message factory.
 * @author markhale
 */
public class MessageFactory_P10 extends MessageFactory {
    public MessageFactory_P10(Network network) {
        super(network);
    }
    public Message createMessage(ConnectedEntity from, String str) {
        // parse prefix
        int endPos = str.indexOf(' ');
        String sender = str.substring(0, endPos);
        int startPos = endPos + 1;
        if(sender.charAt(0) == ':') {
            from = network.getServer(sender.substring(1));
        } else {
            // token prefix
            from = Util.findEntity(network, sender);
        }
        if(from == null)
            throw new IllegalArgumentException("Unknown sender: "+sender);
        
        return parseMessage(from, str, startPos);
    }
}
