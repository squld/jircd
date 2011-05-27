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

import jircd.irc.Channel;
import jircd.irc.Command;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.Network;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * Security checks are performed on CTCP messages.
 * 
 * @author markhale
 */
public class PrivMsg implements Command {
	public void invoke(RegisteredEntity tSource, String[] aParameters) {
		if (tSource instanceof User && ((User) tSource).isLocal()) {
			tSource.getHandler().active();
		}

		String tDestination = aParameters[0];
		if (tDestination.charAt(0) == '#') {
			sendToChannel(tSource, tDestination, aParameters);
		} else if (tDestination.charAt(0) == '&') {
			// message to local channel
			Util.sendNoSuchChannelError(tSource, tDestination);
		} else {
			sendToUser(tSource, tDestination, aParameters);
		}
	}

	private void sendToUser(RegisteredEntity aSource, String aNick, String[] aParameters) {
		// message to user
		User tUser = findUser(aSource.getServer().getNetwork(), aNick);
		// check user exists
		if (tUser == null) {
			Util.sendNoSuchNickError(aSource, aNick);
		}

		String tAwayMessage = tUser.getAwayMessage();

		// check user is not away
		if (tAwayMessage != null) {
			// send away message
			Message tMessage = new Message(tUser, Constants.RPL_AWAY, aSource);
			tMessage.appendLastParameter(tAwayMessage);
			aSource.send(tMessage);
			return;
		}

		// send message
		String tText = aParameters[1];
		int tCTCPStartPosition = tText.indexOf(Constants.CTCP_DELIMITER);
		while (tCTCPStartPosition > 0) {
			final int tCTCPEndPosition = tText.indexOf(Constants.CTCP_DELIMITER, tCTCPStartPosition + 1);
			if (tCTCPStartPosition > tCTCPEndPosition) {
				break;
			}
			String tCTCP = tText.substring(tCTCPStartPosition + 1, tCTCPEndPosition);
			int tSpacePosition = tCTCP.indexOf(' ');
			int tEndPosition = (tSpacePosition != -1) ? tSpacePosition : tCTCP.length();
			String tDataType = tCTCP.substring(0, tEndPosition);
			String tAction = "";
			if (tDataType.equals("DCC") && tSpacePosition != -1) {
				// DCC CHAT message
				int tStartPosition = tSpacePosition + 1;
				tSpacePosition = tCTCP.indexOf(' ', tStartPosition);
				tEndPosition = (tSpacePosition != -1) ? tSpacePosition : tCTCP.length();
				tAction = tCTCP.substring(tStartPosition, tEndPosition);
			}
			Util.checkCTCPPermission(tDataType, tAction);
			tCTCPStartPosition = tText.indexOf(Constants.CTCP_DELIMITER, tCTCPEndPosition + 1);
		}
		Message tMessage = new Message(aSource, "PRIVMSG", tUser);
		tMessage.appendLastParameter(tText);
		tUser.send(tMessage);
	}

	private void sendToChannel(RegisteredEntity aSender, String aChannel, String[] aParameters) {
		// message to channel
		Channel tChannel = aSender.getServer().getNetwork().getChannel(aChannel);

		// TODO: check for +m vs voice, +n vs not-in-chan, etc.
		if (tChannel == null) {
			Util.sendNoSuchChannelError(aSender, aChannel);
			return;
		}

		User tUser = (User) aSender;
		String tText = aParameters[1];

		final boolean tIsModerated = tChannel.isModeSet(Channel.CHANMODE_MODERATED);
		final boolean tHasPrivileges = tChannel.isVoice(tUser) || tChannel.isOp(tUser);
		if (!tIsModerated || (tIsModerated && tHasPrivileges)) {
			Message tMessage = new Message(tUser, "PRIVMSG", tChannel);
			tMessage.appendLastParameter(tText);
			tChannel.send(tMessage, tUser);
		} else if (tIsModerated && !tHasPrivileges) {
			Util.sendCannotSendToChannelError(aSender, aChannel);
		}
	}

	protected User findUser(Network aNetwork, String aNick) {
		return aNetwork.getUser(aNick);
	}

	public String getName() {
		return "PRIVMSG";
	}

	public int getMinimumParameterCount() {
		return 2;
	}
}