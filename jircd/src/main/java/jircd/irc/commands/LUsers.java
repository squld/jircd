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

import jircd.irc.Command;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.Network;
import jircd.irc.RegisteredEntity;
import jircd.irc.Server;
import jircd.irc.User;

/**
 * @author markhale
 */
public class LUsers implements Command {
	private int maxLocal = 0;
	private int maxGlobal = 0;

	public void invoke(RegisteredEntity src, String[] params) {
		if (params == null || params.length == 0) {
			Server server = src.getServer();
			Network network = server.getNetwork();

			int serverVisCount = server.getUserCount(User.UMODE_INVISIBLE, false);
			int serverInvisCount = server.getUserCount(User.UMODE_INVISIBLE, true);
			int curLocal = serverVisCount + serverInvisCount;
			if (curLocal > maxLocal) maxLocal = curLocal;

			int netVisCount = network.getUserCount(User.UMODE_INVISIBLE, false);
			int netInvisCount = network.getUserCount(User.UMODE_INVISIBLE, true);
			int curGlobal = netVisCount + netInvisCount;
			if (curGlobal > maxGlobal) maxGlobal = curGlobal;

			Message message = new Message(Constants.RPL_LUSERCLIENT, src);
			message.appendLastParameter("There are " + netVisCount + " users and " + netInvisCount + " invisible on " + network.getServers().size() + " servers");
			src.send(message);

			message = new Message(Constants.RPL_LUSEROP, src);
			message.appendParameter(Integer.toString(network.getUserCount(User.UMODE_OPER, true)));
			message.appendLastParameter("operator(s) online");
			src.send(message);

			message = new Message(Constants.RPL_LUSERCHANNELS, src);
			message.appendParameter(Integer.toString(network.getChannels().size()));
			message.appendLastParameter("channels formed");
			src.send(message);

			message = new Message(Constants.RPL_LUSERME, src);
			message.appendLastParameter("I have " + serverVisCount + " clients and " + 0 + " servers.");
			src.send(message);

			message = new Message("265", src);
			message.appendLastParameter("Current local users: " + curLocal + " Max: " + maxLocal);
			src.send(message);

			message = new Message("266", src);
			message.appendLastParameter("Current global users: " + curGlobal + " Max: " + maxGlobal);
			src.send(message);
		} else {
			// find correct server and ask
		}
	}
	public String getName() {
		return "LUSERS";
	}
	public int getMinimumParameterCount() {
		return 0;
	}
}
