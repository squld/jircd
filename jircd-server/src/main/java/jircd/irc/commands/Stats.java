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

package jircd.irc.commands;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Set;

import javax.naming.Binding;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import jircd.jIRCdMBean;
import jircd.irc.Command;
import jircd.irc.CommandContext;
import jircd.irc.ConnectedEntity;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class Stats implements Command {
    private static final DecimalFormat TWO_PLACES = new DecimalFormat("00");
    private final jIRCdMBean jircd;
    
    public Stats(jIRCdMBean jircd) {
        this.jircd = jircd;
    }
    public void invoke(RegisteredEntity src, String[] params) {
        String query = params[0];
        switch(query.charAt(0)) {
            case 'm':
                sendCommandUsage(src);
                break;
            case 'o':
                try {
                    Util.checkOperatorPermission((User)src);
                    sendOperators(src);
                } catch(SecurityException se) {
                    Util.sendNoPrivilegesError(src);
                } catch(NamingException ne) {
                    ne.printStackTrace();
                }
                break;
            case 'u':
                sendUptime(src);
                break;
        }
        Message message = new Message(Constants.RPL_ENDOFSTATS, src);
        message.appendParameter(query);
        message.appendLastParameter(Util.getResourceString(src, "RPL_ENDOFSTATS"));
        src.send(message);
    }
    private void sendCommandUsage(ConnectedEntity src) {
        Set ctxs = jircd.getCommandContexts();
        for(Iterator iter = ctxs.iterator(); iter.hasNext(); ) {
            CommandContext ctx = (CommandContext) iter.next();
            Message message = new Message(Constants.RPL_STATSCOMMANDS, src);
            message.appendParameter(ctx.getCommand().getName());
            message.appendParameter(Integer.toString(ctx.getUsedCount()));
            src.send(message);
        }
    }
    private void sendOperators(ConnectedEntity src) throws NamingException {
        InitialContext ctx = new InitialContext();
        try {
            NamingEnumeration iter = ctx.listBindings("");
            while(iter.hasMoreElements()) {
                Binding binding = (Binding) iter.nextElement();
                String name = binding.getName();
                String[] userInfo = Util.split((String) binding.getObject(), ' ');
                Message message = new Message(Constants.RPL_STATSOLINE, src);
                message.appendParameter("O");
                message.appendParameter(userInfo[1]);
                message.appendParameter("*");
                message.appendParameter(name);
                src.send(message);
            }
        } finally {
            ctx.close();
        }
    }
    private void sendUptime(ConnectedEntity src) {
        int uptimeSecs = (int) (jircd.getUptimeMillis()/Constants.SECS_TO_MILLIS);
        int days = uptimeSecs/(24*60*60);
        int hours = uptimeSecs/(60*60) - 24*days;
        int mins = uptimeSecs/60 - 60*(hours + 24*days);
        int secs = uptimeSecs - 60*(mins + 60*(hours + 24*days));
        Message message = new Message(Constants.RPL_STATSUPTIME, src);
        message.appendLastParameter("Server Up "+days+" days "+hours+':'+TWO_PLACES.format(mins)+':'+TWO_PLACES.format(secs));
        src.send(message);
    }
    public String getName() {
        return "STATS";
    }
    public int getMinimumParameterCount() {
        return 1;
    }
}
