package jp.kurusugawa.jircd.project;

import java.io.PrintWriter;
import java.util.Map;

public interface ProjectEntity {
	public interface Context {
		public Map<String, Object> getVariables();

		public PrintWriter getErrorWriter();

		public PrintWriter getWriter();
	}

	public ProjectEntityContext getEntityContext();
}
