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
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An IRC network.
 * @author markhale
 */
public final class Network implements Entity {
    private String name;
    
    /** Servers on this network.
     * (String name, Server server) */
    private final ConcurrentHashMap servers = new ConcurrentHashMap();
    
    /** (Integer token, Server server) */
    private final ConcurrentHashMap serverTokens = new ConcurrentHashMap();
    
    /** All channels on this network.
     * (String name, Channel channel) */
    private final ConcurrentHashMap channels = new ConcurrentHashMap();
    
    public Network(String name) {
        this.name = name;
    }
    public final String getName() {
        return name;
    }
    
    public Collection getServers() {
        return Collections.unmodifiableCollection(servers.values());
    }
    /**
     * Adds a server to this network.
     */
    void addServer(Server server) {
        servers.put(server.getName().toLowerCase(), server);
        serverTokens.put(new Integer(server.getToken()), server);
    }
    public Server getServer(String name) {
        return (Server) servers.get(name.toLowerCase());
    }
    public Server getServer(int token) {
        return (Server) serverTokens.get(new Integer(token));
    }
    void removeServer(Server server) {
        servers.remove(server.getName().toLowerCase());
        serverTokens.remove(new Integer(server.getToken()));
    }
    
    public Collection getChannels() {
        return Collections.unmodifiableCollection(channels.values());
    }
    /**
     * Adds a channel to this network.
     */
    void addChannel(Channel channel) {
        channels.put(channel.getName().toLowerCase(), channel);
    }
    public Channel getChannel(String name) {
        return (Channel) channels.get(name.toLowerCase());
    }
    void removeChannel(Channel channel) {
        channels.remove(channel.getName().toLowerCase());
    }
    
    /**
     * Gets a user on this network.
     * @return null if nick does not exist on the network.
     */
    public User getUser(String nick) {
        for(Enumeration iter = servers.elements(); iter.hasMoreElements();) {
            Server server = (Server) iter.nextElement();
            User user = server.getUser(nick);
            if(user != null)
                return user;
        }
        return null;
    }
    
    public int getUserCount(char mode, boolean isSet) {
        int count = 0;
        for(Enumeration iter = servers.elements(); iter.hasMoreElements();) {
            Server server = (Server) iter.nextElement();
            count += server.getUserCount(mode, isSet);
        }
        return count;
    }
    
    public void send(Message message, Server excluded) {
        Connection.Handler handler = excluded.getHandler();
        ConnectedEntity excludedPeer = (handler != null ? handler.getEntity() : null);
        for(Enumeration iter = servers.elements(); iter.hasMoreElements();) {
            Server server = (Server) iter.nextElement();
            if(server.isPeer() && !server.equals(excludedPeer)) {
                // send to local servers for forwarding
                server.send(message);
            }
        }
    }
    /**
     * Sends a message to all the servers on this network.
     */
    public void send(Message message) {
        send(message, null);
    }
    
    public String toString() {
        return name;
    }
}
