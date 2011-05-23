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

import java.io.IOException;

import jircd.jIRCdMBean;
import jircd.irc.Command;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.Util;

/**
 * @author markhale
 */
public class Motd implements Command {
    private String[] motd;
    
    public Motd(jIRCdMBean jircd) {
        try {
            motd = Util.loadTextFile(jircd.getProperty("jircd.motd", "motd.txt"), 500);
        } catch(IOException ioe) {
            motd = new String[0];
        }
    }
    public void invoke(RegisteredEntity src, String[] params) {
        Message msg = new Message(Constants.RPL_MOTDSTART, src);
        msg.appendLastParameter("- " + src.getServer().getName() + " Message of the Day -");
        src.send(msg);
        for (int i = 0; i < motd.length; i++) {
            msg = new Message(Constants.RPL_MOTD, src);
            msg.appendLastParameter("- " + motd[i]);
            src.send(msg);
        }
        msg = new Message(Constants.RPL_ENDOFMOTD, src);
        msg.appendLastParameter("End of /MOTD command.");
        src.send(msg);
    }
    public String getName() {
        return "MOTD";
    }
    public int getMinimumParameterCount() {
        return 0;
    }
}
