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

import java.util.Iterator;

import jircd.irc.Channel;
import jircd.irc.Command;
import jircd.irc.Connection;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class WhoIs implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        if (params.length == 1) {
            String nick = params[0];
            User who = src.getServer().getNetwork().getUser(nick);
            if (who == null) {
                Util.sendNoSuchNickError(src, nick);
            } else {
                String hostname;
                if (who.equals(src) || ((User)src).isModeSet(User.UMODE_OPER))
                    hostname = who.getHostName();
                else
                    hostname = who.getDisplayHostName();
                
                Message message = new Message(Constants.RPL_WHOISUSER, src);
                message.appendParameter(who.getNick());
                message.appendParameter(who.getIdent());
                message.appendParameter(hostname);
                message.appendParameter("*");
                message.appendLastParameter(who.getDescription());
                src.send(message);
                
                StringBuffer chanlist = new StringBuffer();
                for(Iterator iter = who.getChannels().iterator(); iter.hasNext();) {
                    Channel chan = (Channel) iter.next();
                    if (chan.isModeSet(Channel.CHANMODE_SECRET) || chan.isOn((User)src)) {
                        chanlist.append(' ');
                        if (chan.isOp(who)) chanlist.append("@");
                        if (chan.isVoice(who)) chanlist.append("+");
                        chanlist.append(chan.getName());
                    }
                }
                if (chanlist.length() > 0) {
                    message = new Message(Constants.RPL_WHOISCHANNELS, src);
                    message.appendParameter(who.getNick());
                    message.appendParameter(chanlist.substring(1)); // get rid of leading space ' '
                    src.send(message);
                }
                
                message = new Message(Constants.RPL_WHOISSERVER, src);
                message.appendParameter(who.getNick());
                message.appendParameter(who.getServer().getName());
                message.appendLastParameter(who.getServer().getDescription());
                src.send(message);
                
                if (who.isModeSet(User.UMODE_OPER)) {
                    message = new Message(Constants.RPL_WHOISOPERATOR, src);
                    message.appendParameter(who.getNick());
                    message.appendLastParameter(Util.getResourceString(src, "RPL_WHOISOPERATOR"));
                    src.send(message);
                }
                
                if(who.isLocal()) {
                    message = new Message(Constants.RPL_WHOISIDLE, src);
                    message.appendParameter(who.getNick());
                    Connection.Handler handler = who.getHandler();
                    message.appendParameter(Long.toString(handler.getIdleTimeMillis() / Constants.SECS_TO_MILLIS));
                    message.appendParameter(Long.toString(handler.getConnection().getConnectTimeMillis() / Constants.SECS_TO_MILLIS));
                    message.appendLastParameter(Util.getResourceString(src, "RPL_WHOISIDLE"));
                    src.send(message);
                }
            }
            Message message = new Message(Constants.RPL_ENDOFWHOIS, src);
            message.appendParameter(nick);
            message.appendLastParameter(Util.getResourceString(src, "RPL_ENDOFWHOIS"));
            src.send(message);
        } else {
            // find correct server and ask
        }
    }
    public String getName() {
        return "WHOIS";
    }
    public int getMinimumParameterCount() {
        return 1;
    }
}
