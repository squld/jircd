package server;

import jp.kurusugawa.jircd.IRCDaemon;
import jp.kurusugawa.jircd.FreedomSecurityManager;
import jp.kurusugawa.jircd.JobManager;

static def initialize(shell, entity) {
}

static def finalize(shell, entity) {
}

static def loadPlugins(shell, entity) {
	IRCDaemon.get().loadService(FreedomSecurityManager.class);
	IRCDaemon.get().loadService(JobManager.class);
}
