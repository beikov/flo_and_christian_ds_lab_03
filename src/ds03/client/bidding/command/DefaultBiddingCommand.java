package ds03.client.bidding.command;

import ds03.client.bidding.BiddingUserContext;
import ds03.io.ProtocolException;

public class DefaultBiddingCommand extends AbstractBiddingCommand {

	public DefaultBiddingCommand(long waitTimeout) {
		super(waitTimeout);
	}

	@Override
	public void execute(BiddingUserContext context, String[] args) {
		synchronized (context) {
			context.getChannel().write(join(args));
			printResults(context, args);
		}
	}

	protected void printResults(BiddingUserContext context, String[] args) {
		String read = null;
		final boolean wasClosed = context.getChannel().isClosed();
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
		} else {
			context.getOut().writeln(read);
		}
	}
}
