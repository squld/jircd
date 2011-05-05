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
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * A user on a server.
 * Inner class pattern of Server.
 * @author thaveman
 * @author markhale
 */
public class User extends RegisteredEntity {
    public static final char UMODE_INVISIBLE = 'i';
    public static final char UMODE_SNOTICE   = 's';
    public static final char UMODE_WALLOPS   = 'w';
    public static final char UMODE_OPER      = 'o';
    public static final char UMODE_AWAY      = 'a';
    
    private String nickName;
    private long nickTimestamp;
    private final String ident;
    private final String displayHostName;
    private final String hostName;
    private final String description;
    private String awayMsg;
    protected final Server server;
    /** set of Channel */
    private final Set channels = new CopyOnWriteArraySet();
    private final Modes modes = new Modes();
    
    /**
     * Constructor for remote users (i.e. attached via another server).
     * This user is added to the server.
     */
    public User(String nickname, int hopcount, String ident, String hostname, String description, Server server) {
        this(nickname, hopcount, ident, hostname, description, server, server.getHandler());
    }
    /**
     * Constructor for local users (0 hops).
     * This user is added to the server.
     */
    public User(UnregisteredEntity unk, String ident, String desc) {
        this(unk.getName(), 0, ident, unk.getHandler().getConnection().getRemoteHost(), desc, unk.getServer(), unk.getHandler());
        setLocale(unk.getLocale());
    }
    /**
     * Constructs a user.
     * This user is added to the server.
     */
    private User(String nickname, int hopcount, String ident, String hostname, String description, Server server, Connection.Handler handler) {
        super(handler, hopcount);
        if(server == null)
            throw new NullPointerException("The server cannot be null");
        setNick(nickname);
        this.ident = ident;
        this.displayHostName = maskHostName(hostname);
        this.hostName = hostname;
        this.description = description;
        this.server = server;
        this.server.addUser(this);
    }
    protected void setNick(String nick) {
        nickName = nick;
        nickTimestamp = System.currentTimeMillis();
    }
    
    protected String maskHostName(String host) {
        // first see if it's an IP or name
        String[] dotnames = Util.split(host, '.');
        boolean isIP = (dotnames.length == 4);
        if(isIP) {
            for(int i=0; i<dotnames.length; i++) {
                String s = dotnames[i];
                for(int j=0; j<s.length(); j++) {
                    char ch = s.charAt(j);
                    if(!(ch >= '0' && ch <= '9')) {
                        isIP = false;
                        break;
                    }
                }
            }
        }
        
        String appendage = Integer.toHexString(Util.RANDOM.nextInt(0xEFFFFF) + 0x100000);
        String maskedHost;
        if (isIP) {
            // IP
            int p = host.lastIndexOf('.');
            maskedHost = host.substring(0, ++p) + appendage;
        } else if (dotnames.length > 1) {
            // dotted name
            int p = host.indexOf('.');
            maskedHost = appendage + host.substring(p);
        } else {
            // simple name
            maskedHost = appendage + ".host";
        }
        return maskedHost;
    }
    
    public void processModes(String modeString) {
        boolean addingMode = true; // are we adding modes (+) or removing (-)
        
        StringBuffer goodModes = new StringBuffer();
        
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
                    
                    // add other processing here for modes that may not want to be
                    // set under certain conditions, etc.
                case UMODE_OPER: // user can't set himself +o, the server must do it
                    if(!addingMode) {
                        doDo = true;
                    }
                    break;
                case UMODE_AWAY: // user can't set himself +/-a, the server must do it
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
                    Util.sendUserModeUnknownFlagError(this);
                }
            }
        }
        
        if (goodModes.length() > 1 && isLocal()) {
            Message message = new Message(this, "MODE", this);
            message.appendParameter(goodModes.toString());
            send(message);
        }
    }
    
    public boolean isModeSet(char mode) {
        return modes.contains(mode);
    }
    public final String getModeList() {
        return modes.toString();
    }
    
    public final void loginOperator(CallbackHandler handler) {
        // lazy instantiation
        if(subject == null)
            subject = new Subject();
        try {
            LoginContext loginCtx = new LoginContext("OperatorLogin", subject, handler);
            loginCtx.login();
            modes.add(UMODE_OPER);
            Message message = new Message(Constants.RPL_YOUREOPER, this);
            message.appendLastParameter(Util.getResourceString(this, "RPL_YOUREOPER"));
            send(message);
        } catch(LoginException le) {
            Message message = new Message(Constants.ERR_NOOPERHOST, this);
            message.appendLastParameter(Util.getResourceString(this, "ERR_NOOPERHOST"));
            send(message);
        }
    }
    
    public void setAwayMessage(String msg) {
        awayMsg = msg;
        if(awayMsg != null)
            modes.add(UMODE_AWAY);
        else
            modes.remove(UMODE_AWAY);
    }
    public String getAwayMessage() {
        return awayMsg;
    }
    
    public final Set getChannels() {
        return Collections.unmodifiableSet(channels);
    }
    
    /** ID */
    public String toString() {
        return getNick() + '!' + getIdent() + '@' + getDisplayHostName();
    }
    /**
     * Returns the server this user is connected to.
     */
    public final Server getServer() {
        return server;
    }
    
    public String getName() {
        return getNick();
    }
    public synchronized String getNick() {
        return nickName;
    }
    
    /**
     * Returns true if this user is local (0 hops).
     */
    public final boolean isLocal() {
        return (hopCount == 0);
    }
    
    public synchronized long getNickTimestamp() {
        return nickTimestamp;
    }
    
    public String getDisplayHostName() {
        return displayHostName;
    }
    public String getHostName() {
        return hostName;
    }
    public String getIdent() {
        return ident;
    }
    public String getDescription() {
        return description;
    }
    
    public synchronized void changeNick(String newnick) {
        server.changeUserNick(this, nickName, newnick);
        setNick(newnick);
    }
    
    /** Channel hook */
    void addChannel(Channel chan) {
        channels.add(chan);
    }
    /** Channel hook */
    void removeChannel(Channel chan) {
        channels.remove(chan);
    }
    
    public void send(Message msg) {
        StringBuffer buf = new StringBuffer();
        ConnectedEntity sender = msg.getSender();
        // append prefix
        if(sender != null) {
            buf.append(':').append(sender.getName());
            if(sender instanceof User) {
                User user = (User) sender;
                buf.append('!').append(user.getIdent()).append('@').append(user.getDisplayHostName());
            }
            buf.append(' ');
        }
        
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
    
    public void disconnect(String reason) {
        Message message = new Message(this, "QUIT").appendLastParameter(reason);
        // first remove the user from any channels he/she may be in
        for(Iterator iter = channels.iterator(); iter.hasNext();) {
            Channel channel = (Channel) iter.next();
            channel.sendLocal(message, this);
            channel.removeUser(this);
        }
        server.getNetwork().send(message, server);
        server.removeUser(this);
        if(isLocal() && handler != null) {
            handler.disconnect();
        }
    }
}
