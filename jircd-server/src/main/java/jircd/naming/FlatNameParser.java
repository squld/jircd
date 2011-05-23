package jircd.naming;

import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * @author Mark Hale
 */
class FlatNameParser implements NameParser {
	private static final Properties syntax = new Properties();
	static {
		syntax.put("jndi.syntax.direction", "flat");
		syntax.put("jndi.syntax.ignorecase", "false");
	}

	public Name parse(String name) throws NamingException {
		return new CompoundName(name, syntax);
	}
}
