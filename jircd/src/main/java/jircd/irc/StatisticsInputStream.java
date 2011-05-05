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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Mark
 */
public final class StatisticsInputStream extends FilterInputStream {
    private long bytesRead;
    private long markRead;
    
    /** Creates a new instance of StatisticsInputStream */
    public StatisticsInputStream(InputStream in) {
        super(in);
    }
    public long getBytesRead() {
        return bytesRead;
    }
    public int read() throws IOException {
        int b = in.read();
        bytesRead++;
        return b;
    }
    public int read(byte[] b, int offset, int len) throws IOException {
        int count = in.read(b, offset, len);
        if(count > 0)
            bytesRead += count;
        return count;
    }
    public long skip(long n) throws IOException {
        long count = in.skip(n);
        if(count > 0)
            bytesRead += count;
        return count;
    }
    public void mark(int readLimit) {
        in.mark(readLimit);
        markRead = bytesRead;
    }
    public void reset() throws IOException {
        bytesRead = markRead;
        in.reset();
    }
}
