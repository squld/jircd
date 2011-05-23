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

/**
 * IRC message format.
 * A message consists of three parts: a sender, a command, and zero or more parameters.
 * @author markhale
 */
public final class Message {
    protected final ConnectedEntity from;
    protected final String command;
    protected String[] params = new String[3];
    protected int paramCount = 0;
    protected boolean hasLast = false;
    
    /**
     * Creates a message.
     * @param from can be null
     */
    public Message(ConnectedEntity from, String command) {
        if(!Util.isCommandIdentifier(command))
            throw new IllegalArgumentException("Invalid command name");
        this.from = from;
        this.command = command;
    }
    public Message(String command) {
        this(null, command);
    }
    /**
     * Constructs a "numeric reply" type of message.
     */
    public Message(ConnectedEntity from, String command, ConnectedEntity target) {
        this(from, command);
        appendParameter(target.getName());
    }
    /**
     * Constructs a "numeric reply" type of message.
     */
    public Message(String command, ConnectedEntity target) {
        this(target.getServer(), command, target);
    }
    /**
     * Creates a message sent to a channel.
     */
    public Message(ConnectedEntity from, String command, Channel target) {
        this(from, command);
        appendParameter(target.getName());
    }
    
    /**
     * Returns the sender of this message.
     */
    public ConnectedEntity getSender() {
        return from;
    }
    /**
     * Returns the command of this message.
     */
    public String getCommand() {
        return command;
    }
    private void addParam(String param) {
        if(paramCount == params.length) {
            int newLength = 2*(paramCount+1);
            if(newLength > Constants.MAX_MESSAGE_PARAMETERS)
                newLength = Constants.MAX_MESSAGE_PARAMETERS;
            String[] old = params;
            params = new String[newLength];
            System.arraycopy(old, 0, params, 0, paramCount);
        }
        params[paramCount++] = param;
    }
    /**
     * Appends a parameter.
     */
    public Message appendParameter(String param) {
        if(hasLast)
            throw new IllegalStateException("The last parameter has already been appended");
        if(!Util.isParameter(param))
            throw new IllegalArgumentException("Use appendLastParameter() instead");
        addParam(param);
        return this;
    }
    /**
     * Explicitly appends the last parameter.
     */
    public Message appendLastParameter(String param) {
        if(hasLast)
            throw new IllegalStateException("The last parameter has already been appended");
        addParam(param);
        hasLast = true;
        return this;
    }
    public String getParameter(int n) {
        return params[n];
    }
    public int getParameterCount() {
        return paramCount;
    }
    /**
     * Returns true if there is an explicit last parameter.
     */
    public boolean hasLastParameter() {
        return hasLast;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append('[').append(from);
        buf.append(", ").append(command).append(", [");
        int lastIndex = paramCount-1;
        for(int i=0; i<lastIndex; i++)
            buf.append(params[i]).append(", ");
        buf.append(params[lastIndex]).append("], ");
        buf.append(hasLast).append(']');
        return buf.toString();
    }
}
