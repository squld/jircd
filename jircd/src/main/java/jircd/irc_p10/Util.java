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

package jircd.irc_p10;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jircd.jIRCdMBean;
import jircd.irc.Channel;
import jircd.irc.ConnectedEntity;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.Network;
import jircd.irc.Server;
import jircd.irc.User;

/**
 * @author markhale
 */
public final class Util {
    public static final int SERVER_TOKEN_MASK = 4095;
    public static final int USER_TOKEN_MASK = 262143;
    public static final String LINK_VERSION = "J10";
    
    private static final char[] encodeLookup = new char[] {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '[', ']'
    };
    private static final byte[] decodeLookup = new byte[128];
    private static final Map commands = new HashMap(23);
    static {
        for(int i=0; i<decodeLookup.length; i++) {
            decodeLookup[i] = -1;
        }
        decodeLookup['A'] = 0; decodeLookup['B'] = 1; decodeLookup['C'] = 2; decodeLookup['D'] = 3;
        decodeLookup['E'] = 4; decodeLookup['F'] = 5; decodeLookup['G'] = 6; decodeLookup['H'] = 7;
        decodeLookup['I'] = 8; decodeLookup['J'] = 9; decodeLookup['K'] = 10; decodeLookup['L'] = 11;
        decodeLookup['M'] = 12; decodeLookup['N'] = 13; decodeLookup['O'] = 14; decodeLookup['P'] = 15;
        decodeLookup['Q'] = 16; decodeLookup['R'] = 17; decodeLookup['S'] = 18; decodeLookup['T'] = 19;
        decodeLookup['U'] = 20; decodeLookup['V'] = 21; decodeLookup['W'] = 22; decodeLookup['X'] = 23;
        decodeLookup['Y'] = 24; decodeLookup['Z'] = 25; decodeLookup['a'] = 26; decodeLookup['b'] = 27;
        decodeLookup['c'] = 28; decodeLookup['d'] = 29; decodeLookup['e'] = 30; decodeLookup['f'] = 31;
        decodeLookup['g'] = 32; decodeLookup['h'] = 33; decodeLookup['i'] = 34; decodeLookup['j'] = 35;
        decodeLookup['k'] = 36; decodeLookup['l'] = 37; decodeLookup['m'] = 38; decodeLookup['n'] = 39;
        decodeLookup['o'] = 40; decodeLookup['p'] = 41; decodeLookup['q'] = 42; decodeLookup['r'] = 43;
        decodeLookup['s'] = 44; decodeLookup['t'] = 45; decodeLookup['u'] = 46; decodeLookup['v'] = 47;
        decodeLookup['w'] = 48; decodeLookup['x'] = 49; decodeLookup['y'] = 50; decodeLookup['z'] = 51;
        decodeLookup['0'] = 52; decodeLookup['1'] = 53; decodeLookup['2'] = 54; decodeLookup['3'] = 55;
        decodeLookup['4'] = 56; decodeLookup['5'] = 57; decodeLookup['6'] = 58; decodeLookup['7'] = 59;
        decodeLookup['8'] = 60; decodeLookup['9'] = 61; decodeLookup['['] = 62; decodeLookup[']'] = 63;
        
        commands.put("AWAY", "A");
        commands.put("JOIN", "J");
        commands.put("INVITE", "I");
        commands.put("PART", "L");
        commands.put("MODE", "M");
        commands.put("NICK", "N");
        commands.put("NOTICE", "O");
        commands.put("TOPIC", "T");
        commands.put("QUIT", "Q");
        commands.put("PING", "G");
        commands.put("PONG", "Z");
        commands.put("PRIVMSG", "P");
        commands.put("WALLOPS", "WA");
    }
    
    private Util() {}
    
    public static int randomServerToken() {
        return (jircd.irc.Util.RANDOM.nextInt() & SERVER_TOKEN_MASK);
    }
    public static int randomUserToken() {
        return (jircd.irc.Util.RANDOM.nextInt() & USER_TOKEN_MASK);
    }
    public static int parseBase64(String s) {
        if(s.length() > 6)
            throw new IllegalArgumentException("String too long: "+s);
        int value = 0;
        int factor = 1;
        for(int i=s.length()-1; i>=0; i--) {
            final int b64 = decodeLookup[s.charAt(i)];
            if(b64 == -1)
                throw new IllegalArgumentException("Not a base 64 string: "+s);
            value += b64*factor;
            factor *= 64;
        }
        return value;
    }
    public static String toBase64(int x) {
        char[] b64 = new char[6];
        b64[5] = encodeLookup[x & 63];
        x >>>= 6;
        b64[4] = encodeLookup[x & 63];
        x >>>= 6;
        b64[3] = encodeLookup[x & 63];
        x >>>= 6;
        b64[2] = encodeLookup[x & 63];
        x >>>= 6;
        b64[1] = encodeLookup[x & 63];
        x >>>= 6;
        b64[0] = encodeLookup[x & 63];
        return new String(b64);
    }
    public static String toBase64Token(Server server) {
        int token = server.getToken();
        char[] b64 = new char[2];
        b64[1] = encodeLookup[token & 63];
        token >>>= 6;
        b64[0] = encodeLookup[token & 63];
        return new String(b64);
    }
    public static String toBase64Token(User_P10 user) {
        int stoken = user.getServer().getToken();
        int utoken = user.getToken();
        char[] b64 = new char[5];
        b64[4] = encodeLookup[utoken & 63];
        utoken >>>= 6;
        b64[3] = encodeLookup[utoken & 63];
        utoken >>>= 6;
        b64[2] = encodeLookup[utoken & 63];
        b64[1] = encodeLookup[stoken & 63];
        stoken >>>= 6;
        b64[0] = encodeLookup[stoken & 63];
        return new String(b64);
    }
    
    /** Supports short tokens */
    public static ConnectedEntity findEntity(Network network, String tokenB64) {
        final int len = tokenB64.length();
        if(len == 2 || len == 1)
            return findServer(network, tokenB64);
        else if(len == 5 || len == 3)
            return findUser(network, tokenB64);
        else
            throw new IllegalArgumentException("Invalid token length: "+tokenB64);
    }
    /** Supports short tokens */
    public static Server_P10 findServer(Network network, String tokenB64) {
        return (Server_P10) network.getServer(parseBase64(tokenB64));
    }
    /** Supports short tokens */
    public static User_P10 findUser(Network network, String tokenB64) {
        String stokenB64, utokenB64;
        if(tokenB64.length() == 5) {
            stokenB64 = tokenB64.substring(0, 2);
            utokenB64 = tokenB64.substring(2);
        } else if(tokenB64.length() == 3) {
            stokenB64 = tokenB64.substring(0, 1);
            utokenB64 = tokenB64.substring(1);
        } else {
            throw new IllegalArgumentException("Invalid token length: "+tokenB64);
        }
        Server_P10 server = findServer(network, stokenB64);
        return (User_P10) server.getUser(parseBase64(utokenB64));
    }
    
    public static int toIPAddress(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            byte addrBytes[] = address.getAddress();
            return ((addrBytes[3]&0xFF)<<24) | ((addrBytes[2]&0xFF)<<16) | ((addrBytes[1]&0xFF)<<8) | (addrBytes[0]&0xFF);
        } catch(UnknownHostException uhe) {
            return 0;
        }
    }
    
    public static void sendPass(ConnectedEntity to, String password) {
        Message message = new Message("PASS");
        message.appendLastParameter(password);
        to.send(message);
    }
    public static void sendServer(ConnectedEntity to, jIRCdMBean jircd) {
        String flags = "0";
        Server server = to.getServer();
        Message message = new Message("SERVER");
        message.appendParameter(server.getName());
        message.appendParameter(Integer.toString(server.getHopCount()+1));
        message.appendParameter(Long.toString(jircd.getStartTimeMillis()/Constants.SECS_TO_MILLIS));
        message.appendParameter(Long.toString(System.currentTimeMillis()/Constants.SECS_TO_MILLIS));
        message.appendParameter(Util.LINK_VERSION);
        message.appendParameter(Util.toBase64Token(server)+"]]]");
        message.appendParameter(flags);
        message.appendLastParameter(server.getDescription());
        to.send(message);
    }
    
    public static void sendNetSync(final Server_P10 server) {
        Server_P10 thisServer = (Server_P10) server.getServer();
        for(Iterator iter = thisServer.getUsers().iterator(); iter.hasNext(); ) {
            User_P10 user = (User_P10) iter.next();
            sendUser(server, thisServer, user);
        }
        for(Iterator iter = thisServer.getNetwork().getChannels().iterator(); iter.hasNext(); ) {
            Channel chan = (Channel) iter.next();
            Message message = new Message(thisServer, "B");
            message.appendParameter(chan.getName());
            message.appendParameter(Long.toString(chan.getCreationTimeMillis()/Constants.SECS_TO_MILLIS));
            message.appendParameter(getNameList(chan));
            server.send(message);
        }
        Message message = new Message(thisServer, "EB");
        server.send(message);
    }
    public static void sendUser(Server_P10 toServer, Server_P10 fromServer, User_P10 user) {
        Message message = new Message(fromServer, "N");
        message.appendParameter(user.getNick());
        message.appendParameter(Integer.toString(user.getHopCount()+1));
        message.appendParameter(Long.toString(user.getNickTimestamp()/Constants.SECS_TO_MILLIS));
        message.appendParameter(user.getIdent());
        message.appendParameter(user.getDisplayHostName());
        message.appendParameter(user.getModeList());
        message.appendParameter(Util.toBase64(toIPAddress(user.getHostName())));
        message.appendParameter(Util.toBase64Token(user));
        message.appendLastParameter(user.getDescription());
        toServer.send(message);
    }
    private static String getNameList(Channel chan) {
        StringBuffer sb = new StringBuffer();
        for(Iterator iter = chan.getUsers().iterator(); iter.hasNext();) {
            User user = (User) iter.next();
            sb.append(',').append(user.toString());
            if (chan.isOp(user))
                sb.append(":o");
            else if (chan.isVoice(user))
                sb.append(":v");
        }
        return (sb.length() > 0 ? sb.substring(1) : ""); // get rid of leading comma
    }
    
    /**
     * Transcodes a message to a P10 message.
     */
    public static Message transcode(Network network, Message msg) {
        String cmd = (String) commands.get(msg.getCommand());
        if(cmd == null) {
            return msg;
        }
        Message msgP10 = new Message(msg.getSender(), cmd);
        if("P".equals(cmd)) {
            String dest = msg.getParameter(0);
            if(!jircd.irc.Util.isChannelIdentifier(dest)) {
                // if a nick transcode nick to token
                User_P10 user = (User_P10) network.getUser(dest);
                dest = toBase64Token(user);
            }
            msgP10.appendParameter(dest);
            msgP10.appendLastParameter(msg.getParameter(1));
        } else {
            jircd.irc.Util.copyParameters(msg, msgP10);
        }
        return msgP10;
    }
}
