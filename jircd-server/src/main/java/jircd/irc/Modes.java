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
 * @author markhale
 */
public final class Modes {
	private int lModes;
	private int uModes;

	private static int toMask(final char mode) {
		final int ch = Character.toLowerCase(mode);
		if(ch < 'a' || ch > 'z')
			throw new IllegalArgumentException("Invalid mode: "+mode);
		return 1<<(ch-'a');
	}

	public synchronized void add(final char mode) {
		final int mask = toMask(mode);
		if(Character.isUpperCase(mode))
			uModes |= mask;
		else
			lModes |= mask;
	}
	public synchronized void remove(final char mode) {
		final int mask = toMask(mode);
		if(Character.isUpperCase(mode))
			uModes &= ~mask;
		else
			lModes &= ~mask;
	}
	public synchronized boolean contains(final char mode) {
		final int mask = toMask(mode);
		if(Character.isUpperCase(mode))
			return ((uModes & mask) != 0);
		else
			return ((lModes & mask) != 0);
	}
	public synchronized String toString() {
		StringBuffer str = new StringBuffer("+");
		for(int i=0; i<26; i++) {
			if((lModes & (1<<i)) != 0)
				str.append((char) ('a'+i));
		}
		for(int i=0; i<26; i++) {
			if((uModes & (1<<i)) != 0)
				str.append((char) ('A'+i));
		}
		return str.toString();
	}
}
