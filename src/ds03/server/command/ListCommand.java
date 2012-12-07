package ds03.server.command;

import java.util.List;

import ds03.command.Command;
import ds03.command.Context;
import ds03.model.Auction;
import ds03.server.service.AuctionService;
import ds03.server.util.Formats;

public class ListCommand implements Command {

	private final AuctionService bidService;

	public ListCommand(AuctionService bidService) {
		this.bidService = bidService;
	}

	@Override
	public void execute(Context context, String[] args) {
		final StringBuilder sb = new StringBuilder();
		final List<Auction> auctions = bidService.getAuctions();

		for (int i = 0; i < auctions.size(); i++) {
			if (i != 0) {
				sb.append('\n');
			}

			final Auction auction = auctions.get(i);
			sb.append(auction.getId()).append(". '");
			sb.append(auction.getDescription());
			sb.append("' ");
			sb.append(auction.getUser());
			sb.append(' ');
			sb.append(Formats.getDateFormat().format(
					auction.getEndTimestamp().getTime()));
			sb.append(' ');
			sb.append(Formats.getNumberFormat().format(auction.getBidValue()));
			sb.append(' ');
			sb.append(auction.getBidUser() == null ? "none" : auction
					.getBidUser());
		}

		context.getChannel().write(sb.toString());
	}
}
