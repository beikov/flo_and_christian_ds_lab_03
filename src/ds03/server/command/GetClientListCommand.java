package ds03.server.command;

import java.util.List;

import ds03.command.Command;
import ds03.command.Context;
import ds03.server.AuctionServerUserContext;
import ds03.server.service.UserService;

public class GetClientListCommand implements Command {

	private final UserService userService;

	public GetClientListCommand(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void execute(Context context, String[] args) {
		final StringBuilder sb = new StringBuilder();
		final List<String> users = userService.getLoggedInUsers();

		for (int i = 0; i < users.size(); i++) {
			String user = users.get(i);

			if (i != 0) {
				sb.append("\n");
			}

			AuctionServerUserContext userContext = userService.getUser(user);
			sb.append(userContext.getNotificationEndpoint()).append(" - ")
					.append(user);
		}

		context.getChannel().write(sb.toString());
	}
}
