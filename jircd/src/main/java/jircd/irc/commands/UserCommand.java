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

import java.util.Date;
import java.util.Iterator;

import jircd.jIRCdMBean;
import jircd.irc.CommandContext;
import jircd.irc.Connection;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.RegistrationCommand;
import jircd.irc.Server;
import jircd.irc.UnregisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

import org.apache.log4j.Logger;

/**
 * @author markhale
 */
public class UserCommand implements RegistrationCommand {
    private static final Logger logger = Logger.getLogger(Error.class);
    protected final jIRCdMBean jircd;
    
    public UserCommand(jIRCdMBean jircd) {
        this.jircd = jircd;
    }
    public final void invoke(final RegisteredEntity src, String[] params) {
        if(src instanceof Server) {
            invoke((Server) src, params);
        } else {
            Util.sendAlreadyRegisteredError(src);
        }
    }
    public final void invoke(final UnregisteredEntity src, String[] params) {
        String nick = src.getName();
        if(UnregisteredEntity.NO_NAME.equals(nick)) {
            // if we have yet to receive a NICK command, remember these USER parameters
            src.setParameters(params);
        } else {
            // already received a NICK command, so can complete login
            Connection.Handler handler = src.getHandler();
            final Connection connection = handler.getConnection();
            if(checkPass(connection, src.getPass())) {
                String username = params[0];
                int modeFlags;
                try {
                    // RFC 2812
                    modeFlags = Integer.parseInt(params[1]);
                } catch(NumberFormatException nfe) {
                    // RFC 1459
                    modeFlags = 0;
                }
                String unused = params[2];
                String desc = params[3];
                User user = createUser(src, username, desc);
                handler.login(user);
                Server thisServer = src.getServer();
                // broadcast new user to other servers on the network
                for(Iterator iter = thisServer.getNetwork().getServers().iterator(); iter.hasNext(); ) {
                    Server server = (Server) iter.next();
                    if(server.isPeer()) {
                        sendUser(server, user);
                    }
                }
                
                Message message = new Message(Constants.RPL_WELCOME, user);
                message.appendLastParameter("Welcome to the " + thisServer.getNetwork().getName() + " " + nick + "!" + username + "@" + connection.getRemoteHost());
                user.send(message);
                
                message = new Message(Constants.RPL_YOURHOST, user);
                message.appendLastParameter("Your host is " + thisServer.getName() + ", running version " + jircd.getVersion());
                user.send(message);
                
                message = new Message(Constants.RPL_CREATED, user);
                message.appendLastParameter("This server was created " + new Date(jircd.getStartTimeMillis()));
                user.send(message);
                
                message = new Message(Constants.RPL_MYINFO, user);
                message.appendLastParameter(thisServer.getName() + " " + jircd.getVersion() + " - -");
                user.send(message);
                
                message = new Message(Constants.RPL_ISUPPORT, user);
                message.appendParameter("NICKLEN="+Constants.MAX_NICK_LENGTH);
                message.appendParameter("CHANNELLEN="+Constants.MAX_CHANNEL_LENGTH);
                message.appendParameter("TOPICLEN="+Constants.MAX_TOPIC_LENGTH);
                message.appendParameter("PREFIX=(ov)@+");
                message.appendParameter("CHANTYPES=#");
                message.appendParameter("CHANMODES=b,k,l,imt");
                message.appendParameter("CASEMAPPING=ascii");
                message.appendParameter("NETWORK=" + thisServer.getNetwork().getName());
                message.appendLastParameter("are supported by this server");
                user.send(message);
                
                CommandContext ctx = jircd.getCommandContext("LUSERS");
                ctx.getCommand().invoke(user, null);
                ctx = jircd.getCommandContext("MOTD");
                ctx.getCommand().invoke(user, null);
                
                StringBuffer modes = new StringBuffer('+');
                if((modeFlags & 2) != 0)
                    modes.append('w');
                if((modeFlags & 8) != 0)
                    modes.append('i');
                if(modes.length() > 1)
                    user.processModes(modes.toString());
            } else {
                logger.warn("Invalid password");
                src.disconnect("Invalid password");
            }
        }
    }
    private boolean checkPass(Connection connection, String[] passParams) {
        String expectedPassword = jircd.getProperty("jircd.accept."+connection.getRemoteAddress()+'#'+connection.getLocalPort());
        if(expectedPassword != null) {
            String password = (passParams != null && passParams.length > 0) ? passParams[0] : null;
            return expectedPassword.equals(password);
        } else {
            return true;
        }
    }
    protected User createUser(UnregisteredEntity unk, String username, String desc) {
        return new User(unk, username, desc);
    }
    protected void sendUser(Server server, User user) {
        Util.sendUser(server, user.getServer(), user);
    }
    private void invoke(Server src, String[] params) {
        String username = params[0];
        String hostname = params[1];
        String servername = params[2];
        String desc = params[3];
    }
    public String getName() {
        return "USER";
    }
    public int getMinimumParameterCount() {
        return 4;
    }
}
