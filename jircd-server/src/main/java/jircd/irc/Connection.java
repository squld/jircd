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

import java.net.Socket;

import jircd.jIRCdMBean;

/**
 * Abstraction for socket connection I/O.
 * @author markhale
 */
public abstract class Connection {
    protected final ConnectionManager manager;
    protected final Socket socket;
    private final long connectTime;
    protected Handler handler;
    
    protected Connection(Socket socket, ConnectionManager manager) {
        this.socket = socket;
        this.manager = manager;
        connectTime = System.currentTimeMillis();
    }
    public final String getRemoteAddress() {
        return socket.getInetAddress().getHostAddress();
    }
    public final String getRemoteHost() {
        return socket.getInetAddress().getHostName();
    }
    public final int getRemotePort() {
        return socket.getPort();
    }
    public final String getLocalAddress() {
        return socket.getLocalAddress().getHostAddress();
    }
    public final String getLocalHost() {
        return socket.getLocalAddress().getHostName();
    }
    public final int getLocalPort() {
        return socket.getLocalPort();
    }
    public final boolean isSecure() {
        return (socket instanceof javax.net.ssl.SSLSocket);
    }
    
    public abstract long getBytesSent();
    public abstract int getLinesSent();
    public abstract long getBytesRead();
    public abstract int getLinesRead();
    public final long getConnectTimeMillis() {
        return connectTime;
    }
    
    public final void setHandler(Handler handler) {
        if(handler == null)
            throw new NullPointerException("The handler cannot be null");
        this.handler = handler;
    }
    public final Handler getHandler() {
        return handler;
    }
    protected abstract void writeLine(String s);
    protected abstract void close();
    
    public String toString() {
        return '[' + socket.getInetAddress().getHostAddress() + ':' + socket.getPort() + ',' + socket.getClass().getName() + ',' + handler + ']';
    }
    
    /**
     * Abstraction for message-layer protocol.
     * This class is responsible for maintaining a connection,
     * and dispatching Messages.
     * The Entity of a Handler is UnregisteredEntity until it logins.
     */
    public static class Handler {
        protected final Connection connection;
        protected final jIRCdMBean jircd; // the server we are connected to
        private ConnectedEntity entity;
        private MessageFactory messageFactory;
        private long lastPing = 0; // millis
        private long lastPong = 0; // millis
        private long latency = 0; // millis
        private long lastActive = 0; // millis
        private final long pingTimeout; // millis
        
        public Handler(jIRCdMBean jircd, Connection connection) {
            if(connection == null)
                throw new NullPointerException("The connection cannot be null");
            this.jircd = jircd;
            this.connection = connection;
            pingTimeout = Constants.SECS_TO_MILLIS * Integer.parseInt(jircd.getProperty("jircd.ping.timeout", "120"));
            Server thisServer = jircd.getServer();
            entity = new UnregisteredEntity(this, thisServer);
            entity.setLocale(Util.lookupLocale(connection.getRemoteAddress()));
            messageFactory = new MessageFactory(thisServer.getNetwork());
            // silent ping to unregistered entity
            lastPing = System.currentTimeMillis();
        }
        /**
         * Registers/logs-in using the specified entity.
         * This should be used by to register Entity implementations.
         */
        public final void login(ConnectedEntity newEntity) {
            if(newEntity == null)
                throw new NullPointerException("The source cannot be null");
            
            if(entity instanceof UnregisteredEntity) {
                if(newEntity.getHandler() != this)
                    throw new IllegalArgumentException("The handler of " + newEntity.toString() + " must be " + toString() + " but it was " + newEntity.getHandler().toString());
                if(newEntity.getServer() != entity.getServer())
                    throw new IllegalArgumentException("The server of " + newEntity.toString() + " must be " + entity.getServer().toString() + " but it was " + newEntity.getServer().toString());
                entity = newEntity;
                // silent pong from unregistered entity
                lastPong = System.currentTimeMillis();
            } else {
                Util.sendAlreadyRegisteredError(newEntity);
            }
        }
        public final Connection getConnection() {
            return connection;
        }
        public final ConnectedEntity getEntity() {
            return entity;
        }
        public final void setMessageFactory(MessageFactory messageFactory) {
            if(messageFactory == null)
                throw new NullPointerException("The message factory cannot be null");
            this.messageFactory = messageFactory;
        }
        public final MessageFactory getMessageFactory() {
            return messageFactory;
        }
        
        /**
         * Pings this connection.
         * Returns false on ping timeout.
         */
        public final boolean ping() {
            final long currentTime = System.currentTimeMillis();
            if (lastPong >= lastPing) { // got a response previously
                if (currentTime - lastPong > pingTimeout) { // more than timeout seconds?
                    // send ping
                    lastPing = currentTime;
                    Server server = entity.getServer();
                    if(entity instanceof Server) {
                        Message message = new Message(server, "PING");
                        message.appendParameter(server.getName());
                        message.appendLastParameter(entity.getName());
                        entity.send(message);
                    } else {
                        Message message = new Message("PING");
                        message.appendLastParameter(server.getName());
                        entity.send(message);
                    }
                }
                return true;
            } else { // I have received no pong since the last ping
                // more than timeout seconds since last ping?
                return (currentTime - lastPing) <= pingTimeout;
            }
        }
        public final void pong() {
            lastPong = System.currentTimeMillis();
            latency = lastPong - lastPing;
        }
        
        public final void active() {
            lastActive = System.currentTimeMillis();
        }
        public final long getIdleTimeMillis() {
            return System.currentTimeMillis() - lastActive;
        }
        
        final void handleLine(String line) {
            // if there has been a pong since the last ping, then don't try to ping again
            if (lastPong > lastPing)
                lastPong = System.currentTimeMillis();
            
            line = line.trim();
            if (line.length() > Constants.MAX_MESSAGE_LENGTH)
                line = line.substring(0, Constants.MAX_MESSAGE_LENGTH); // max length per RFC
            
            Message msg = messageFactory.createMessage(entity, line);
            String cmd = msg.getCommand();
            if (Character.isDigit(cmd.charAt(0))) {
                // numeric reply
                handleReply(msg);
            } else {
                handleCommand(msg);
            }
        }
        protected void handleReply(Message message) {}
        protected void handleCommand(Message message) {
            jircd.invokeCommand(message);
        }
        public final void sendMessage(String message) {
            connection.writeLine(message);
        }
        public final void disconnect() {
            connection.close();
        }
        public String toString() {
            return entity.toString();
        }
    }
}
