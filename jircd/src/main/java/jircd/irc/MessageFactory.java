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

package jircd.irc;

/**
 * Message factory.
 * @author markhale
 */
public class MessageFactory {
    protected final Network network;
    
    public MessageFactory(Network network) {
        this.network = network;
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

            if(sender.indexOf('@') != -1 || sender.indexOf('!') != -1)
                throw new IllegalArgumentException("Unexpected extended prefix (only allowed in server to client messages)");
            
            if(from instanceof Server) {
                if(sender.indexOf('.') == -1) {
                    // no '.', so must be a user
                    from = network.getUser(sender);
                } else {
                    // found a '.', must be a server
                    from = network.getServer(sender);
                }
                if(from == null)
                    throw new IllegalArgumentException("Unknown sender: "+sender);
            } else {
                if(from != network.getUser(sender))
                    throw new IllegalArgumentException("Sender mismatch: received from "+from.getName()+" but sender is "+sender);
            }
        }
        return parseMessage(from, str, startPos);
    }
    protected final Message parseMessage(ConnectedEntity from, String str, int startPos) {
        // parse command
        int endPos = str.indexOf(' ', startPos);
        if(endPos == -1) {
            // no parameters
            String command = str.substring(startPos);
            return new Message(from, command);
        } else {
            String command = str.substring(startPos, endPos);
            Message message = new Message(from, command);
            
            // parse parameters
            int trailingPos = str.indexOf(" :", endPos);
            if(trailingPos == -1)
                trailingPos = str.length();
            while(endPos != -1 && endPos < trailingPos) {
                startPos = endPos + 1;
                endPos = str.indexOf(' ', startPos);
                if(endPos != -1 && endPos-startPos > 0)
                    message.appendParameter(str.substring(startPos, endPos));
            }
            if(endPos == -1) {
                message.appendParameter(str.substring(startPos));
            } else {
                message.appendLastParameter(str.substring(trailingPos+2));
            }
            return message;
        }
    }
}
