package ds03.client.management.command;

import ds03.client.management.ManagementUserContext;

public class LogoutCommand extends AbstractManagementCommand {

	@Override
	public void execute(ManagementUserContext context, String[] args) {
		if (args.length != 0) {
			throw new RuntimeException("Usage: !logout");
		}
		if (!context.isLoggedIn()) {
			context.getOut().writeln("You have to log in first!");
		} else {
			context.getOut().writeln(
					context.getUsername() + " successfully logged out");
			context.logout();
		}
	}
}
