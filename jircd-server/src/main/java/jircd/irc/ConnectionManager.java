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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a set of connections.
 * @author markhale
 */
public class ConnectionManager implements ConnectionManagerMBean {
    private final ConcurrentHashMap connections = new ConcurrentHashMap(); // use as a set
    private int maxConnCount;
    
    /**
     * Notifies the manager that a connection has been opened.
     */
    void connectionOpened(Connection conn) {
        connections.put(conn, conn);
        final int count = getConnectionCount();
        if(count > maxConnCount)
            maxConnCount = count;
    }
    /**
     * Notifies the manager that a connection has been closed.
     */
    void connectionClosed(Connection conn) {
        connections.remove(conn);
    }
    
    public Set getConnections() {
        return Collections.unmodifiableSet(connections.keySet());
    }
    public int getConnectionCount() {
        return connections.size();
    }
    public int getMaxConnectionCount() {
        return maxConnCount;
    }
    
    public long getBytesSent() {
        long total = 0;
        for(Enumeration iter = connections.keys(); iter.hasMoreElements(); ) {
            Connection conn = (Connection) iter.nextElement();
            total += conn.getBytesSent();
        }
        return total;
    }
    
    public int getLinesSent() {
        int total = 0;
        for(Enumeration iter = connections.keys(); iter.hasMoreElements(); ) {
            Connection conn = (Connection) iter.nextElement();
            total += conn.getLinesSent();
        }
        return total;
    }
    
    public long getBytesRead() {
        long total = 0;
        for(Enumeration iter = connections.keys(); iter.hasMoreElements(); ) {
            Connection conn = (Connection) iter.nextElement();
            total += conn.getBytesRead();
        }
        return total;
    }
    
    public int getLinesRead() {
        int total = 0;
        for(Enumeration iter = connections.keys(); iter.hasMoreElements(); ) {
            Connection conn = (Connection) iter.nextElement();
            total += conn.getLinesRead();
        }
        return total;
    }
}
