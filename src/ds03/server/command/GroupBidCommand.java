package ds03.server.command;

import java.math.BigDecimal;

import ds03.command.Context;
import ds03.model.Auction;
import ds03.server.service.AuctionService;

public class GroupBidCommand extends BidCommand {

	public GroupBidCommand(AuctionService bidService) {
		super(bidService);
	}

	@Override
	protected Auction doBid(Context context, Long id, BigDecimal amount) {
		bidService.groupBid(context.getUsername(), id, amount);
		return null;
	}

	@Override
	protected void response(Context context, Long id, BigDecimal amount,
			Auction auction) {
		context.getChannel().write("Group bid waits for approval.");
	}

}
