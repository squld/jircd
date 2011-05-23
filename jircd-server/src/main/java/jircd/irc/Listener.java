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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import jircd.jIRCdMBean;

import org.apache.log4j.Logger;

/**
 * @author markhale
 */
public abstract class Listener extends ConnectionManager implements Runnable {
    private static final Logger logger = Logger.getLogger(Listener.class);
    
    protected final jIRCdMBean jircd;
    protected final String boundAddress;
    protected final int boundPort;
    private final ExecutorService threadPool;
    private Future future;
    
    public Listener(jIRCdMBean jircd, String address, int port, ExecutorService pool, String params) {
        this.jircd = jircd;
        boundAddress = address;
        boundPort = port;
        threadPool = pool;
    }
    
    public final synchronized void start() {
        if(future == null || future.isDone()) {
            future = threadPool.submit(this);
        }
    }
    public final void run() {
        while (!Thread.interrupted()) {
            try {
                waitForActivity();
            } catch (IOException ioe) {
                logger.warn("IOException in " + toString(), ioe);
            }
        }
    }
    protected abstract void waitForActivity() throws IOException;
    public final synchronized void stop() {
        if(future != null) {
            future.cancel(true);
            future = null;
        }
    }
    
    public abstract boolean bind();
    public void close() {
        stop();
    }
    
    public final String getAddress() {
        return boundAddress;
    }
    public final int getPort() {
        return boundPort;
    }
}
