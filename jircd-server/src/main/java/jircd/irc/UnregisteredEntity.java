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
 * An unknown entity.
 * @author markhale
 */
public final class UnregisteredEntity extends ConnectedEntity {
        public static final String NO_NAME = "*";
	private final Server server;
	private String[] pass;
	private String name = NO_NAME;
	private String[] params;

	public UnregisteredEntity(Connection.Handler client, Server server) {
		super(client);
		if(client == null)
			throw new NullPointerException("The connection handler cannot be null");
		if(server == null)
			throw new NullPointerException("The server cannot be null");
		this.server = server;
	}
	public void setPass(String[] passParams) {
		pass = passParams;
	}
	public String[] getPass() {
		return pass;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return this.name;
	}
	/** User parameters */
	public void setParameters(String[] params) {
		this.params = params;
	}
	public String[] getParameters() {
		return params;
	}
	/** ID */
	public String toString() {
		return getName();
	}
	/**
	 * Returns the server this entity is connected to.
	 */
	public final Server getServer() {
		return server;
	}
	
	public void send(Message msg) {
            StringBuffer buf = new StringBuffer();
            // append command
            buf.append(msg.getCommand());
            
            // append parameters
            final int paramCount = msg.getParameterCount();
            if(paramCount > 0) {
                final int lastParamIndex = paramCount - 1;
                for(int i=0; i<lastParamIndex; i++)
                    buf.append(' ').append(msg.getParameter(i));
                if(msg.hasLastParameter())
                    buf.append(" :").append(msg.getParameter(lastParamIndex));
                else
                    buf.append(' ').append(msg.getParameter(lastParamIndex));
            }
            handler.sendMessage(buf.toString());
	}
}
