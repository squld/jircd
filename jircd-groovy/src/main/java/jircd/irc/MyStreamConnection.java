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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.Logger;

/**
 * Connection implementation using streams.
 * 
 * @author markhale
 */
public class MyStreamConnection extends Connection implements Runnable {
	private static final Logger logger = Logger.getLogger(MyStreamConnection.class);

	private final StatisticsInputStream inputStats;

	private final StatisticsOutputStream outputStats;

	private final LineNumberReader input;

	private final BufferedWriter output;

	private final ExecutorService threadPool;

	private Future future;

	private int linesSent;

	public MyStreamConnection(Socket socket, ConnectionManager manager, ExecutorService pool) throws IOException {
		super(socket, manager);
		inputStats = new StatisticsInputStream(socket.getInputStream());
		outputStats = new StatisticsOutputStream(socket.getOutputStream());
		input = new LineNumberReader(new InputStreamReader(inputStats, Constants.CHARSET), Constants.MAX_MESSAGE_SIZE);
		output = new BufferedWriter(new OutputStreamWriter(outputStats, Constants.CHARSET), Constants.MAX_MESSAGE_SIZE);
		threadPool = pool;
		logger.debug("Initiated connection " + toString());
		manager.connectionOpened(this);
	}

	public final synchronized void start() {
		if (future == null || future.isDone()) {
			try {
				future = threadPool.submit(this);
			} catch (RejectedExecutionException ree) {
				handler.getEntity().disconnect(ree.getMessage());
			}
		}
	}

	public final void run() {
		while (!Thread.interrupted()) {
			try {
				String line = input.readLine(); // accept common line
				// terminators for maximum
				// compatibility
				if (logger.isDebugEnabled()) {
					logger.debug("Message received from " + toString() + "\n\t" + line);
				}
				if (line != null && line.length() > 0) {
					char tFirstChar = Character.toUpperCase(line.charAt(0));
					if (tFirstChar == ':' || '0' <= tFirstChar && tFirstChar <= '9' || 'A' <= tFirstChar && tFirstChar <= 'Z') {
					} else {
						line = "U_" + line;
					}
					handler.handleLine(line);
				} else if (line == null) {
					handler.getEntity().disconnect("Connection reset by peer");
					return;
				}
			} catch (IOException e) {
				handler.getEntity().disconnect(e.getMessage());
				return;
			} catch (RuntimeException e) {
				logger.warn("Exception occured in " + toString(), e);
				handler.getEntity().disconnect(e.getMessage());
				return;
			}
		}
	}

	public final synchronized void stop() {
		if (future != null) {
			future.cancel(true);
			future = null;
		}
	}

	public void writeLine(String text) {
		// Do not use PrintWriter.println() since that depends on the system
		// property line.separator,
		try {
			output.write(text);
			output.write(Constants.MESSAGE_TERMINATOR);
			output.flush();
			linesSent++;
			if (logger.isDebugEnabled())
				logger.debug("Message sent to " + toString() + "\n\t" + text);
		} catch (IOException e) {
			logger.debug("Exception occurred while sending message", e);
		}
	}

	public long getBytesSent() {
		return outputStats.getBytesWritten();
	}

	public int getLinesSent() {
		return linesSent;
	}

	public long getBytesRead() {
		return inputStats.getBytesRead();
	}

	public int getLinesRead() {
		return input.getLineNumber();
	}

	protected void close() {
		logger.debug("Closing connection " + toString());
		stop();
		try {
			socket.close();
		} catch (IOException e) {
			logger.debug("Exception on socket close", e);
		} finally {
			manager.connectionClosed(this);
		}
	}
}
