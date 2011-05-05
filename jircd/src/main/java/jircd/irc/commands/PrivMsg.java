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
import jircd.irc.Network;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * Security checks are performed on CTCP messages.
 * @author markhale
 */
public class PrivMsg implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        if(src instanceof User && ((User)src).isLocal())
            src.getHandler().active();
        String msgdest = params[0];
        if (msgdest.charAt(0) == '#') {
            // message to channel
            Channel chan = src.getServer().getNetwork().getChannel(msgdest);
            // TODO: check for +m vs voice, +n vs not-in-chan, etc.
            if (chan == null) {
                Util.sendNoSuchChannelError(src, msgdest);
            } else {
                User user = (User) src;
                final boolean isModerated = chan.isModeSet(Channel.CHANMODE_MODERATED);
                final boolean hasPrivileges = chan.isVoice(user) || chan.isOp(user);
                if(!isModerated || (isModerated && hasPrivileges)) {
                    Message message = new Message(user, "PRIVMSG", chan);
                    message.appendLastParameter(params[1]);
                    chan.send(message, user);
                } else if(isModerated && !hasPrivileges) {
                    Util.sendCannotSendToChannelError(src, msgdest);
                }
            }
        } else if (msgdest.charAt(0) == '&') {
            // message to local channel
            Util.sendNoSuchChannelError(src, msgdest);
        } else {
            // message to user
            User target = findUser(src.getServer().getNetwork(), msgdest);
            // check user exists
            if (target != null) {
                String awayMsg = target.getAwayMessage();
                // check user is not away
                if(awayMsg == null) {
                    // send message
                    String text = params[1];
                    int ctcpStartPos = text.indexOf(Constants.CTCP_DELIMITER);
                    while(ctcpStartPos != -1) {
                        final int ctcpEndPos = text.indexOf(Constants.CTCP_DELIMITER, ctcpStartPos+1);
                        if(ctcpStartPos < ctcpEndPos) {
                            String ctcp = text.substring(ctcpStartPos+1, ctcpEndPos);
                            int spacePos = ctcp.indexOf(' ');
                            int endPos = (spacePos != -1) ? spacePos : ctcp.length();
                            String dataType = ctcp.substring(0, endPos);
                            String action = "";
                            if(dataType.equals("DCC") && spacePos != -1) {
                                // DCC CHAT message
                                int startPos = spacePos+1;
                                spacePos = ctcp.indexOf(' ', startPos);
                                endPos = (spacePos != -1) ? spacePos : ctcp.length();
                                action = ctcp.substring(startPos, endPos);
                            }
                            Util.checkCTCPPermission(dataType, action);
                        }
                        ctcpStartPos = text.indexOf(Constants.CTCP_DELIMITER, ctcpEndPos+1);
                    }
                    Message message = new Message(src, "PRIVMSG", target);
                    message.appendLastParameter(text);
                    target.send(message);
                } else {
                    // send away message
                    Message message = new Message(target, Constants.RPL_AWAY, src);
                    message.appendLastParameter(awayMsg);
                    src.send(message);
                }
            } else {
                Util.sendNoSuchNickError(src, msgdest);
            }
        }
    }
    protected User findUser(Network network, String nick) {
        return network.getUser(nick);
    }
    public String getName() {
        return "PRIVMSG";
    }
    public int getMinimumParameterCount() {
        return 2;
    }
}
