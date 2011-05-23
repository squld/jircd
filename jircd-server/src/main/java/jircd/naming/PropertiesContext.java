package jircd.naming;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InsufficientResourcesException;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;

/**
 * @author Mark Hale
 */
class PropertiesContext implements Context {
	private static final NameParser parser = new FlatNameParser();
	private final Hashtable env;
	private final Properties bindings;

	public PropertiesContext(Hashtable env) throws NamingException {
		this(env, new Properties());
		String url = (String) env.get(Context.PROVIDER_URL);
		if(url != null) {
			try {
				InputStream in = new URL(url).openStream();
				try {
					bindings.load(in);
				} finally {
					in.close();
				}
			} catch(IOException ioe) {
				throw new InsufficientResourcesException(ioe.toString());
			}
		}
	}
	private PropertiesContext(Hashtable env, Properties bindings) {
		this.env = (env != null) ? (Hashtable)env.clone() : new Hashtable();
		this.bindings = bindings;
	}

	public Object lookup(final Name name) throws NamingException {
		if(name.isEmpty())
			return new PropertiesContext(env, bindings);

		Name localName;
		if(name instanceof CompositeName) {
			if(name.size() > 1)
				throw new InvalidNameException(name+" has more components than the namespace can handle");
			localName = parser.parse(name.get(0));
		} else {
			localName = name;
		}
		Object obj = bindings.get(localName.toString());
		if(obj == null)
			throw new NameNotFoundException(name+" not found");
		return obj;
	}
	public Object lookup(String name) throws NamingException {
		return lookup(new CompositeName(name));
	}
	public void bind(Name name, Object obj) throws NamingException {
		throw new OperationNotSupportedException();
	}
	public void bind(String name, Object obj) throws NamingException {
		bind(new CompositeName(name), obj);
	}
	public void rebind(Name name, Object obj) throws NamingException {
		throw new OperationNotSupportedException();
	}
	public void rebind(String name, Object obj) throws NamingException {
		rebind(new CompositeName(name), obj);
	}
	public void unbind(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}
	public void unbind(String name) throws NamingException {
		unbind(new CompositeName(name));
	}
	public void rename(Name oldName, Name newName) throws NamingException {
		throw new OperationNotSupportedException();
	}
	public void rename(String oldName, String newName) throws NamingException {
		rename(new CompositeName(oldName), new CompositeName(newName));
	}
	public NamingEnumeration list(Name name) throws NamingException {
		if(name.isEmpty()) {
			return new NamesEnumeration(bindings.keySet().iterator());
		}
		Object target = lookup(name);
		if(target instanceof Context) {
			Context ctx = (Context) target;
			try {
				return ctx.list("");
			} finally {
				ctx.close();
			}
		}
		throw new NotContextException(name+" cannot be listed");
	}
	public NamingEnumeration list(String name) throws NamingException {
		return list(new CompositeName(name));
	}
	public NamingEnumeration listBindings(Name name) throws NamingException {
		if(name.isEmpty()) {
			return new BindingsEnumeration(bindings.entrySet().iterator());
		}
		Object target = lookup(name);
		if(target instanceof Context) {
			Context ctx = (Context) target;
			try {
				return ctx.listBindings("");
			} finally {
				ctx.close();
			}
		}
		throw new NotContextException(name+" cannot be listed");
	}
	public NamingEnumeration listBindings(String name) throws NamingException {
		return listBindings(new CompositeName(name));
	}
	public void destroySubcontext(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}
	public void destroySubcontext(String name) throws NamingException {
		destroySubcontext(new CompositeName(name));
	}
	public Context createSubcontext(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}
	public Context createSubcontext(String name) throws NamingException {
		return createSubcontext(new CompositeName(name));
	}
	public Object lookupLink(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}
	public Object lookupLink(String name) throws NamingException {
		return lookupLink(new CompositeName(name));
	}
	public NameParser getNameParser(Name name) throws NamingException {
		return parser;
	}
	public NameParser getNameParser(String name) throws NamingException {
		return getNameParser(new CompositeName(name));
	}
	public Name composeName(Name name, Name prefix) throws NamingException {
		Name composed = (Name)prefix.clone();
		composed.addAll(name);
		return composed;
	}
	public String composeName(String name, String prefix) throws NamingException {
		return composeName(new CompositeName(name), new CompositeName(prefix)).toString();
	}
	public Object addToEnvironment(String key, Object value) throws NamingException {
		return env.put(key, value);
	}
	public Object removeFromEnvironment(String key) throws NamingException {
		return env.remove(key);
	}
	public Hashtable getEnvironment() throws NamingException {
		return (Hashtable)env.clone();
	}
	public void close() throws NamingException {
	}
	public String getNameInNamespace() throws NamingException {
		return "";
	}

	class NamesEnumeration implements NamingEnumeration {
		private final Iterator iter;

		NamesEnumeration(Iterator iter) {
			this.iter = iter;
		}
		public boolean hasMoreElements() {
			return hasMore();
		}
		public boolean hasMore() {
			return iter.hasNext();
		}
		public Object next() {
			String name = (String) iter.next();
			String className = bindings.get(name).getClass().getName();
			return new NameClassPair(name, className);
		}
		public Object nextElement() {
			return next();
		}
		public void close() {
		}
	}

	class BindingsEnumeration implements NamingEnumeration {
		private final Iterator iter;

		BindingsEnumeration(Iterator iter) {
			this.iter = iter;
		}
		public boolean hasMoreElements() {
			return hasMore();
		}
		public boolean hasMore() {
			return iter.hasNext();
		}
		public Object next() {
			Map.Entry entry = (Map.Entry) iter.next();
			return new Binding((String)entry.getKey(), entry.getValue());
		}
		public Object nextElement() {
			return next();
		}
		public void close() {
		}
	}
}
