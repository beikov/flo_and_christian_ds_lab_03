package ds03.server;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import ds03.command.Command;
import ds03.command.util.CommandUtils;
import ds03.command.util.ExceptionHandler;
import ds03.io.ProtocolException;
import ds03.server.command.BidCommand;
import ds03.server.command.CreateCommand;
import ds03.server.command.ListCommand;
import ds03.server.command.LoginCommand;
import ds03.server.command.LogoutCommand;
import ds03.server.service.AuctionService;
import ds03.server.service.UserService;
import ds03.util.SecurityUtils;

public class ClientHandler implements Runnable {

	private static final Pattern handshakePattern = Pattern
			.compile("!login [a-zA-Z0-9_\\-]+ [a-zA-Z0-9/+]{43}=");
	private static final Map<String, Command> loggedOutCommandMap = new HashMap<String, Command>();
	private static final Map<String, Command> loggedInCommandMap = new HashMap<String, Command>();
	private static final Command logoutCommand;
	private final AuctionServerUserContextImpl context;

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

	public ClientHandler(final AuctionServerUserContextImpl context) {
		this.context = context;
	}

	public void stop() {
		context.close();
	}

	@Override
	public void run() {
		final AuctionServerUserContext con = context; // avoid field access

		while (true) {
			try {
				String req = con.getChannel().read();

				if (!con.isLoggedIn()) {
					final String plainAssumptedCommandName = req.split("\\s")[0];

					if (!loggedOutCommandMap
							.containsKey(plainAssumptedCommandName)) {
						req = SecurityUtils.decryptRsa(req,
								SecurityUtils.getServerPrivateKey());

						if (!handshakePattern.matcher(req).matches()) {
							con.getChannel()
									.write("Mismatched protocol format");
							break;
						}
					}
				}

				if (!CommandUtils.invokeCommand(req,
						con.isLoggedIn() ? loggedInCommandMap
								: loggedOutCommandMap, con,
						new ExceptionHandler() {

							@Override
							public void onException(Exception ex) {
								con.getChannel().write(ex.getMessage());
							}
						})) {
					break;
				}
			} catch (ProtocolException ex) {
				break;
			} catch (Exception ex) {
				break;
			}
		}

		con.close();
	}
}
