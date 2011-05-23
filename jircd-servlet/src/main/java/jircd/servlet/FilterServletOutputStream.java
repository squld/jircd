/*
 * IRC Servlet API (implementation)
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

package jircd.servlet;

import java.io.OutputStream;
import java.io.IOException;
import javax.servlet.ServletOutputStream;

public class FilterServletOutputStream extends ServletOutputStream {
	protected final OutputStream out;

	public FilterServletOutputStream(OutputStream out) {
		this.out = out;
	}
	public void write(int b) throws IOException {
		out.write(b);
	}
}
