package ds03.server;

import java.util.HashMap;
import java.util.Map;

import ds03.command.Command;
import ds03.server.command.BidCommand;
import ds03.server.command.CreateCommand;
import ds03.server.command.ListCommand;
import ds03.server.command.LoginCommand;
import ds03.server.command.LogoutCommand;
import ds03.server.service.AuctionService;
import ds03.server.service.UserService;
public class ClientHandler implements Runnable {

	private static final String[] NO_ARGS = new String[0];
	private static final Map<String, Command> loggedOutCommandMap = new HashMap<String, Command>();
	private static final Map<String, Command> loggedInCommandMap = new HashMap<String, Command>();
	private static final Command logoutCommand;
	private final AuctionServerUserContextImpl userConnection;

	static {
		final Command loginCommand = new LoginCommand(UserService.INSTANCE);
		logoutCommand = new LogoutCommand();

		loggedOutCommandMap.put("!login", loginCommand);
		loggedOutCommandMap.put("!logout", logoutCommand);
		loggedOutCommandMap.put("!list", new ListCommand(
				AuctionService.INSTANCE));

		loggedInCommandMap.put("!login", loginCommand);
		loggedInCommandMap.put("!logout", logoutCommand);

		loggedInCommandMap.put("!list",
				new ListCommand(AuctionService.INSTANCE));
		loggedInCommandMap.put("!create", new CreateCommand(
				AuctionService.INSTANCE));
		loggedInCommandMap.put("!bid", new BidCommand(AuctionService.INSTANCE));
	}

	public ClientHandler(final AuctionServerUserContextImpl userConnection) {
		this.userConnection = userConnection;
	}

	public void stop() {
		userConnection.close();
	}

	@Override
	public void run() {
		final AuctionServerUserContextImpl con = userConnection; // avoid field access
		String command;

		while (true) {
			command = con.getChannel().read();

			if (command == null || "!end".equals(command)) {
				break;
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

			final Command cmd = con.getUsername() == null ? loggedOutCommandMap
					.get(commandKey) : loggedInCommandMap.get(commandKey);

			if (cmd != null) {
				try {
					cmd.execute(con, commandArgs);
				} catch (Exception ex) {
					con.getChannel().write(ex.getMessage() == null ? ex.toString()
							: ex.getMessage());
				}
			} else {
				con.getChannel().write("Invalid command '" + commandKey + "'");
			}
		}

		con.close();
	}
}
