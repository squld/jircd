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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An IRC server.
 * Inner class pattern of Network.
 * @author thaveman
 * @author markhale
 */
public class Server extends RegisteredEntity {
    /** (String nickName, User user) */
    private final Map users = new ConcurrentHashMap();
    private final String name;
    private final String description;
    protected final Server route;
    protected final Network network;
    protected final int token;
    
    /**
     * Constructs an IRC server linked to another IRC server.
     * This server is added to the network of the other.
     */
    public Server(String name, int hopcount, int token, String description, Server route) {
        this(name, hopcount, token, description, route, route.getHandler(), route.getNetwork());
    }
    /**
     * Constructs a peer server.
     * This server is added to the network of the other.
     */
    public Server(UnregisteredEntity unk, int token, String description) {
        this(unk.getName(), 1, token, description, unk.getServer(), unk.getHandler(), unk.getServer().getNetwork());
        setLocale(unk.getLocale());
    }
    /**
     * Constructs an IRC server (0 hops) on an IRC network.
     * This server is added to the network.
     * This should only be used to construct the local jIRCd server.
     */
    public Server(String name, int token, String description, Network network) {
        this(name, 0, token, description, null, null, network);
    }
    private Server(String name, int hopcount, int token, String description, Server route, Connection.Handler handler, Network network) {
        super(handler, hopcount);
        if(network == null)
            throw new NullPointerException("The network cannot be null");
        this.name = name;
        this.token = token;
        this.description = description;
        this.route = route;
        this.network = network;
        this.network.addServer(this);
    }
    
    /**
     * Returns the server's name.
     */
    public final String getName() {
        return name;
    }
    /** ID */
    public String toString() {
        return getName();
    }
    
    /**
     * Returns true if this server is a peer (1 hop).
     */
    public final boolean isPeer() {
        return (hopCount == 1);
    }
    
    public final Collection getUsers() {
        return Collections.unmodifiableCollection(users.values());
    }
    /**
     * Returns the network this server is connected to.
     * This method should never return null, to always ensure a reference to the network
     * can be obtained with <code>Server.getNetwork()</code>.
     */
    public final Network getNetwork() {
        return network;
    }
    /**
     * Returns the server this server is connected to.
     */
    public final Server getServer() {
        return route;
    }
    
    public final String getDescription() {
        return description;
    }
    public final int getToken() {
        return token;
    }

    /** User hook */
    void addUser(User user) {
        users.put(user.getNick().toLowerCase(), user);
    }
    public final User getUser(String nick) {
        return (User) users.get(nick.toLowerCase());
    }
    /** User hook */
    protected void removeUser(User usr) {
        users.remove(usr.getNick().toLowerCase());
    }
    
    public void changeUserNick(User user, String oldnick, String newnick) {
        users.put(newnick.toLowerCase(), user);
        users.remove(oldnick.toLowerCase());
        Message message = new Message(user, "NICK");
        message.appendParameter(newnick);
        for(Iterator iter = users.values().iterator(); iter.hasNext();) {
            User iusr = (User) iter.next();
            iusr.send(message);
        }
    }
    
    public final int getUserCount(char mode, boolean isSet) {
        int count = 0;
        for(Iterator iter = users.values().iterator(); iter.hasNext();) {
            User user = (User) iter.next();
            if (user.isModeSet(mode) == isSet) {
                count++;
            }
        }
        return count;
    }
    
    public void send(Message msg) {
        StringBuffer buf = new StringBuffer();
        // append prefix
        ConnectedEntity sender = msg.getSender();
        if(sender != null)
            buf.append(':').append(sender.getName()).append(' ');
        
        // append command
        buf.append(msg.getCommand());
        
        // append parameters
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
        handler.sendMessage(buf.toString());
    }
}
