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

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * IRC channel.
 * Inner class pattern of Network.
 * @author thaveman
 * @author markhale
 */
public class Channel implements Entity {
    public static final char CHANMODE_PRIVATE    = 'p';
    public static final char CHANMODE_SECRET     = 's';
    public static final char CHANMODE_INVITEONLY = 'i';
    public static final char CHANMODE_TOPICOPS   = 't';
    public static final char CHANMODE_NOEXTERNAL = 'n';
    public static final char CHANMODE_MODERATED  = 'm';
    
    public static final char CHANMODE_OPERATOR   = 'o';
    public static final char CHANMODE_VOICE      = 'v';
    public static final char CHANMODE_BAN        = 'b';
    
    public static final char CHANMODE_LIMIT      = 'l';
    public static final char CHANMODE_KEY        = 'k';
    
    private final String name;
    private final long creationTime;
    private final Modes modes = new Modes();
    protected final Object topicLock = new Object();
    protected String topic = "";
    protected String topicAuthor = "";
    protected long topicTime; // millis
    private String key; // null if none
    private int limit; // 0 if none
    protected final Network network;
    /** (User user, Modes modes) */
    private final ConcurrentHashMap members = new ConcurrentHashMap();
    /** (Server peerServer, Set linkedMembers) */
    private final ConcurrentHashMap peerServers = new ConcurrentHashMap();
    /** set of Bans */
    private final Set bans = new CopyOnWriteArraySet();
    /** set of invited Users */
    private final Set invites = new CopyOnWriteArraySet();
    
    /**
     * Channel ban.
     */
    private class Ban {
        private final String mask;
        private final String who;
        private final long when;
        
        public Ban(String mask, String who) {
            this.mask = mask;
            this.who = who;
            this.when = System.currentTimeMillis();
        }
    }
    
    /**
     * Constructs a new IRC channel.
     */
    public Channel(String name, Network network) {
        if(name == null)
            throw new NullPointerException("Channel name cannot be null");
        this.name = name;
        this.creationTime = System.currentTimeMillis();
        this.network = network;
        network.addChannel(this);
    }
    
    public String getName() {
        return name;
    }
    
    public long getCreationTimeMillis() {
        return creationTime;
    }
    
    public Set getUsers() {
        return Collections.unmodifiableSet(members.keySet());
    }
    public int getCount() {
        return members.size();
    }
    
    public String getTopic() {
        return topic;
    }
    
    public boolean isOn(User usr) {
        return members.containsKey(usr);
    }
    
    private Modes getModes(User usr) {
        return (Modes) members.get(usr);
    }
    
    public void joinUser(User us, String usKey) {
        // check for bans
        if(isBanned(us.toString())) {
            Message message = new Message(Constants.ERR_BANNEDFROMCHAN, us);
            message.appendParameter(name);
            message.appendLastParameter(Util.getResourceString(us, "ERR_BANNEDFROMCHAN"));
            us.send(message);
            return;
        }
        // check for key
        if (this.key != null && this.key.length() > 0) {
            if (!this.key.equals(usKey)) {
                Message message = new Message(Constants.ERR_BADCHANNELKEY, us);
                message.appendParameter(name);
                message.appendLastParameter(Util.getResourceString(us, "ERR_BADCHANNELKEY"));
                us.send(message);
                return;
            }
        }
        // check for member limit
        if (this.limit > 0) {
            if (members.size() >= this.limit) {
                Message message = new Message(Constants.ERR_CHANNELISFULL, us);
                message.appendParameter(name);
                message.appendLastParameter(Util.getResourceString(us, "ERR_CHANNELISFULL"));
                us.send(message);
                return;
            }
        }
        // check for invite
        if (this.isModeSet(CHANMODE_INVITEONLY) && !invites.contains(us)) {
            Message message = new Message(Constants.ERR_INVITEONLYCHAN, us);
            message.appendParameter(name);
            message.appendLastParameter(Util.getResourceString(us, "ERR_INVITEONLYCHAN"));
            us.send(message);
            return;
        }
        
        addUser(us);
        Message message = new Message(us, "JOIN", this);
        sendLocal(message);
        network.send(message, us.getServer());
        if (us.isLocal()) {
            sendNames(us);
            sendTopicInfo(us);
        }
    }
    
    public void addUser(User user) {
        // concurrent - ordering important
        final boolean makeOp = members.isEmpty();
        user.addChannel(this);
        if(!user.isLocal()) {
            Server peerServer = (Server) user.getHandler().getEntity();
            Set linkedMembers = (Set) peerServers.get(peerServer);
            if(linkedMembers == null) {
                linkedMembers = Collections.synchronizedSet(new HashSet());
                peerServers.put(peerServer, linkedMembers);
            }
            linkedMembers.add(user);
        }
        Modes memberModes = new Modes();
        members.put(user, memberModes);
        if(makeOp)
            memberModes.add(Channel.CHANMODE_OPERATOR);
    }
    
    public void removeUser(User user) {
        members.remove(user);
        if(!user.isLocal()) {
            Server peerServer = (Server) user.getHandler().getEntity();
            Set linkedMembers = (Set) peerServers.get(peerServer);
            linkedMembers.remove(user);
            if(linkedMembers.isEmpty())
                peerServers.remove(peerServer);
        }
        user.removeChannel(this);
        if(members.isEmpty())
            remove();
    }
    protected void remove() {
        network.removeChannel(this);
    }
    
    public void addBan(String mask, String who) {
        bans.add(new Ban(mask,who));
    }
    private boolean isBanned(String user) {
        for(Iterator iter = bans.iterator(); iter.hasNext();) {
            Ban ban = (Ban) iter.next();
            if(Util.match(ban.mask, user)) {
                return true;
            }
        }
        return false;
    }
    public void listBans(User towho) {
        for(Iterator iter = bans.iterator(); iter.hasNext();) {
            Ban ban = (Ban) iter.next();
            Message message = new Message(Constants.RPL_BANLIST, towho);
            message.appendParameter(name);
            message.appendParameter(ban.mask);
            message.appendParameter(ban.who);
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, towho.getLocale());
            message.appendParameter(df.format(new Date(ban.when)));
            towho.send(message);
        }
        Message message = new Message(Constants.RPL_ENDOFBANLIST, towho);
        message.appendParameter(name);
        message.appendLastParameter(Util.getResourceString(towho, "RPL_ENDOFBANLIST"));
        towho.send(message);
    }
    
    public boolean removeBan(String mask) {
        for(Iterator iter = bans.iterator(); iter.hasNext();) {
            Ban ban = (Ban) iter.next();
            if(ban.mask.equals(mask)) {
                iter.remove();
                return true;
            }
        }
        return false;
    }
    
    public void invite(User user) {
        invites.add(user);
    }
    
    public final boolean isOp(User usr) {
        Modes memberModes = getModes(usr);
        return (memberModes != null && memberModes.contains(Channel.CHANMODE_OPERATOR));
    }
    public final boolean isVoice(User usr) {
        Modes memberModes = getModes(usr);
        return (memberModes != null && memberModes.contains(Channel.CHANMODE_VOICE));
    }
    
    public void sendTopicInfo(User usr) {
        String tmpTopic, tmpAuthor;
        long tmpTime;
        // safely read topic information
        synchronized(topicLock) {
            tmpTopic = topic;
            tmpAuthor = topicAuthor;
            tmpTime = topicTime;
        }
        if (tmpTopic.length() == 0) {
            Message message = new Message(Constants.RPL_NOTOPIC, usr);
            message.appendParameter(name);
            message.appendLastParameter(Util.getResourceString(usr, "RPL_NOTOPIC"));
            usr.send(message);
        } else {
            Message message = new Message(Constants.RPL_TOPIC, usr);
            message.appendParameter(name);
            message.appendLastParameter(tmpTopic);
            usr.send(message);
            message = new Message(Constants.RPL_TOPICWHOTIME, usr);
            message.appendParameter(name);
            message.appendParameter(tmpAuthor);
            message.appendParameter(Long.toString(tmpTime/Constants.SECS_TO_MILLIS));
            usr.send(message);
        }
    }
    
    public void sendNames(User usr) {
        StringBuffer sb = new StringBuffer();
        for(Iterator iter = members.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            User member = (User) entry.getKey();
            Modes memberModes = (Modes) entry.getValue();
            if (memberModes.contains(Channel.CHANMODE_OPERATOR))
                sb.append(" @");
            else if (memberModes.contains(Channel.CHANMODE_VOICE))
                sb.append(" +");
            else
                sb.append(' ');
            sb.append(member.getNick());
        }
        
        String ournames = (sb.length() > 0 ? sb.substring(1) : ""); // get rid of leading space ' '
        
        String chanPrefix = "=";
        if(isModeSet(CHANMODE_SECRET))
            chanPrefix = "@";
        else if(isModeSet(CHANMODE_PRIVATE))
            chanPrefix = "*";
        
        Message message = new Message(Constants.RPL_NAMREPLY, usr);
        message.appendParameter(chanPrefix);
        message.appendParameter(name);
        message.appendLastParameter(ournames);
        usr.send(message);
        
        message = new Message(Constants.RPL_ENDOFNAMES, usr);
        message.appendParameter(name);
        message.appendLastParameter(Util.getResourceString(usr, "RPL_ENDOFNAMES"));
        usr.send(message);
    }
    
    public void sendLocal(Message message, RegisteredEntity excluded) {
        for(Enumeration iter = members.keys(); iter.hasMoreElements();) {
            User user = (User) iter.nextElement();
            if(user.isLocal() && !user.equals(excluded)) {
                user.send(message);
            }
        }
    }
    public void sendLocal(Message message) {
        sendLocal(message, null);
    }
    
    /**
     * Sends a message to this channel, excluding a specified user.
     */
    public void send(Message message, RegisteredEntity excluded) {
        sendLocal(message, excluded);
        Connection.Handler handler = excluded.getHandler();
        ConnectedEntity excludedPeer = (handler != null ? handler.getEntity() : null);
        for(Enumeration iter = peerServers.keys(); iter.hasMoreElements();) {
            Server server = (Server) iter.nextElement();
            if(!server.equals(excludedPeer)) {
                server.send(message);
            } else {
                Set linkedMembers = (Set) peerServers.get(excludedPeer);
                if(linkedMembers.size() > 1 || !linkedMembers.contains(excluded))
                    server.send(message);
            }
        }
    }
    /**
     * Sends a message to all the users in this channel.
     */
    public void send(Message message) {
        send(message, null);
    }
    
    public void setTopic(User sender, String newTopic) {
        // safely write topic information
        synchronized(topicLock) {
            topic = newTopic;
            topicAuthor = sender.getNick();
            topicTime = System.currentTimeMillis();
        }
        Message message = new Message(sender, "TOPIC", this);
        message.appendLastParameter(newTopic);
        sendLocal(message);
        network.send(message, sender.getServer());
    }
    
    public String getModesList() {
        String modesList = modes.toString();
        StringBuffer modeParams = new StringBuffer();
        
        if(modes.contains(CHANMODE_KEY))
            modeParams.append(' ').append(this.key);
        if(modes.contains(CHANMODE_LIMIT))
            modeParams.append(' ').append(this.limit);
        
        if(modeParams.length() > 0)
            modesList = modesList+modeParams;
        return modesList;
    }
    
    public void processModes(User sender, String modeString, String[] modeParams) {
        if("+b".equals(modeString) && modeParams.length == 0) {
            this.listBans(sender);
            return;
        }
        
        boolean addingMode = true; // are we adding modes (+) or removing (-)
        
        StringBuffer goodModes = new StringBuffer();
        String[] goodParams = new String[modeParams.length];
        int goodParamsCount = 0;
        
        int n = 0; // modeParams index
        
        for (int i = 0; i < modeString.length(); i++) {
            boolean doDo = false;
            
            char modeChar = modeString.charAt(i);
            switch(modeChar) {
                case '+':
                    addingMode = true;
                    goodModes.append('+');
                    break;
                case '-':
                    addingMode = false;
                    goodModes.append('-');
                    break;
                case CHANMODE_LIMIT:
                    if (addingMode) {
                        if (n >= modeParams.length) break;
                        try {
                            int tryLimit = Integer.parseInt(modeParams[n]);
                            limit = tryLimit;
                            goodParams[goodParamsCount++] = modeParams[n];
                            doDo = true;
                        } catch(NumberFormatException nfe) {
                        } finally {
                            n++; // move on to the next parameter
                        }
                    } else {
                        limit = 0;
                        doDo = true;
                    }
                    break;
                case CHANMODE_KEY:
                    if (addingMode) {
                        if (n >= modeParams.length) break;
                        String tryKey = modeParams[n];
                        n++;
                        if (Util.isIRCString(tryKey)) {
                            key = tryKey;
                            goodParams[goodParamsCount++] = tryKey;
                            doDo = true;
                        }
                    } else {
                        if (n >= modeParams.length) break;
                        String tryKey = modeParams[n];
                        n++;
                        if (key.equalsIgnoreCase(tryKey)) {
                            key = null;
                            goodParams[goodParamsCount++] = tryKey;
                            doDo = true;
                        }
                    }
                    break;
                case CHANMODE_BAN:
                    if (n >= modeParams.length) break;
                    String banMask = modeParams[n];
                    n++;
                    if (addingMode) {
                        this.addBan(banMask, sender.getNick());
                        doDo = true;
                        goodParams[goodParamsCount++] = banMask;
                    } else {
                        if (this.removeBan(banMask)) {
                            doDo = true;
                            goodParams[goodParamsCount++] = banMask;
                        } else break;
                    }
                    break;
                case CHANMODE_OPERATOR:
                case CHANMODE_VOICE:
                    if (n >= modeParams.length) break;
                    String nick = modeParams[n];
                    n++;
                    User member = network.getUser(nick);
                    if (member != null) {
                        Modes memberModes = this.getModes(member);
                        if (memberModes != null) {
                            doDo = true;
                            goodParams[goodParamsCount++] = nick;
                            if(addingMode)
                                memberModes.add(modeChar);
                            else
                                memberModes.remove(modeChar);
                        } else {
                            Util.sendUserNotInChannelError(sender, nick, this.name);
                        }
                    } else {
                        Util.sendNoSuchNickError(sender, nick);
                    }
                    break;
                default:
                    doDo = true;
            }
            
            if (doDo) {
                try {
                    if (addingMode)
                        modes.add(modeChar);
                    else
                        modes.remove(modeChar);
                    goodModes.append(modeChar);
                } catch(IllegalArgumentException e) {
                    //Invalid Mode Character Detected!
                    Message message = new Message(Constants.ERR_UNKNOWNMODE, sender);
                    message.appendParameter(Character.toString(modeChar));
                    message.appendLastParameter("is unknown mode char to me for "+name);
                    sender.send(message);
                }
            }
        }
        
        if (goodModes.length() > 1) {
            Message message = new Message(sender, "MODE", this);
            message.appendParameter(goodModes.toString());
            for(int i=0; i<goodParamsCount; i++)
                message.appendParameter(goodParams[i]);
            sendLocal(message);
            network.send(message, sender.getServer());
        }
    }
    
    public boolean isModeSet(char mode) {
        return modes.contains(mode);
    }
    
    public String toString() {
        return name+": "+topic;
    }
}
