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

package jircd.misc.commands;

import jircd.irc.Command;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;
import jircd.irc.Util;

/**
 * LOCALE <nick> [<language> [<COUNTRY> [<Variant>]]]
 * 601 <nick> [<language> [<COUNTRY> [<Variant>]]] :<description>
 * @author markhale
 */
public class Locale implements Command {
	public void invoke(RegisteredEntity src, String[] params) {
		User sender = (User) src;
		String dest = params[0];
		User userDest = src.getServer().getNetwork().getUser(dest);
		if (hasPermission(sender, userDest)) {
			if (params.length > 1) {
				java.util.Locale newLocale;
				switch (params.length) {
					case 2: newLocale = Util.getLocale(params[1]);
						break;
					case 3: newLocale = Util.getLocale(params[1], params[2]);
						break;
					default: newLocale = Util.getLocale(params[1], params[2], params[3]);
				}
				userDest.setLocale(newLocale);
				Message message = new Message("LOCALE", userDest);
				message.appendParameter(newLocale.getLanguage());
				message.appendParameter(newLocale.getCountry());
				message.appendParameter(newLocale.getVariant());
				userDest.send(message);
			} else {
				java.util.Locale locale = userDest.getLocale();
				Message message = new Message("601", userDest);
				message.appendParameter(locale.getLanguage());
				message.appendParameter(locale.getCountry());
				message.appendParameter(locale.getVariant());
				message.appendLastParameter(locale.getDisplayName(locale));
				src.send(message);
			}
		} else {
			Message message = new Message(Constants.ERR_USERSDONTMATCH, src);
			message.appendLastParameter(Util.getResourceString(src, "ERR_USERSDONTMATCH"));
			src.send(message);
		}
	}
	private boolean hasPermission(User user, User context) {
		return (user == context) || user.isModeSet(User.UMODE_OPER);
	}
	public String getName() {
		return "LOCALE";
	}
	public int getMinimumParameterCount() {
		return 1;
	}
}
