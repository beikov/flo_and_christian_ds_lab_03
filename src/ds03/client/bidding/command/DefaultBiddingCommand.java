package ds03.client.bidding.command;

import ds03.client.bidding.BiddingUserContext;

public class DefaultBiddingCommand extends AbstractBiddingCommand {

	@Override
	public void execute(BiddingUserContext context, String[] args) {
		context.getChannel().write(join(args));
		printResults(context, args);
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

	protected void printResults(BiddingUserContext context, String[] args) {
		context.getOut().writeln(context.getChannel().read());
	}
}
