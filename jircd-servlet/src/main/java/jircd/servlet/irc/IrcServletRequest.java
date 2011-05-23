/*
 * IRC Servlet API
 * Copyright (C) 2004 Mark Hale <markhale@users.sourceforge.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package jircd.servlet.irc;

import javax.servlet.ServletRequest;

public interface IrcServletRequest extends ServletRequest {
	/**
	 * Returns the nick of the sender.
	 */
	String getNick();
	/**
	 * Returns the name of the IRC command that generated this request, for example, PRIVMSG.
	 */
	String getCommand();
	/**
	 * Returns the body of the request as a string.
	 */
	String getText();
}
