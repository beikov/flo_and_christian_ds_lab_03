package ds03.command.util;

import java.util.Map;

import ds03.command.Command;
import ds03.command.Context;
import ds03.io.ProtocolException;

public class CommandUtils {

	private static final String[] NO_ARGS = new String[0];

	public static boolean invokeCommand(String command,
			Map<String, Command> commandMap, final Context context) {
		return invokeCommand(command, commandMap, context,
				new ExceptionHandler() {

					@Override
					public void onException(Exception ex) {
						context.getOut().writeln(ex.getMessage());
					}
				});
	}

	public static boolean invokeCommand(String command,
			Map<String, Command> commandMap, Context context,
			ExceptionHandler eh) {

		if (command == null || "!exit".equals(command)) {
			return false;
		}

		final String[] commandParts = command.split("\\s");
		final String commandKey = commandParts[0];
		final String[] commandArgs;

		if (commandParts.length > 1) {
			commandArgs = new String[commandParts.length - 1];
			System.arraycopy(commandParts, 1, commandArgs, 0,
					commandArgs.length);
		} else {
			commandArgs = NO_ARGS;
		}

		final Command cmd = commandMap.get(commandKey);

		if (cmd != null) {
			try {
				cmd.execute(context, commandArgs);
			} catch (ProtocolException ex) {
				throw ex;
			} catch (Exception ex) {
				eh.onException(ex);
			}
		} else {
			context.getOut().writeln(
					"ERROR: Invalid command '" + commandKey + "'");
		}

		return true;
	}
}
