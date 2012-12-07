package ds03.client.bidding.command;

import ds03.client.bidding.BiddingUserContext;
import ds03.command.Command;
import ds03.command.Context;

public abstract class AbstractBiddingCommand implements Command {

	@Override
	public void execute(Context context, String[] args) {
		execute((BiddingUserContext) context, args);
	}
	
	public abstract void execute(BiddingUserContext context, String[] args);

}
