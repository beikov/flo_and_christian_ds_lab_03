package ds03.client.bidding.command;

import ds03.client.bidding.BiddingUserContext;
import ds03.command.Command;
import ds03.command.Context;

public abstract class AbstractBiddingCommand implements Command {

	private final long waitTimeout;

	public AbstractBiddingCommand(long waitTimeout) {
		this.waitTimeout = waitTimeout;
	}

	@Override
	public void execute(Context context, String[] args) {
		execute((BiddingUserContext) context, args);
	}

	public abstract void execute(BiddingUserContext context, String[] args);

	protected void waitForReconnection(BiddingUserContext context) {
		try {
			context.wait(waitTimeout);
		} catch (InterruptedException e) {
			// don't care
		}
	}

	protected String join(String[] args) {
		final StringBuilder sb = new StringBuilder();

		if (args.length > 1) {
			for (int i = 0; i < args.length - 1; i++) {
				sb.append(args[i]);
				sb.append(" ");
			}
		}

		sb.append(args[args.length - 1]);

		return sb.toString();
	}
}
