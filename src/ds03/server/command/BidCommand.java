package ds03.server.command;

import java.math.BigDecimal;

import ds03.command.Command;
import ds03.command.Context;
import ds03.model.Auction;
import ds03.server.service.AuctionService;
import ds03.server.util.Formats;

public class BidCommand implements Command {

	private final AuctionService bidService;

	public BidCommand(AuctionService bidService) {
		this.bidService = bidService;
	}

	@Override
	public void execute(Context context, String[] args) {
		Long id = null;
		BigDecimal amount = null;

		if (args.length > 0) {
			try {
				id = Long.parseLong(args[0]);
			} catch (NumberFormatException ex) {
				/* Service will throw an exception with a good message */
			}
		}

		if (args.length > 1) {
			try {
				amount = new BigDecimal(args[1]);
			} catch (NumberFormatException ex) {
				/* Service will throw an exception with a good message */
			}
		}

		final Auction auction = bidService.bid(context.getUsername(), id,
				amount);
		final boolean success = auction.getBidValue().compareTo(amount) < 0;
		final StringBuilder sb = new StringBuilder();

		sb.append("You ");

		if (success) {
			sb.append("successfully");
		} else {
			sb.append("unsuccessfully");
		}

		sb.append(" bid with ");
		sb.append(amount);
		sb.append(" on '");
		sb.append(auction.getDescription());
		sb.append("'");

		if (!success) {
			sb.append(". Current highest bid is ");
			sb.append(Formats.getNumberFormat().format(auction.getBidValue()));
		}

		sb.append('.');

		context.getChannel().write(sb.toString());
	}
}
