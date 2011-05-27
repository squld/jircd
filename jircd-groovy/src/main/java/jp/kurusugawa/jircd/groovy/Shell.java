package jp.kurusugawa.jircd.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.ScriptException;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class Shell {
	private static final ThreadLocal<Shell> THREAD_SHELL;

	static {
		THREAD_SHELL = new ThreadLocal<Shell>();
	}

	public static Shell getCurrentThreadShell() {
		return THREAD_SHELL.get();
	}

	private ScriptEnvironment mScriptEnvironment;

	public Shell() {
		mScriptEnvironment = new ScriptEnvironment();
		getContext().setVariable("shell", this);
	}

	public Shell(ClassLoader aClassLoader) {
		mScriptEnvironment = new ScriptEnvironment(null, aClassLoader);
		getContext().setVariable("shell", this);
	}

	public void dive(String... aUrlStrings) throws MalformedURLException {
		URLClassLoader tClassLoader = createClassLoader(aUrlStrings);
		mScriptEnvironment = new ScriptEnvironment(mScriptEnvironment, tClassLoader);
	}

	private URLClassLoader createClassLoader(String... aUrlStrings) throws MalformedURLException {
		List<URL> tUrls = toUrlList(aUrlStrings);
		URL[] tUrlArray = tUrls.toArray(new URL[tUrls.size()]);
		if (mScriptEnvironment != null) {
			return new URLClassLoader(tUrlArray, mScriptEnvironment.getClassLoader());
		}
		return new URLClassLoader(tUrlArray);
	}

	private List<URL> toUrlList(String[] aUrlStrings) throws MalformedURLException {
		List<URL> tUrls = new ArrayList<URL>(aUrlStrings.length);
		for (String tUrlString : aUrlStrings) {
			tUrls.add(new URL(tUrlString));
		}
		return tUrls;
	}

	public Binding getContext() {
		return mScriptEnvironment.getGroovyShell().getContext();
	}

	public void exit() {
		if (mScriptEnvironment.getParentEnvironment() != null) {
			mScriptEnvironment = mScriptEnvironment.getParentEnvironment();
		} else {
			throw new IllegalStateException("Shell stack top!");
		}
	}

	public Object evaluate(String aScript) throws ScriptException {
		final boolean tShellBind = THREAD_SHELL.get() == null;
		if (tShellBind) {
			THREAD_SHELL.set(this);
		}
		try {
			return mScriptEnvironment.getGroovyShell().evaluate(aScript);
		} finally {
			if (tShellBind) {
				THREAD_SHELL.remove();
			}
		}
	}

	public void setWriter(PrintWriter aWriter) {
		getContext().setVariable("out", aWriter);
	}

	public PrintWriter getWriter() {
		return (PrintWriter) getContext().getVariable("out");
	}

	public void setErrorWriter(PrintWriter aWriter) {
		getContext().setVariable("err", aWriter);
	}

	public PrintWriter getErrorWriter() {
		return (PrintWriter) getContext().getVariable("err");
	}

	@Override
	public String toString() {
		GroovyShell tGroovyShell = mScriptEnvironment.getGroovyShell();
		return tGroovyShell.toString();
	}

	private static class ScriptEnvironment {
		private final ScriptEnvironment mParentEnvironment;

		private final GroovyShell mGroovyShell;

		private final ClassLoader mClassLoader;

		private ScriptEnvironment() {
			this(null, ScriptEnvironment.class.getClassLoader());
		}

		private ScriptEnvironment(ScriptEnvironment aParentEnvironment, ClassLoader aClassLoader) {
			mClassLoader = aClassLoader;
			mGroovyShell = new GroovyShell(mClassLoader);
			mParentEnvironment = aParentEnvironment;
			if (aParentEnvironment != null) {
				copyGroovyShellBindings(aParentEnvironment.getGroovyShell(), mGroovyShell);
			}
		}

		@SuppressWarnings("unchecked")
		private static void copyGroovyShellBindings(GroovyShell aSource, GroovyShell aDestination) {
			aDestination.getContext().getVariables().putAll(aSource.getContext().getVariables());
		}

		ScriptEnvironment getParentEnvironment() {
			return mParentEnvironment;
		}

		GroovyShell getGroovyShell() {
			return mGroovyShell;
		}

		private ClassLoader getClassLoader() {
			return mClassLoader;
		}
	}
}
