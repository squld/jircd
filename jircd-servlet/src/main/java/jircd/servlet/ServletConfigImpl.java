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

import java.util.Properties;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;

public class ServletConfigImpl implements ServletConfig {
	private final String name;
	private final Properties parameters;
	private final ServletContext context;

	public ServletConfigImpl(String name, Properties parameters, ServletContext context) {
		this.name = name;
		this.context = context;
		this.parameters = parameters;
	}
	public String getInitParameter(String name) {
		return parameters.getProperty(name);
	}
	public Enumeration getInitParameterNames() {
		return parameters.propertyNames();
	}
	public ServletContext getServletContext() {
		return context;
	}
	public String getServletName() {
		return name;
	}
}
