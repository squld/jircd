package jircd.servlet;

import jircd.irc.Message;
import jircd.irc.ConnectedEntity;
import jircd.irc.User;
import jircd.irc_p10.Util;
import jircd.irc_p10.Server_P10;
import jircd.irc_p10.User_P10;
import jircd.servlet.irc.IrcServlet;

/**
 * A servlet on a server.
 * @author markhale
 */
public class Servlet extends User_P10 {
	private final IrcServlet servlet;

	public Servlet(String nick, String name, IrcServlet servlet, Server_P10 server) {
		super(nick, 0, Util.randomUserToken(), "Servlet", "jIRCd", name, server);
		this.servlet = servlet;
	}
	protected String maskHost(String hostname) {
		return hostname;
	}
	public void send(Message message) {
		ConnectedEntity sender = message.getSender();
		if(sender instanceof User) {
			User user = (User) sender;
			String cmd = message.getCommand();
			String text = message.getParameter(message.getParameterCount()-1);
			IrcServletRequestImpl request = new IrcServletRequestImpl(user, cmd, text);
			IrcServletResponseImpl response = new IrcServletResponseImpl(this, user, cmd);
			try {
				servlet.service(request, response);
				if(!response.isCommitted())
					response.commit();
			} catch(Exception e) {
				servlet.log("An exception occured while trying to service a request", e);
			}
		}
	}
}
