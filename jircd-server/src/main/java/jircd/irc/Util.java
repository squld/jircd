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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.ProtectionDomain;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.LookupService;

/**
 * @author thaveman
 * @author markhale
 */
public final class Util {
    /** Common secure random number generator. */
    public static final SecureRandom RANDOM = new SecureRandom();
    
    private static final Map LOCALES;
    private static final LookupService GEOIP_SERVICE;
    private static final Map COUNTRY_LOCALES;
    
    static {
        LookupService service;
        try {
            service = new LookupService("GeoIP.dat");
        } catch(IOException ioe) {
            service = null;
            ioe.printStackTrace();
        }
        GEOIP_SERVICE = service;
        
        LRUHashMap locales = new LRUHashMap(39);
        // languages
        locales.put(Locale.ENGLISH.toString(), Locale.ENGLISH);
        locales.put(Locale.FRENCH.toString(), Locale.FRENCH);
        locales.put(Locale.GERMAN.toString(), Locale.GERMAN);
        locales.put(Locale.ITALIAN.toString(), Locale.ITALIAN);
        locales.put(Locale.CHINESE.toString(), Locale.CHINESE);
        locales.put(Locale.JAPANESE.toString(), Locale.JAPANESE);
        locales.put(Locale.KOREAN.toString(), Locale.KOREAN);
        // countries
        locales.put(Locale.UK.toString(), Locale.UK);
        locales.put(Locale.FRANCE.toString(), Locale.FRANCE);
        locales.put(Locale.GERMANY.toString(), Locale.GERMANY);
        locales.put(Locale.ITALY.toString(), Locale.ITALY);
        locales.put(Locale.US.toString(), Locale.US);
        locales.put(Locale.CANADA.toString(), Locale.CANADA);
        locales.put(Locale.CANADA_FRENCH.toString(), Locale.CANADA_FRENCH);
        locales.put(Locale.CHINA.toString(), Locale.CHINA);
        locales.put(Locale.JAPAN.toString(), Locale.JAPAN);
        locales.put(Locale.TAIWAN.toString(), Locale.TAIWAN);
        locales.put(Locale.KOREA.toString(), Locale.KOREA);
        LOCALES = Collections.synchronizedMap(locales);
        
        HashMap countryLocales = new HashMap(31);
        countryLocales.put("GB", Locale.UK);
        countryLocales.put("FR", Locale.FRANCE);
        countryLocales.put("DE", Locale.GERMANY);
        countryLocales.put("IT", Locale.ITALY);
        countryLocales.put("ES", getLocale("es", "ES"));
        countryLocales.put("US", Locale.US);
        countryLocales.put("CA", Locale.CANADA);
        countryLocales.put("MX", getLocale("es", "MX"));
        countryLocales.put("AR", getLocale("es", "AR"));
        countryLocales.put("BR", getLocale("pt", "BR"));
        countryLocales.put("CN", Locale.CHINA);
        countryLocales.put("JP", Locale.JAPAN);
        countryLocales.put("TW", Locale.TAIWAN);
        COUNTRY_LOCALES = Collections.unmodifiableMap(countryLocales);
    }
    
    static class LRUHashMap extends LinkedHashMap {
        private final int maxEntries;
        
        public LRUHashMap(int maxEntries) {
            super(4*(maxEntries+1)/3, 0.75f, true);
            this.maxEntries = maxEntries;
        }
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > maxEntries;
        }
    }
    
    private Util() {}
    
    public static void checkCommandPermission(Command cmd) {
        if(System.getSecurityManager() != null)
            AccessController.checkPermission(new CommandPermission(cmd.getName()));
    }
    public static void checkCTCPPermission(String dataType, String action) {
        if(System.getSecurityManager() != null)
            AccessController.checkPermission(new CTCPPermission(dataType, action));
    }
    public static void checkOperatorPermission(User user) {
        if(!user.isModeSet(User.UMODE_OPER))
            throw new AccessControlException("access denied (User +o)");
    }
    private static final ProtectionDomain[] EMPTY_PROTECTIONDOMAINS = new ProtectionDomain[0];
    private static void checkPermission(Subject subject, Permission permission, AccessControlContext acc) {
        if(acc == null)
            acc = new AccessControlContext(EMPTY_PROTECTIONDOMAINS);
        if(subject != null) {
            SubjectDomainCombiner sdc = new SubjectDomainCombiner(subject);
            acc = new AccessControlContext(acc, sdc);
        }
        acc.checkPermission(permission);
    }
    public static void checkCommandPermission(User user, Command cmd) {
        checkPermission(user.getSubject(), new CommandPermission(cmd.getName()), AccessController.getContext());
    }
    
    public static boolean isIRCString(String str) {
        final int len = str.length();
        for(int i=0; i < len; i++) {
            if (!isIRCCharacter(str.charAt(i))) return false;
        }
        return true;
    }
    private static boolean isIRCCharacter(char c) {
        return ((c >= 'A' && c <= '~') || isDigit(c) || c == '-');
    }
    private static boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }
    
    public static boolean isNickName(String name) {
        final int len = name.length();
        if(len > Constants.MAX_NICK_LENGTH) return false;
        for(int i=0; i < len; i++) {
            if (!isNickCharacter(name.charAt(i))) return false;
        }
        return true;
    }
    private static boolean isNickCharacter(char c) {
        return ((c >= 'A' && c <= '~') || isDigit(c) || c == '-');
    }
    
    public static boolean isChannelIdentifier(String name) {
        final int len = name.length();
        if(len > Constants.MAX_CHANNEL_LENGTH) return false;
        if(!isChannelIdentifierStart(name.charAt(0))) return false;
        for(int i=1; i < len; i++)
            if(!isChannelIdentifierPart(name.charAt(i))) return false;
        return true;
    }
    private static boolean isChannelIdentifierStart(char c) {
        return (c == '#' || c == '&' || c == '+' || c == '!');
    }
    private static boolean isChannelIdentifierPart(char c) {
        return (c != ' ' && c != ',' && c != '\r' && c != '\n');
    }
    
    public static boolean isUserModeIdentifier(char c) {
        return (c == '@') || (c == '+');
    }
    
    public static boolean isCommandIdentifier(String s) {
        s = s.toUpperCase();
        char c = s.charAt(0);
        if(isDigit(c)) {
            return (s.length() == 3) && isDigit(s.charAt(1)) && isDigit(s.charAt(2));
        } else {
            if(c < 'A' || c > 'Z') return false;
            for(int i=1; i<s.length(); i++) {
                c = s.charAt(i);
                if(c < 'A' || c > 'Z') return false;
            }
            return true;
        }
    }
    
    public static boolean isParameter(String s) {
        return (s.length() > 0) && (s.charAt(0) != ':') && (s.indexOf(' ') == -1);
    }
    
    public static void copyParameters(Message from, Message to) {
        final int count = from.getParameterCount();
        if(count == 0)
            return;
        final int lastIndex = count-1;
        for(int i=0; i<lastIndex; i++) {
            to.appendParameter(from.getParameter(i));
        }
        if(from.hasLastParameter())
            to.appendLastParameter(from.getParameter(lastIndex));
        else
            to.appendParameter(from.getParameter(lastIndex));
    }
    
    public static String[] split(String str, char separator) {
        int startPos = 0;
        int endPos = str.indexOf(separator);
        if(endPos == -1) {
            return new String[] {str};
        } else {
            String[] splitList = new String[10];
            int count = 0;
            while(endPos != -1) {
                if(count+1 == splitList.length) { // count+1 to leave room for the last substring
                    String[] old = splitList;
                    splitList = new String[2*old.length];
                    System.arraycopy(old, 0, splitList, 0, count);
                }
                splitList[count++] = str.substring(startPos, endPos);
                startPos = endPos+1;
                endPos = str.indexOf(separator, startPos);
            }
            splitList[count++] = str.substring(startPos);
            // trim array
            String[] old = splitList;
            splitList = new String[count];
            System.arraycopy(old, 0, splitList, 0, count);
            return splitList;
        }
    }
    public static String join(String[] strs, char separator, int offset) {
        StringBuffer buf = new StringBuffer();
        if(offset < strs.length)
            buf.append(strs[offset]);
        for(int i=offset+1; i<strs.length; i++) {
            buf.append(separator).append(strs[i]);
        }
        return buf.toString();
    }
    
    public static void sendPass(ConnectedEntity to, String password) {
        Message message = new Message("PASS");
        message.appendParameter(password);
        message.appendParameter(Constants.LINK_VERSION);
        message.appendParameter(Constants.LINK_FLAGS);
        to.send(message);
    }
    public static void sendServer(ConnectedEntity to) {
        Server server = to.getServer();
        Message message = new Message("SERVER");
        message.appendParameter(server.getName());
        message.appendParameter(Integer.toString(server.getHopCount()+1));
        message.appendParameter(Integer.toString(server.getToken()));
        message.appendLastParameter(server.getDescription());
        to.send(message);
    }
    
    public static void sendNetSync(final Server server) {
        Server thisServer = server.getServer();
        for(Iterator iter = thisServer.getUsers().iterator(); iter.hasNext(); ) {
            User user = (User) iter.next();
            sendUser(server, null, user);
        }
        for(Iterator iter = thisServer.getNetwork().getChannels().iterator(); iter.hasNext(); ) {
            Channel chan = (Channel) iter.next();
            Message message = new Message(thisServer, "NJOIN");
            message.appendParameter(chan.getName());
            message.appendParameter(getNameList(chan));
            server.send(message);
        }
    }
    public static void sendUser(Server toServer, Server fromServer, User user) {
        Message message = new Message(fromServer, "NICK");
        message.appendParameter(user.getNick());
        message.appendParameter(Integer.toString(user.getHopCount()+1));
        message.appendParameter(user.getIdent());
        message.appendParameter(user.getHostName());
        message.appendParameter(Integer.toString(user.getServer().getToken()));
        message.appendParameter(user.getModeList());
        message.appendLastParameter(user.getDescription());
        toServer.send(message);
    }
    private static String getNameList(Channel chan) {
        StringBuffer sb = new StringBuffer();
        for(Iterator iter = chan.getUsers().iterator(); iter.hasNext();) {
            User user = (User) iter.next();
            if (chan.isOp(user))
                sb.append(",@");
            else if (chan.isVoice(user))
                sb.append(",+");
            else
                sb.append(',');
            sb.append(user.getNick());
        }
        return (sb.length() > 0 ? sb.substring(1) : ""); // get rid of leading comma
    }
    
    public static String getResourceString(ConnectedEntity src, String key) {
        return ResourceBundle.getBundle("jircd.irc.Bundle", src.getLocale()).getString(key);
    }

    public static void sendError(ConnectedEntity src, String errMsg) {
        Message message = new Message("ERROR");
        message.appendLastParameter(errMsg);
        src.send(message);
    }
    public static void sendNoSuchNickError(ConnectedEntity src, String nick) {
        Message message = new Message(Constants.ERR_NOSUCHNICK, src);
        message.appendParameter(nick);
        message.appendLastParameter(getResourceString(src, "ERR_NOSUCHNICK"));
        src.send(message);
    }
    public static void sendNoSuchChannelError(ConnectedEntity src, String channel) {
        Message message = new Message(Constants.ERR_NOSUCHCHANNEL, src);
        message.appendParameter(channel);
        message.appendLastParameter(getResourceString(src, "ERR_NOSUCHCHANNEL"));
        src.send(message);
    }
    public static void sendNoSuchServerError(ConnectedEntity src, String server) {
        Message message = new Message(Constants.ERR_NOSUCHSERVER, src);
        message.appendParameter(server);
        message.appendLastParameter(getResourceString(src, "ERR_NOSUCHSERVER"));
        src.send(message);
    }
    public static void sendNotOnChannelError(ConnectedEntity src, String channel) {
        Message message = new Message(Constants.ERR_NOTONCHANNEL, src);
        message.appendParameter(channel);
        message.appendLastParameter(getResourceString(src, "ERR_NOTONCHANNEL"));
        src.send(message);
    }
    public static void sendCannotSendToChannelError(ConnectedEntity src, String channel) {
        Message message = new Message(Constants.ERR_CANNOTSENDTOCHAN, src);
        message.appendParameter(channel);
        message.appendLastParameter(getResourceString(src, "ERR_CANNOTSENDTOCHAN"));
        src.send(message);
    }
    public static void sendUserNotInChannelError(ConnectedEntity src, String nick, String channel) {
        Message message = new Message(Constants.ERR_USERNOTINCHANNEL, src);
        message.appendParameter(nick);
        message.appendParameter(channel);
        message.appendLastParameter(getResourceString(src, "ERR_USERNOTINCHANNEL"));
        src.send(message);
    }
    public static void sendChannelOpPrivilegesNeededError(ConnectedEntity src, String channel) {
        Message message = new Message(Constants.ERR_CHANOPRIVSNEEDED, src);
        message.appendParameter(channel);
        message.appendLastParameter(getResourceString(src, "ERR_CHANOPRIVSNEEDED"));
        src.send(message);
    }
    public static void sendUnknownCommandError(ConnectedEntity src, String cmdName) {
        Message message = new Message(Constants.ERR_UNKNOWNCOMMAND, src);
        message.appendParameter(cmdName);
        message.appendLastParameter(getResourceString(src, "ERR_UNKNOWNCOMMAND"));
        src.send(message);
    }
    public static void sendNeedMoreParamsError(ConnectedEntity src, String cmdName) {
        Message message = new Message(Constants.ERR_NEEDMOREPARAMS, src);
        message.appendParameter(cmdName);
        message.appendLastParameter(getResourceString(src, "ERR_NEEDMOREPARAMS"));
        src.send(message);
    }
    public static void sendNoPrivilegesError(ConnectedEntity src) {
        Message message = new Message(Constants.ERR_NOPRIVILEGES, src);
        message.appendLastParameter(getResourceString(src, "ERR_NOPRIVILEGES"));
        src.send(message);
    }
    public static void sendUserModeUnknownFlagError(ConnectedEntity src) {
        Message message = new Message(Constants.ERR_UMODEUNKNOWNFLAG, src);
        message.appendLastParameter(getResourceString(src, "ERR_UMODEUNKNOWNFLAG"));
        src.send(message);
    }
    public static void sendAlreadyRegisteredError(ConnectedEntity src) {
        Message message = new Message(Constants.ERR_ALREADYREGISTRED, src);
        message.appendLastParameter(Util.getResourceString(src, "ERR_ALREADYREGISTRED"));
        src.send(message);
    }
    public static void sendNotRegisteredError(UnregisteredEntity src) {
        Message message = new Message(Constants.ERR_NOTREGISTERED, src);
        message.appendLastParameter(getResourceString(src, "ERR_NOTREGISTERED"));
        src.send(message);
    }
    
    public static boolean match(String pattern, String text) {
        return matchWildcard(pattern, text);
    }
    private static boolean matchWildcard(String pattern, String text) {
        if(text == null)
            return false;
        int patSize = pattern.length() - 1;
        int texSize = text.length() - 1;
        int patIndex = 0;
        int texIndex = 0;
        
        while (true) {
            if (patIndex > patSize) return (texIndex > texSize);
            
            if (pattern.charAt(patIndex) == '*') {
                patIndex++;
                
                if (patIndex > patSize) return true;
                
                while (pattern.charAt(patIndex) == '*')
                    patIndex++;
                
                while (patIndex <= patSize && pattern.charAt(patIndex) == '?' && texIndex <= texSize) {
                    texIndex++;
                    patIndex++;
                }
                
                if (patIndex > patSize) return false;
                
                if (pattern.charAt(patIndex) == '*') continue;
                
                while (texIndex <= texSize) {
                    if (matchWildcard(pattern.substring(patIndex),text.substring(texIndex)))
                        return true;
                    else if (texIndex == texSize)
                        return false;
                    texIndex++;
                }
            }//end if
            if (texIndex > texSize) return true;
            if (patIndex <= patSize && pattern.charAt(patIndex) != '?' &&
                    Character.toUpperCase(pattern.charAt(patIndex)) != Character.toUpperCase(text.charAt(texIndex)))
                return false;
            texIndex++;
            patIndex++;
        }
    }
    public static boolean match(String pattern, String mask, String addr) {
        byte[] pat = parseIPv4Address(pattern);
        byte[] msk = parseIPv4Address(mask);
        byte[] ip = parseIPv4Address(addr);
        if(pat.length != ip.length || msk.length != ip.length)
            return false;
        for(int i=0; i<ip.length; i++) {
            if((ip[i] & msk[i]) != (pat[i] & msk[i]))
                return false;
        }
        return true;
    }
    private static byte[] parseIPv4Address(String addr) {
        byte[] ip = new byte[4];
        int pos = 0;
        int octet = 0;
        for(int i=0; i<addr.length(); i++) {
            char ch = addr.charAt(i);
            if(ch >= '0' && ch <= '9') {
                octet = octet*10 + (ch - '0');
            } else if(ch == '.') {
                ip[pos++] = (byte) octet;
                octet = 0;
            } else {
                throw new IllegalArgumentException("Not an IPv4 address: "+addr);
            }
        }
        ip[pos++] = (byte) octet;
        return ip;
    }
    
    public static String[] loadTextFile(String filename, int maxLines) throws IOException {
        String[] tmpLines = new String[maxLines];
        BufferedReader file = new BufferedReader(new FileReader(filename));
        int n;
        try {
            String line = file.readLine();
            for (n=0; line != null && n < tmpLines.length; n++) {
                tmpLines[n] = line;
                line = file.readLine();
            }
        } finally {
            file.close();
        }
        
        String[] lines = new String[n];
        System.arraycopy(tmpLines, 0, lines, 0, n);
        return lines;
    }
    
    public static Locale getLocale(String language) {
        Locale locale = (Locale) LOCALES.get(language);
        if(locale == null) {
            locale = new Locale(language);
            LOCALES.put(locale.toString(), locale);
        }
        return locale;
    }
    public static Locale getLocale(String language, String country) {
        Locale locale = (Locale) LOCALES.get(language+'_'+country);
        if(locale == null) {
            locale = new Locale(language, country);
            LOCALES.put(locale.toString(), locale);
        }
        return locale;
    }
    public static Locale getLocale(String language, String country, String variant) {
        Locale locale = (Locale) LOCALES.get(language+'_'+country+'_'+variant);
        if(locale == null) {
            locale = new Locale(language, country, variant);
            LOCALES.put(locale.toString(), locale);
        }
        return locale;
    }
    public static Locale lookupLocale(String ip) {
        if(GEOIP_SERVICE != null) {
            Country country = GEOIP_SERVICE.getCountry(ip);
            Locale locale = (Locale) COUNTRY_LOCALES.get(country.getCode());
            if(locale != null)
                return locale;
            else
                return Locale.getDefault();
        } else {
            return Locale.getDefault();
        }
    }
}
