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

import java.io.IOException;
import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;

/**
 * Provides an abstract class to be subclassed to create an IRC servlet.
 */
public abstract class IrcServlet extends GenericServlet {
	public static final String PRIVMSG = "PRIVMSG";
	public static final String NOTICE = "NOTICE";

	/**
	 * Called by the server (via the <code>service</code> method) to allow a servlet to handle a PRIVMSG message.
	 */
	protected void doPrivMsg(IrcServletRequest request, IrcServletResponse response) throws ServletException, IOException {}
	/**
	 * Called by the server (via the <code>service</code> method) to allow a servlet to handle a NOTICE message.
	 */
	protected void doNotice(IrcServletRequest request, IrcServletResponse response) throws ServletException, IOException {}
	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		IrcServletRequest ircRequest = (IrcServletRequest) request;
		IrcServletResponse ircResponse = (IrcServletResponse) response;
		String cmd = ircRequest.getCommand();
		if(PRIVMSG.equals(cmd))
			doPrivMsg(ircRequest, ircResponse);
		else if(NOTICE.equals(cmd))
			doNotice(ircRequest, ircResponse);
	}
}
