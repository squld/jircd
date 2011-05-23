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
 * @author markhale
 */
public final class Constants {
	private Constants() {}

	public static final int DEFAULT_PORT = 6667;
	public static final int DEFAULT_SSL_PORT = 994;
	public static final String CHARSET = "UTF8";
	public static final int MAX_MESSAGE_PARAMETERS = 15;
	/** Maximum message length including CR-LF. */
	public static final int MAX_MESSAGE_SIZE = 512;
	/** Maximum message length excluding CR-LF. */
	public static final int MAX_MESSAGE_LENGTH = MAX_MESSAGE_SIZE-2;
        /** Use \r\n for maximum compatibility */
        public static final String MESSAGE_TERMINATOR = "\r\n";
	public static final int MAX_NICK_LENGTH = 50;
	public static final int MAX_CHANNEL_LENGTH = 50;
	/** Maximum channel topic length */
	public static final int MAX_TOPIC_LENGTH = 400;

	public static final long SECS_TO_MILLIS = 1000L;
        
        public static final String LINK_VERSION = "021020000";
        public static final String LINK_FLAGS = "IRC|";

	public static final char CTCP_DELIMITER = 1;

	public static final String RPL_WELCOME = "001";
	public static final String RPL_YOURHOST = "002";
	public static final String RPL_CREATED = "003";
	public static final String RPL_MYINFO = "004";
	public static final String RPL_ISUPPORT = "005";
        public static final String RPL_STATSCOMMANDS = "212";
	public static final String RPL_ENDOFSTATS = "219";
	public static final String RPL_UMODEIS = "221";
	public static final String RPL_STATSUPTIME = "242";
	public static final String RPL_STATSOLINE = "243";
	public static final String RPL_LUSERCLIENT = "251";
	public static final String RPL_LUSEROP = "252";
	public static final String RPL_LUSERUNKNOWN = "253";
	public static final String RPL_LUSERCHANNELS = "254";
	public static final String RPL_LUSERME = "255";
	public static final String RPL_ADMINME = "256";
	public static final String RPL_ADMINLOC1 = "257";
	public static final String RPL_ADMINLOC2 = "258";
	public static final String RPL_ADMINEMAIL = "259";
	public static final String RPL_AWAY = "301";
	public static final String RPL_USERHOST = "302";
	public static final String RPL_ISON = "303";
	public static final String RPL_UNAWAY = "305";
	public static final String RPL_NOWAWAY = "306";
	public static final String RPL_WHOISUSER = "311";
	public static final String RPL_WHOISSERVER = "312";
	public static final String RPL_WHOISOPERATOR = "313";
	public static final String RPL_ENDOFWHO = "315";
	public static final String RPL_WHOISIDLE = "317";
	public static final String RPL_ENDOFWHOIS = "318";
	public static final String RPL_WHOISCHANNELS = "319";
	public static final String RPL_LISTSTART = "321";
	public static final String RPL_LIST = "322";
	public static final String RPL_LISTEND = "323";
	public static final String RPL_CHANNELMODEIS = "324";
	public static final String RPL_NOTOPIC = "331";
	public static final String RPL_TOPIC = "332";
	public static final String RPL_TOPICWHOTIME = "333";
	public static final String RPL_INVITING = "341";
	public static final String RPL_VERSION = "351";
	public static final String RPL_WHOREPLY = "352";
	public static final String RPL_NAMREPLY = "353";
	public static final String RPL_LINKS = "364";
	public static final String RPL_ENDOFLINKS = "365";
	public static final String RPL_ENDOFNAMES = "366";
	public static final String RPL_BANLIST = "367";
	public static final String RPL_ENDOFBANLIST = "368";
	public static final String RPL_INFO = "371";
	public static final String RPL_MOTD = "372";
	public static final String RPL_ENDOFINFO = "374";
	public static final String RPL_MOTDSTART = "375";
	public static final String RPL_ENDOFMOTD = "376";
	public static final String RPL_YOUREOPER = "381";
	public static final String RPL_REHASHING = "382";
	public static final String RPL_TIME = "391";

	public static final String ERR_NOSUCHNICK = "401";
	public static final String ERR_NOSUCHSERVER = "402";
	public static final String ERR_NOSUCHCHANNEL = "403";
	public static final String ERR_CANNOTSENDTOCHAN = "404";
	public static final String ERR_UNKNOWNCOMMAND = "421";
	public static final String ERR_NONICKNAMEGIVEN = "431";
	public static final String ERR_ERRONEUSNICKNAME = "432";
	public static final String ERR_NICKNAMEINUSE = "433";
	public static final String ERR_USERNOTINCHANNEL = "441";
	public static final String ERR_NOTONCHANNEL = "442";
	public static final String ERR_USERONCHANNEL = "443";
	public static final String ERR_NOTREGISTERED = "451";
	public static final String ERR_NEEDMOREPARAMS = "461";
	public static final String ERR_ALREADYREGISTRED = "462";
	public static final String ERR_CHANNELISFULL = "471";
	public static final String ERR_UNKNOWNMODE = "472";
	public static final String ERR_INVITEONLYCHAN = "473";
	public static final String ERR_BANNEDFROMCHAN = "474";
	public static final String ERR_BADCHANNELKEY = "475";
	public static final String ERR_BADCHANMASK = "476";
	public static final String ERR_NOCHANMODES = "477";
	public static final String ERR_BANLISTFULL = "478";
	public static final String ERR_NOPRIVILEGES = "481";
	public static final String ERR_CHANOPRIVSNEEDED = "482";
	public static final String ERR_NOOPERHOST = "491";
	public static final String ERR_UMODEUNKNOWNFLAG = "501";
	public static final String ERR_USERSDONTMATCH = "502";
}
