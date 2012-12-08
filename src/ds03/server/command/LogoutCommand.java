package ds03.server.command;

import ds03.command.Command;
import ds03.command.Context;

public class LogoutCommand implements Command {

	@Override
	public void execute(Context context, String[] args) {
		if (!context.isLoggedIn()) {
			context.getChannel().write("You have to log in first!");
		} else {
			context.getChannel()
					.write("Successfully logged out as "
							+ context.getUsername() + "!");

			context.logout();
		}
	}
}
