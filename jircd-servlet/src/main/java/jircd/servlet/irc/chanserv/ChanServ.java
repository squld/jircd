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

package jircd.servlet.irc.chanserv;

import java.io.IOException;
import java.sql.*;
import javax.servlet.ServletException;
import jircd.jIRCdMBean;
import jircd.irc.Channel;
import jircd.irc.Network;
import jircd.irc.User;
import jircd.irc.Util;
import jircd.servlet.irc.*;

/**
 * @author markhale
 */
public class ChanServ extends IrcServlet {
    private static final String REGISTER = "register";
    private static final String SET = "set";
    private static final String DROP = "drop";
    
    public void init() throws ServletException {
        jIRCdMBean jircd = (jIRCdMBean) getServletContext().getAttribute("jircd");
        try {
            Class.forName(jircd.getProperty("chanserv.jdbc.driver"));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        Network network = (Network) getServletContext().getAttribute("jircd.irc.network");
        try {
            Connection conn = DriverManager.getConnection(jircd.getProperty("chanserv.jdbc.url"));
            try {
                PreparedStatement stmt = conn.prepareStatement(jircd.getProperty("chanserv.sql.queryChannels"));
                try {
                    ResultSet rs = stmt.executeQuery();
                    while(rs.next()) {
                        Channel channel = new RegisterableChannel(rs, network, jircd);
                    }
                    rs.close();
                } finally {
                    stmt.close();
                }
            } finally {
                conn.close();
            }
        } catch(SQLException sqle) {
            throw new ServletException(sqle);
        }
    }
    protected void doPrivMsg(IrcServletRequest request, IrcServletResponse response) throws IOException {
        final String[] params = Util.split(request.getText(), ' ');
        final String cmd = params[0];
        response.setCommand(NOTICE);
        if(cmd.startsWith(REGISTER)) {
            if(params.length > 1) {
                Network network = (Network) getServletContext().getAttribute("jircd.irc.network");
                Channel chan = network.getChannel(params[1]);
                if(chan instanceof RegisterableChannel) {
                    RegisterableChannel regchan = (RegisterableChannel) chan;
                    User user = network.getUser(request.getNick());
                    if(!regchan.isRegistered() && chan.isOp(user)) {
                        regchan.setRegistered(true);
                        response.send("Channel registered.");
                    } else {
                        response.send("Channel is already registered.");
                    }
                } else {
                    response.send("Channel does not exist or is not registerable.");
                }
            } else {
                response.send("Not enough parameters specified.");
            }
        } else if(cmd.startsWith(DROP)) {
            if(params.length > 1) {
                Network network = (Network) getServletContext().getAttribute("jircd.irc.network");
                Channel chan = network.getChannel(params[1]);
                if(chan instanceof RegisterableChannel) {
                    RegisterableChannel regchan = (RegisterableChannel) chan;
                    User user = network.getUser(request.getNick());
                    if(regchan.isRegistered() && chan.isOp(user)) {
                        regchan.setRegistered(false);
                        response.send("Channel dropped.");
                    } else {
                        response.send("Channel is not registered.");
                    }
                } else {
                    response.send("Channel does not exist or is not registerable.");
                }
            } else {
                response.send("Not enough parameters specified.");
            }
        } else if(cmd.startsWith(SET)) {
            if(params.length > 2) {
                Network network = (Network) getServletContext().getAttribute("jircd.irc.network");
                Channel chan = network.getChannel(params[1]);
                if(chan instanceof RegisterableChannel) {
                    RegisterableChannel regchan = (RegisterableChannel) chan;
                    if(regchan.isRegistered()) {
                        String option = params[2];
                        if("entrymsg".equals(option)) {
                            if(params.length > 3) {
                                regchan.setEntryMessage(Util.join(params, ' ', 3));
                                response.send("Entry message set.");
                            } else {
                                regchan.setEntryMessage(null);
                                response.send("Entry message removed.");
                            }
                        } else {
                            response.send("Unrecognised option.");
                        }
                    } else {
                        response.send("Channel is not registered.");
                    }
                } else {
                    response.send("Channel does not exist or is not registerable.");
                }
            } else {
                response.send("Not enough parameters specified.");
            }
        } else {
            response.send("Unrecognised command.");
        }
    }
    public void destroy() {
        jIRCdMBean jircd = (jIRCdMBean) getServletContext().getAttribute("jircd");
        String shutdownURL = jircd.getProperty("chanserv.jdbc.url.shutdown");
        if(shutdownURL != null) {
            try {
                Connection conn = DriverManager.getConnection(shutdownURL);
                try {
                    String shutdownSQL = jircd.getProperty("chanserv.sql.shutdown");
                    if(shutdownSQL != null) {
                        Statement stmt = conn.createStatement();
                        try {
                            stmt.executeUpdate(shutdownSQL);
                        } finally {
                            stmt.close();
                        }
                    }
                } finally {
                    conn.close();
                }
            } catch(SQLException sqle) {
                // ignore
            }
        }
    }
}
