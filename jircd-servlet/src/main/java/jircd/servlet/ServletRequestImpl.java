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

import java.util.Collections;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;
import java.util.Locale;
import javax.servlet.ServletRequest;

public abstract class ServletRequestImpl implements ServletRequest {
	protected final Hashtable attributes = new Hashtable();
	protected final Hashtable parameters = new Hashtable();
	protected final Vector locales = new Vector(1, 1);
	private String charset;

	public Object getAttribute(String name) {
		return attributes.get(name);
	}
	public Enumeration getAttributeNames() {
		return attributes.keys();
	}
	public void setAttribute(String name, Object object) {
		attributes.put(name, object);
	}
	public void removeAttribute(String name) {
		attributes.remove(name);
	}
	public String[] getParameterValues(String name) {
		return (String[]) parameters.get(name);
	}
	public String getParameter(String name) {
		String[] values = getParameterValues(name);
		return (values != null) ? values[0] : null;
	}
	public Enumeration getParameterNames() {
		return parameters.keys();
	}
	public Map getParameterMap() {
		return Collections.unmodifiableMap(parameters);
	}
	public String getCharacterEncoding() {
		return charset;
	}
	public void setCharacterEncoding(String charset) {
		this.charset = charset;
	}
	public Locale getLocale() {
		return (Locale) locales.firstElement();
	}
	public Enumeration getLocales() {
		return locales.elements();
	}
}
