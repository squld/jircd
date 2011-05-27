package jp.kurusugawa.jircd.commands;

import java.util.Arrays;
import java.util.Set;

import jircd.irc.Command;
import jircd.irc.CommandContext;
import jircd.irc.Constants;
import jircd.irc.Message;
import jircd.irc.RegisteredEntity;
import jp.kurusugawa.jircd.IRCDaemon;

import org.apache.log4j.Logger;

public class Plugin implements Command {
	private static final Logger LOGGER = Logger.getLogger(Plugin.class);

	public int getMinimumParameterCount() {
		return 0;
	}

	public String getName() {
		return "PLUGIN";
	}

	public void invoke(RegisteredEntity aRegisteredEntity, String[] aParams) {
		if (aParams.length <= 0) {
			listCommands(aRegisteredEntity);
			return;
		}

		LOGGER.info(Arrays.toString(aParams));

		String tSubCommand = aParams[0];
		if ("list".equals(tSubCommand)) {
			listCommands(aRegisteredEntity);
		} else if ("reload".equals(tSubCommand)) {
			IRCDaemon.get().reloadPlugins();
			listCommands(aRegisteredEntity);
		}
	}

	private void listCommands(RegisteredEntity aRegisteredEntity) {
		Set<CommandContext> tCommandContexts = IRCDaemon.get().getCommandContexts();
		for (CommandContext tContext : tCommandContexts) {
			Message tMessage = new Message(Constants.RPL_INFO, aRegisteredEntity);
			tMessage.appendParameter("-");
			tMessage.appendParameter(tContext.getCommand().getName());
			tMessage.appendLastParameter(String.valueOf(tContext.getUsedCount()));
			aRegisteredEntity.send(tMessage);
		}
	}
}
