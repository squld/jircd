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
import jircd.irc.CommandContext;
import jircd.irc.ConnectedEntity;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.RegistrationCommand;
import jircd.irc.Server;
import jircd.irc.UnregisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class Nick implements RegistrationCommand {
    private static final int MINIMUM_PARAM_COUNT_SERVER = 7;
    protected final jIRCdMBean jircd;
    
    public Nick(jIRCdMBean jircd) {
        this.jircd = jircd;
    }
    public final void invoke(final RegisteredEntity src, String[] params) {
        if (src instanceof User) {
            invoke((User) src, params);
        } else if (src instanceof Server) {
            invoke((Server) src, params);
        }
    }
    public final void invoke(final UnregisteredEntity src, String[] params) {
        String newNick = params[0];
        if (Util.isNickName(newNick)) {
            if (src.getServer().getNetwork().getUser(newNick) == null) {
                src.setName(newNick);
                String[] userParams = src.getParameters();
                if (userParams != null) {
                    // re-invoke USER command if received USER before NICK
                    CommandContext ctx = (CommandContext) jircd.getCommandContext("USER");
                    RegistrationCommand command = (RegistrationCommand) ctx.getCommand();
                    command.invoke(src, userParams);
                }
            } else {
                sendNickNameInUseError(src, newNick);
            }
        } else {
            sendErroneousNickNameError(src, newNick);
        }
    }
    private void invoke(User src, String[] params) {
        // user is requesting a change of nick
        String newNick = params[0];
        if (Util.isNickName(newNick)) {
            User newUser = src.getServer().getNetwork().getUser(newNick);
            if (newUser != null) {
                sendNickNameInUseError(src, newNick);
            } else {
                src.changeNick(newNick);
            }
        } else {
            sendErroneousNickNameError(src, newNick);
        }
    }
    private void invoke(Server src, String[] params) {
        if (params.length == MINIMUM_PARAM_COUNT_SERVER) {
            String nick = params[0];
            int hopcount = Integer.parseInt(params[1]);
            String ident = params[2];
            String host = params[3];
            int token = Integer.parseInt(params[4]);
            String modes = params[5];
            String desc = params[6];
            Server userServer = src.getNetwork().getServer(token);
            User user = new User(nick, hopcount, ident, host, desc, userServer);
        } else {
            // too few parameters
            Util.sendNeedMoreParamsError(src, getName());
        }
    }
    private static void sendNickNameInUseError(ConnectedEntity src, String nick) {
        Message message = new Message(Constants.ERR_NICKNAMEINUSE, src);
        message.appendParameter(nick);
        message.appendLastParameter(Util.getResourceString(src, "ERR_NICKNAMEINUSE"));
        src.send(message);
    }
    private static void sendErroneousNickNameError(ConnectedEntity src, String nick) {
        Message message = new Message(Constants.ERR_ERRONEUSNICKNAME, src);
        message.appendParameter(nick);
        message.appendLastParameter(Util.getResourceString(src, "ERR_ERRONEUSNICKNAME"));
        src.send(message);
    }
    public String getName() {
        return "NICK";
    }
    public int getMinimumParameterCount() {
        return 1;
    }
}
