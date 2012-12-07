package ds03.client.bidding;

import ds03.command.Context;
import ds03.io.AuctionProtocolChannel;

public interface BiddingUserContext extends Context {

	public AuctionProtocolChannel getChannel();

}