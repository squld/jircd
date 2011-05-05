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

import jircd.irc.Message;
import jircd.irc.Server;
import jircd.irc.UnregisteredEntity;

/**
 * @author markhale
 */
public class BouncerServer extends Server {
    public BouncerServer(UnregisteredEntity unk, int token, String description) {
        super(unk, token, description);
    }
    public void send(Message msg) {
        if(msg.getSender().getServer() != this) {
            StringBuffer buf = new StringBuffer();
            // append command
            String cmd = msg.getCommand();
            buf.append(cmd);
            
            // append parameters
            if("PING".equals(cmd)) {
                buf.append(" :").append(msg.getParameter(0));
            } else {
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
            }
            handler.sendMessage(buf.toString());
        }
    }
}
