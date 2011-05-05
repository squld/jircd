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
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class List implements Command {
    public void invoke(RegisteredEntity src, String[] params) {
        // TODO: wildcards and things, Iterator should be synchronized
        User user = (User) src;
        
        Message message = new Message(Constants.RPL_LISTSTART, src);
        message.appendParameter("Channel");
        message.appendLastParameter("Users  Name");
        src.send(message);
        
        for(Iterator iter = src.getServer().getNetwork().getChannels().iterator(); iter.hasNext(); ) {
            Channel channel = (Channel) iter.next();
            final boolean isOn = channel.isOn(user);
            final boolean isSecret = channel.isModeSet(Channel.CHANMODE_SECRET);
            if (!isSecret || (isSecret && isOn)) {
                String topic = channel.getTopic();
                if(channel.isModeSet(Channel.CHANMODE_PRIVATE) && !isOn) {
                    topic = "Prv";
                }
                message = new Message(Constants.RPL_LIST, src);
                message.appendParameter(channel.getName());
                message.appendParameter(Integer.toString(channel.getCount()));
                message.appendLastParameter(topic);
                src.send(message);
            }
        }
        
        message = new Message(Constants.RPL_LISTEND, src);
        message.appendLastParameter(Util.getResourceString(src, "RPL_LISTEND"));
        src.send(message);
    }
    public String getName() {
        return "LIST";
    }
    public int getMinimumParameterCount() {
        return 0;
    }
}
