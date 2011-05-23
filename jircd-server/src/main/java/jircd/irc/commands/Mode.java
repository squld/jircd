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
public class Mode implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        String modeDest = params[0];
        String newModes = (params.length > 1 ? params[1] : null);
        
        if (Util.isChannelIdentifier(modeDest)) {
            // channel mode
            Channel chanDest = src.getServer().getNetwork().getChannel(modeDest);
            if (chanDest == null) {
                Util.sendNoSuchChannelError(src, modeDest);
            } else {
                User sender = (User) src;
                if (chanDest.isOn(sender)) {
                    String[] modeParams = null;
                    if(newModes != null) {
                        modeParams = new String[params.length-2];
                        for (int i = 0; i < modeParams.length; i++) {
                            modeParams[i] = params[i+2];
                        }
                    }
                    channelMode(sender, chanDest, newModes, modeParams);
                } else {
                    // not on channel
                    Util.sendNotOnChannelError(src, modeDest);
                }
            }
        } else {
            // user mode
            User sender = (User) src;
            User userDest = src.getServer().getNetwork().getUser(modeDest);
            userMode(sender, userDest, newModes);
        }
    }
    private void channelMode(User src, Channel chanDest, String newModes, String[] modeParams) {
        if (newModes != null) {
            if (hasPermission(src, chanDest)) {
                chanDest.processModes(src, newModes, modeParams);
            } else {
                Util.sendChannelOpPrivilegesNeededError(src, chanDest.getName());
            }
        } else {
            Message message = new Message(Constants.RPL_CHANNELMODEIS, src);
            message.appendParameter(chanDest.getName());
            message.appendParameter(chanDest.getModesList());
            src.send(message);
        }
    }
    private void userMode(User src, User userDest, String newModes) {
        if (hasPermission(src, userDest)) {
            if (newModes != null) {
                userDest.processModes(newModes);
            } else {
                Message message = new Message(Constants.RPL_UMODEIS, userDest);
                message.appendParameter(userDest.getModeList());
                src.send(message);
            }
        } else {
            Message message = new Message(Constants.ERR_USERSDONTMATCH, src);
            message.appendLastParameter(Util.getResourceString(src, "ERR_USERSDONTMATCH"));
            src.send(message);
        }
    }
    private boolean hasPermission(User user, Channel context) {
        return context.isOp(user) || user.isModeSet(User.UMODE_OPER);
    }
    private boolean hasPermission(User user, User context) {
        return (user == context) || user.isModeSet(User.UMODE_OPER);
    }
    public String getName() {
        return "MODE";
    }
    public int getMinimumParameterCount() {
        return 1;
    }
}
