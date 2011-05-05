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

package jircd.ircx;

import java.util.Properties;

import jircd.irc.Channel;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.Network;
import jircd.irc.User;

/**
 * @author markhale
 */
public class ChannelX extends Channel {
    public static final char CHANMODE_REGISTERED = 'r';
    
    private final Properties props = new Properties();
    
    public ChannelX(String name, Network network) {
        super(name, network);
        props.setProperty("NAME", name);
        props.setProperty("OID", "0");
        props.setProperty("CREATION", Long.toString(getCreationTimeMillis()/Constants.SECS_TO_MILLIS));
    }
    public void setProperty(User sender, String prop, String data) {
        if("NAME".equals(prop) || "OID".equals(prop) || "CREATION".equals(prop))
            return;
        else if("TOPIC".equals(prop))
            setTopic(sender, data);
        else
            props.setProperty(prop, data);
    }
    public String getProperty(String prop) {
        return props.getProperty(prop, "");
    }
    public void setTopic(User sender, String newTopic) {
        super.setTopic(sender, newTopic);
        props.setProperty("TOPIC", topic);
    }
    public void addUser(User user) {
        super.addUser(user);
        String entryMsg = props.getProperty("ONJOIN");
        if(entryMsg != null && entryMsg.length() > 0) {
            Message msg = new Message(user.getServer(), "PRIVMSG");
            msg.appendParameter(user.getNick());
            msg.appendLastParameter(entryMsg);
            user.send(msg);
        }
    }
    public void removeUser(User user) {
        super.removeUser(user);
        String partMsg = props.getProperty("ONPART");
        if(partMsg != null && partMsg.length() > 0) {
            Message msg = new Message(user.getServer(), "NOTICE");
            msg.appendParameter(user.getNick());
            msg.appendLastParameter(partMsg);
            user.send(msg);
        }
    }
    protected void remove() {
        if(!isModeSet(CHANMODE_REGISTERED))
            super.remove();
    }
}
