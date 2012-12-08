package ds03.server.command;

import java.math.BigDecimal;

import ds03.command.Context;
import ds03.model.Auction;
import ds03.server.service.AuctionService;
import ds03.server.util.Formats;

public class GroupBidCommand extends BidCommand {

	public GroupBidCommand(AuctionService bidService) {
		super(bidService);
	}

	@Override
	protected void doBid(Context context, Long id, BigDecimal amount) {
		bidService.groupBid(context.getUsername(), id, amount);
		context.getChannel().write("Group bid waits for approval.");
	}

}
