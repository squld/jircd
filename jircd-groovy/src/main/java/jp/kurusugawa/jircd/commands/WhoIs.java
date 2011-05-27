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

package jp.kurusugawa.jircd.commands;

import java.util.Set;

import jircd.irc.Channel;
import jircd.irc.Command;
import jircd.irc.Connection;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * @author markhale
 * @author squld@kurusugawa.jp
 */
public class WhoIs implements Command {
	public void invoke(RegisteredEntity tSource, String[] params) {
		for (String tNick : params) {
			try {
				User tWho = tSource.getServer().getNetwork().getUser(tNick);
				if (tWho == null) {
					Util.sendNoSuchNickError(tSource, tNick);
					continue;
				}

				sendWhoIsUser(tSource, tWho);
				sendWhoIsChannels(tSource, tWho);
				sendWhoIsServer(tSource, tWho);

				if (tWho.isModeSet(User.UMODE_OPER)) {
					sendWhoIsOperator(tSource, tWho);
				}

				if (tWho.isLocal()) {
					sendWhoIsIdle(tSource, tWho);
				}
			} finally {
				sendEndOfWhoIs(tSource, tNick);
			}
		}
	}

	private void sendEndOfWhoIs(RegisteredEntity aSource, String aNick) {
		Message tMessage = new Message(Constants.RPL_ENDOFWHOIS, aSource);
		tMessage.appendParameter(aNick);
		tMessage.appendLastParameter(Util.getResourceString(aSource, "RPL_ENDOFWHOIS"));
		aSource.send(tMessage);
	}

	private void sendWhoIsIdle(RegisteredEntity aSource, User aWho) {
		Message tMessage = new Message(Constants.RPL_WHOISIDLE, aSource);
		tMessage.appendParameter(aWho.getNick());
		Connection.Handler tHandler = aWho.getHandler();
		tMessage.appendParameter(Long.toString(tHandler.getIdleTimeMillis() / Constants.SECS_TO_MILLIS));
		tMessage.appendParameter(Long.toString(tHandler.getConnection().getConnectTimeMillis() / Constants.SECS_TO_MILLIS));
		tMessage.appendLastParameter(Util.getResourceString(aSource, "RPL_WHOISIDLE"));
		aSource.send(tMessage);
	}

	private void sendWhoIsOperator(RegisteredEntity aSource, User aWho) {
		Message tMessage = new Message(Constants.RPL_WHOISOPERATOR, aSource);
		tMessage.appendParameter(aWho.getNick());
		tMessage.appendLastParameter(Util.getResourceString(aSource, "RPL_WHOISOPERATOR"));
		aSource.send(tMessage);
	}

	private void sendWhoIsServer(RegisteredEntity aSource, User aWho) {
		Message tMessage = new Message(Constants.RPL_WHOISSERVER, aSource);
		tMessage.appendParameter(aWho.getNick());
		tMessage.appendParameter(aWho.getServer().getName());
		tMessage.appendLastParameter(aWho.getServer().getDescription());
		aSource.send(tMessage);
	}

	@SuppressWarnings("unchecked")
	private void sendWhoIsChannels(RegisteredEntity aSource, User aWho) {
		StringBuilder tChannelList = new StringBuilder();
		for (Channel tChannel : (Set<Channel>) aWho.getChannels()) {
			if (tChannel.isModeSet(Channel.CHANMODE_SECRET) || tChannel.isOn((User) aSource)) {
				tChannelList.append(' ');
				if (tChannel.isOp(aWho)) {
					tChannelList.append("@");
				}
				if (tChannel.isVoice(aWho)) {
					tChannelList.append("+");
				}
				tChannelList.append(tChannel.getName());
			}
		}

		if (tChannelList.length() > 0) {
			Message tMessage = new Message(Constants.RPL_WHOISCHANNELS, aSource);
			tMessage.appendParameter(aWho.getNick());
			// get rid of leading space ' '
			tMessage.appendLastParameter(tChannelList.substring(1));
			aSource.send(tMessage);
		}
	}

	private void sendWhoIsUser(RegisteredEntity aSource, User aWho) {
		Message tMessage = new Message(Constants.RPL_WHOISUSER, aSource);
		tMessage.appendParameter(aWho.getNick());
		tMessage.appendParameter(aWho.getIdent());

		String tHostname;
		if (aWho.equals(aSource) || ((User) aSource).isModeSet(User.UMODE_OPER)) {
			tHostname = aWho.getHostName();
		} else {
			tHostname = aWho.getDisplayHostName();
		}
		tMessage.appendParameter(tHostname);

		tMessage.appendParameter("*");
		tMessage.appendLastParameter(aWho.getDescription());
		aSource.send(tMessage);
	}

	public String getName() {
		return "WHOIS";
	}

	public int getMinimumParameterCount() {
		return 1;
	}
}
