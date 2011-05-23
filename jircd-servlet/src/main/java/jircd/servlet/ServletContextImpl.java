package jircd.servlet;

import java.io.InputStream;
import java.net.URL;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.log4j.Logger;

public class ServletContextImpl implements ServletContext {
	private final String name;
	private final Properties parameters;
	private final Hashtable attributes = new Hashtable();
	private final Logger logger;

	public ServletContextImpl(String name, Properties parameters) {
		this.name = name;
		this.parameters = parameters;
		logger = Logger.getLogger("jircd.servlet.context."+name);
	}
	public String getInitParameter(String name) {
		return parameters.getProperty(name);
	}
	public Enumeration getInitParameterNames() {
		return parameters.propertyNames();
	}
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

	public void log(String msg) {
		logger.info(msg);
	}
        /** @deprecated */
	public void log(Exception exception, String msg) {
		logger.info(msg, exception);
	}
	public void log(String msg, Throwable throwable) {
		logger.info(msg, throwable);
	}

	public ServletContext getContext(String path) {
		return null;
	}
	public Set getResourcePaths(String path) {
		return null;
	}
	public URL getResource(String path) {
		return null;
	}
	public InputStream getResourceAsStream(String path) {
		return null;
	}
	public RequestDispatcher getRequestDispatcher(String path) {
		return null;
	}
	public RequestDispatcher getNamedDispatcher(String name) {
		return null;
	}

	public String getMimeType(String file) {
		return null;
	}
	public String getRealPath(String path) {
		return null;
	}
	public String getServerInfo() {
		return "jIRCd/" + jIRCd.VERSION_MAJOR + '.' + jIRCd.VERSION_MINOR + '.' + jIRCd.VERSION_PATCH;
	}
	public int getMajorVersion() {
		return 2;
	}
	public int getMinorVersion() {
		return 2;
	}
	public String getServletContextName() {
		return name;
	}

        /** @deprecated */
	public Servlet getServlet(String name) {
		return null;
	}
        /** @deprecated */
	public Enumeration getServlets() {
		return new EmptyEnumeration();
	}
        /** @deprecated */
	public Enumeration getServletNames() {
		return new EmptyEnumeration();
	}
		@Override
		public Dynamic addFilter(String aArg0, String aArg1) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Dynamic addFilter(String aArg0, Filter aArg1) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Dynamic addFilter(String aArg0, Class<? extends Filter> aArg1) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public void addListener(String aArg0) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public <T extends EventListener> void addListener(T aArg0) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void addListener(Class<? extends EventListener> aArg0) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public javax.servlet.ServletRegistration.Dynamic addServlet(String aArg0, String aArg1) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public javax.servlet.ServletRegistration.Dynamic addServlet(String aArg0, Servlet aArg1) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public javax.servlet.ServletRegistration.Dynamic addServlet(String aArg0, Class<? extends Servlet> aArg1) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public <T extends Filter> T createFilter(Class<T> aArg0) throws ServletException {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public <T extends EventListener> T createListener(Class<T> aArg0) throws ServletException {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public <T extends Servlet> T createServlet(Class<T> aArg0) throws ServletException {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public void declareRoles(String... aArg0) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public ClassLoader getClassLoader() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public String getContextPath() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public int getEffectiveMajorVersion() {
			// TODO Auto-generated method stub
			return 0;
		}
		@Override
		public int getEffectiveMinorVersion() {
			// TODO Auto-generated method stub
			return 0;
		}
		@Override
		public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public FilterRegistration getFilterRegistration(String aArg0) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public JspConfigDescriptor getJspConfigDescriptor() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public ServletRegistration getServletRegistration(String aArg0) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Map<String, ? extends ServletRegistration> getServletRegistrations() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public SessionCookieConfig getSessionCookieConfig() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public boolean setInitParameter(String aArg0, String aArg1) {
			// TODO Auto-generated method stub
			return false;
		}
		@Override
		public void setSessionTrackingModes(Set<SessionTrackingMode> aArg0) {
			// TODO Auto-generated method stub
			
		}
}
