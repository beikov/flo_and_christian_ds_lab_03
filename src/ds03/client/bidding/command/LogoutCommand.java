package ds03.client.bidding.command;

import ds03.client.bidding.BiddingUserContext;
import ds03.io.ProtocolException;

public class LogoutCommand extends AbstractBiddingCommand {

	public LogoutCommand(long waitTimeout) {
		super(waitTimeout);
	}

	@Override
	public void execute(BiddingUserContext context, String[] args) {
		if (!context.isLoggedIn()) {
			context.getOut().writeln("ERROR: You have to log in first!");
			return;
		}

		final boolean wasClosed = context.getChannel().isClosed();

		synchronized (context) {
			context.getChannel().write("!logout");
			String read = null;

			try {
				read = context.getChannel().read();
			} catch (ProtocolException e) {

			}

			if (read == null) {
				if (!wasClosed) {
					waitForReconnection(context);
					execute(context, args);
				} else {
					context.getOut()
							.writeln(
									"Server currently not available. You can continue bidding.");
				}
				return;
			} else {
				context.getOut().writeln(read);
			}
		}

		context.logout();
	}

}
