package ds03.client.management.command;

import org.apache.log4j.Logger;

import ds03.client.management.ManagementUserContext;

public class LoginCommand extends AbstractManagementCommand {
	private static final Logger LOG = Logger.getLogger(LoginCommand.class);

	@Override
	public void execute(ManagementUserContext context, String[] args) {

		String username = null;
		String password = null;

		if (args.length != 2) {
			throw new RuntimeException("Usage: !login <username> <password>");
		}

		username = args[0];
		password = args[1];
		try {
			if (context.isLoggedIn()) {
				context.getOut().writeln(
						"ERROR: You are currently not logged in.");

			} else if (context.login(username, password)) {
				context.getOut().writeln(username + " successfully logged in");
			} else {
				System.err.println("ERROR: Login failed");
			}
		} catch (Exception e) {
			LOG.error("Failed to get remote object", e);
		}
	}
}
