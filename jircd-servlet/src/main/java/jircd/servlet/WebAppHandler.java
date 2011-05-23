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

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class WebAppHandler extends DefaultHandler {
	protected final Properties contextParameters = new Properties();
	protected final Map servlets = new HashMap();
	protected final Map mappings = new HashMap();
	protected final Map initParameters = new HashMap();
	protected String contextName;
	private int depth;
	private String name;
	private String description;
	private String servletName;
	private String servletClass;
	private String urlPattern;
	private StringBuffer buffer = new StringBuffer();

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		depth++;
		buffer.setLength(0);
	}
	public void endElement(String uri, String localName, String qName) {
		if(qName.equals("servlet-name")) {
			servletName = buffer.toString().trim();
		} else if(qName.equals("servlet-class")) {
			servletClass = buffer.toString().trim();
		} else if(qName.equals("display-name")) {
			name = buffer.toString().trim();
			if(depth == 2)
				contextName = name;
		} else if(qName.equals("description")) {
			description = buffer.toString().trim();
		} else if(qName.equals("url-pattern")) {
			urlPattern = buffer.toString().trim();
		} else if(qName.equals("servlet")) {
			servlets.put(servletName, servletClass);
		} else if(qName.equals("servlet-mapping")) {
			mappings.put(urlPattern, servletName);
		}
		depth--;
	}
	public void characters(char[] ch, int start, int length) {
		buffer.append(ch, start, length);
	}
}
