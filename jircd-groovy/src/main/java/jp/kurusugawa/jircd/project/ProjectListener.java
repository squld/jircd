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

package jp.kurusugawa.jircd.project;

import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jircd.irc.User;

public interface ProjectListener {
	public void initialize(ProjectChannel aChannel);

	public void addUser(ProjectChannel aChannel, User aUser);

	public void removeUser(ProjectChannel aChannel, User aUser);

	public void setTopic(ProjectChannel aChannel, User aUser, String aNewTopic);

	public void destroyChannel(ProjectChannel aChannel);

	public void createChannel(ProjectChannel aChannel);

	public void sendMessage(ProjectChannel aChannel, Message aMessage, RegisteredEntity aExcluded);
}