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
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import jircd.jIRCdMBean;

import org.apache.log4j.Logger;

/**
 *
 * @author Mark
 */
public class SelectorListener extends Listener {
    private static final int BACKLOG = 16535;
    private static final int DEFAULT_RECEIVE_BUFFER_SIZE = 4096;
    private static final int DEFAULT_SEND_BUFFER_SIZE = 4096;
    private static final Charset CHARSET = Charset.forName(Constants.CHARSET);
    
    private static final Logger logger = Logger.getLogger(SelectorListener.class);
    
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private SelectionKey serverKey;
    
    /** Creates a new instance of NIOListener */
    public SelectorListener(jIRCdMBean jircd, String address, int port, ExecutorService pool, String params) {
        super(jircd, address, port, pool, params);
    }
    
    public boolean bind() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(boundAddress, boundPort), BACKLOG);
            serverKey = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException ioe) {
            logger.warn("Bind exception", ioe);
            return false;
        }
        return true;
    }
    
    protected void waitForActivity() throws IOException {
        synchronized(selector) {
            final int numReady = selector.select();
            for(Iterator iter = selector.selectedKeys().iterator(); iter.hasNext(); iter.remove()) {
                SelectionKey key = (SelectionKey) iter.next();
                if(key == serverKey) {
                    if(key.isAcceptable())
                        doAccept();
                } else {
                    doSocketOps(key);
                }
            }
        }
    }
    private void doAccept() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        if(clientChannel != null) {
            clientChannel.configureBlocking(false);
            SelectionKey key = clientChannel.register(selector, SelectionKey.OP_READ);
            KeyConnection connection = new KeyConnection(key);
            Connection.Handler handler = new Connection.Handler(jircd, connection);
            connection.setHandler(handler);
            key.attach(connection);
            connectionOpened(connection);
        }
        
    }
    private void doSocketOps(SelectionKey key) throws IOException {
        if(key.isValid() && key.isReadable()) {
            doSocketRead(key);
        }
        if(key.isValid() && key.isWritable()) {
            doSocketWrite(key);
        }
    }
    
    private void doSocketRead(SelectionKey key) throws IOException {
        final SocketChannel channel = (SocketChannel) key.channel();
        final KeyConnection client = (KeyConnection) key.attachment();
        final ByteBuffer buffer = client.inBuffer;
        int bytesRead = 0;
        // read from channel
        while(buffer.hasRemaining() && (bytesRead = channel.read(buffer)) > 0) {
            client.bytesRead += bytesRead;
        }
        client.processInBuffer();
        // end of stream
        if(bytesRead < 0 && key.isValid()) {
            disconnect(key);
        }
    }
    
    private void doSocketWrite(SelectionKey key) throws IOException {
        final SocketChannel channel = (SocketChannel) key.channel();
        final KeyConnection client = (KeyConnection) key.attachment();
        final ByteBuffer buffer = client.outBuffer;
        client.processOutQueue();
        int bytesSent = 0;
        // write to channel
        while(buffer.hasRemaining() && (bytesSent = channel.write(buffer)) > 0) {
            client.bytesSent += bytesSent;
        }
        if(client.responseQueue.isEmpty() && !client.currentResponse.hasRemaining() && !buffer.hasRemaining()) {
            // nothing left to send
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }
    
    private void disconnect(SelectionKey key) {
        ((KeyConnection) key.attachment()).close();
    }
    public String toString() {
        return getClass().getName() + '[' + boundAddress + ':' + boundPort + ']';
    }
    
    public void close() {
        super.close();
        synchronized(selector) {
            for(Iterator iter=selector.keys().iterator(); iter.hasNext(); ) {
                SelectionKey key = (SelectionKey) iter.next();
                if(key == serverKey) {
                    try {
                        serverKey.channel().close();
                    } catch(IOException ioe) {
                        logger.warn("Server channel close exception", ioe);
                    }
                } else {
                    disconnect(key);
                }
            }
            try {
                selector.close();
            } catch(IOException ioe) {
                logger.warn("Selector close exception", ioe);
            }
        }
    }
    
    /**
     * Connection implementation using buffers.
     */
    class KeyConnection extends Connection {
        private final SelectionKey key;
        private final ByteBuffer inBuffer;
        private final ByteBuffer outBuffer;
        private final CharsetDecoder decoder = CHARSET.newDecoder();
        private final CharBuffer currentRequest = CharBuffer.allocate(Constants.MAX_MESSAGE_SIZE);
        private final CharsetEncoder encoder = CHARSET.newEncoder();
        private final Queue responseQueue = new ConcurrentLinkedQueue();
        private final CharBuffer currentResponse = CharBuffer.allocate(Constants.MAX_MESSAGE_SIZE);
        private long bytesRead;
        private int linesRead;
        private long bytesSent;
        private int linesSent;
        
        public KeyConnection(SelectionKey key) {
            super(((SocketChannel) key.channel()).socket(), SelectorListener.this);
            this.key = key;
            
            int inBufSize = DEFAULT_RECEIVE_BUFFER_SIZE;
            try {
                inBufSize = socket.getReceiveBufferSize();
            } catch(SocketException se) {
            } finally {
                inBuffer = ByteBuffer.allocateDirect(inBufSize);
            }
            
            int outBufSize = DEFAULT_SEND_BUFFER_SIZE;
            try {
                outBufSize = socket.getSendBufferSize();
            } catch(SocketException se) {
            } finally {
                outBuffer = ByteBuffer.allocateDirect(outBufSize);
            }
            
            // set the out buffer to the "nothing to send" position
            outBuffer.position(outBuffer.limit());
            currentResponse.position(currentResponse.limit());
        }
        
        public void writeLine(String str) {
            responseQueue.add(str);
            if(responseQueue.size() == 1) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        }
        
        private void processInBuffer() {
            inBuffer.flip();
            while(inBuffer.hasRemaining()) {
                readRequestFromReceiveBuffer();
            }
            inBuffer.compact();
        }
        private void readRequestFromReceiveBuffer() {
            final int skipTo = currentRequest.position();
            CoderResult rc = decoder.decode(inBuffer, currentRequest, false);
            currentRequest.flip();
            currentRequest.position(skipTo);
            while(currentRequest.hasRemaining()) {
                final char ch = currentRequest.get();
                if(ch == '\r' || ch == '\n') {
                    final int pos = currentRequest.position();
                    final boolean hasRemaining = currentRequest.hasRemaining();
                    currentRequest.rewind();
                    String line = currentRequest.subSequence(0, pos-1).toString();
                    if(ch == '\r' && hasRemaining && currentRequest.get(pos) == '\n')
                        currentRequest.position(pos+1);
                    else
                        currentRequest.position(pos);
                    try {
                        linesRead++;
                        if(logger.isDebugEnabled())
                            logger.debug("Message received from " + toString() + "\n\t" + line);
                        if(line.length() > 0)
                            handler.handleLine(line);
                    } catch(RuntimeException e) {
                        logger.warn("Exception occured in " + toString(), e);
                        handler.getEntity().disconnect(e.getMessage());
                    }
                    currentRequest.compact();
                    currentRequest.flip();
                }
            }
            currentRequest.limit(currentRequest.capacity());
        }
        
        private void processOutQueue() {
            outBuffer.compact();
            if(currentResponse.hasRemaining()) {
                writeResponseToSendBuffer();
            }
            for(String str; !currentResponse.hasRemaining() && (str = (String) responseQueue.poll()) != null; ) {
                // append some of the next response to the end of the buffer
                currentResponse.clear();
                currentResponse.put(str);
                currentResponse.put(Constants.MESSAGE_TERMINATOR);
                currentResponse.flip();
                writeResponseToSendBuffer();
                linesSent++;
                if(logger.isDebugEnabled())
                    logger.debug("Message sent to " + toString() + "\n\t" + str);
            }
            outBuffer.flip();
        }
        private void writeResponseToSendBuffer() {
            CoderResult rc = encoder.encode(currentResponse, outBuffer, false);
        }
        
        public long getBytesRead() {
            return bytesRead;
        }
        public int getLinesRead() {
            return linesRead;
        }
        public long getBytesSent() {
            return bytesSent;
        }
        public int getLinesSent() {
            return linesSent;
        }
        
        protected void close() {
            logger.debug("Closing connection "+toString());
            try {
                key.channel().close();
            } catch(IOException e) {
                logger.debug("Exception on channel close", e);
            } finally {
                manager.connectionClosed(this);
            }
        }
        
    }
}
