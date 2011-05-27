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

package jp.kurusugawa.jircd;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;

import jircd.jIRCdMBean;
import jircd.irc.Connection;
import jircd.irc.Listener;
import jircd.irc.MyStreamConnection;

import org.apache.log4j.Logger;

/**
 * Listens on a port and accepts new clients.
 * 
 * @author thaveman
 * @author markhale
 */
public class SocketListener extends Listener {
	private static final int BACKLOG = 16535;

	private static final Logger LOG = Logger.getLogger(SocketListener.class);
	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[] {};
	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[] {};

	private final ServerSocketFactory mFactory;

	private final ExecutorService mStreamThreadPool = Executors.newCachedThreadPool();

	private ServerSocket mServerSocket;

	public SocketListener(jIRCdMBean aJircd, String aAddress, int aPort, ExecutorService aPool, String aFactoryName) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		super(aJircd, aAddress, aPort, aPool, aFactoryName);
		if (aFactoryName.length() > 0) {
			Class<?> tFactoryClass = Class.forName(aFactoryName);
			Method tFactoryMethod = tFactoryClass.getMethod("getDefault", EMPTY_CLASS_ARRAY);
			mFactory = (ServerSocketFactory) tFactoryMethod.invoke(null, EMPTY_OBJECT_ARRAY);
		} else {
			// default factory
			mFactory = ServerSocketFactory.getDefault();
		}
	}

	public String toString() {
		return getClass().getName() + '[' + mFactory.getClass().getName() + ',' + boundAddress + ':' + boundPort + ']';
	}

	/** Waits for a connection */
	protected void waitForActivity() throws IOException {
		Socket tSocket = mServerSocket.accept();
		MyStreamConnection tConnection = new MyStreamConnection(tSocket, this, mStreamThreadPool);
		Connection.Handler tHandler = new Connection.Handler(jircd, tConnection);
		tConnection.setHandler(tHandler);
		tConnection.start();
	}

	public boolean bind() {
		try {
			if (boundAddress.equals("localhost")) {
				mServerSocket = mFactory.createServerSocket(boundPort, BACKLOG);
			} else {
				mServerSocket = mFactory.createServerSocket(boundPort, BACKLOG, InetAddress.getByName(boundAddress));
			}
		} catch (IOException ioe) {
			LOG.warn("Bind exception", ioe);
			return false;
		}
		return true;
	}

	public void close() {
		super.close();
		mStreamThreadPool.shutdown();
		try {
			mServerSocket.close();
		} catch (IOException e) {
			LOG.warn("Server socket close exception", e);
		}
	}
}
