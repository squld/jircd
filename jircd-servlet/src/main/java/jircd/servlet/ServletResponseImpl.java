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

import java.util.Locale;
import javax.servlet.ServletResponse;

public abstract class ServletResponseImpl implements ServletResponse {
	private String contentType;
	private String charset = "ISO-8859-1";
	private Locale locale = Locale.getDefault();
	protected boolean isCommitted;

	public boolean isCommitted() {
		return isCommitted;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getContentType() {
		return contentType;
	}
	public void setCharacterEncoding(String charset) {
		this.charset = charset;
	}
	public String getCharacterEncoding() {
		return charset;
	}
	public void setLocale(Locale locale) {
		this.locale = locale;
	}
	public Locale getLocale() {
		return locale;
	}
}
