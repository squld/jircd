package jircd.naming;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

public class PropertiesContextFactory implements InitialContextFactory {
	public Context getInitialContext(Hashtable env) throws NamingException {
		return new PropertiesContext(env);
	}
}
