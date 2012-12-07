package ds03.server.command;

import ds03.command.Command;
import ds03.command.Context;
import ds03.server.AuctionServerUserContext;
import ds03.server.service.UserService;

public class LoginCommand implements Command {

	private final UserService userService;

	public LoginCommand(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void execute(Context context, String[] args) {
		String username = null;

		if (args.length > 0) {
			username = args[0];
		}

		if (context.isLoggedIn()) {
			context.getChannel().write("You have to log out first!");
		} else if (userService.login(username, (AuctionServerUserContext) context)) {
			try {
				context.login(username, "");
				context.getChannel().write("Successfully logged in as "
						+ username + "!");
			} catch (RuntimeException ex) {
				userService.logout(username);
				throw ex;
			}
		} else {
			context.getChannel().write("Already logged in");
		}
	}
}
