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

import jircd.irc.Channel;
import jircd.irc.Command;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class Invite implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        String nickname = params[0];
        String channame = params[1];
        User luser = src.getServer().getNetwork().getUser(nickname);
        if(luser == null) {
            Util.sendNoSuchNickError(src, nickname);
        } else {
            String awayMsg = luser.getAwayMessage();
            if(awayMsg != null) {
                Message message = new Message(luser, Constants.RPL_AWAY, src);
                message.appendLastParameter(awayMsg);
                src.send(message);
            } else {
                Channel chan = src.getServer().getNetwork().getChannel(channame);
                if(chan == null) {
                    Message message = new Message(src, "INVITE", luser);
                    message.appendParameter(channame);
                    luser.send(message);
                    message = new Message(Constants.RPL_INVITING, src);
                    message.appendParameter(nickname);
                    message.appendParameter(channame);
                    src.send(message);
                } else {
                    if(chan.isOn(luser)) {
                        Message message = new Message(Constants.ERR_USERONCHANNEL, src);
                        message.appendParameter(nickname);
                        message.appendParameter(channame);
                        message.appendLastParameter("is already on channel");
                        src.send(message);
                    } else if(!chan.isOn((User)src)) {
                        Util.sendNotOnChannelError(src, channame);
                    } else {
                        if(chan.isModeSet(Channel.CHANMODE_INVITEONLY) && !chan.isOp((User)src)) {
                            Util.sendChannelOpPrivilegesNeededError(src, channame);
                        } else {
                            chan.invite(luser);
                            Message message = new Message(src, "INVITE", luser);
                            message.appendParameter(channame);
                            luser.send(message);
                            message = new Message(Constants.RPL_INVITING, src);
                            message.appendParameter(nickname);
                            message.appendParameter(channame);
                            src.send(message);
                        }
                    }
                }
            }
        }
    }
    public String getName() {
        return "INVITE";
    }
    public int getMinimumParameterCount() {
        return 2;
    }
}
