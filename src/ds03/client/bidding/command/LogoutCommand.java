package ds03.client.bidding.command;

import ds03.client.bidding.BiddingUserContext;

public class LogoutCommand extends AbstractBiddingCommand {

	@Override
	public void execute(BiddingUserContext context, String[] args) {
		context.getChannel().write("!logout");
		context.getOut().writeln(context.getChannel().read());

		context.logout();
	}

}
