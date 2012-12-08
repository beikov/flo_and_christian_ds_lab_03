package ds03.server.command;

import ds03.command.Command;
import ds03.command.Context;
import ds03.model.Auction;
import ds03.server.service.AuctionService;
import ds03.server.util.Formats;

public class CreateCommand implements Command {

	private final AuctionService bidService;

	public CreateCommand(AuctionService bidService) {
		this.bidService = bidService;
	}

	@Override
	public void execute(Context context, String[] args) {
		final StringBuilder sb = new StringBuilder();

		for (int i = 1; i < args.length; i++) {
			if (i != 1) {
				sb.append(' ');
			}

			sb.append(args[i]);
		}

		Integer duration = null;

		if (args.length > 0) {
			try {
				duration = Integer.parseInt(args[0]);
			} catch (NumberFormatException ex) {
				/* Service will throw an exception with a good message */
			}
		}

		final Auction auction = bidService.createAuction(context.getUsername(),
				duration, sb.toString());

		sb.insert(0, "An auction '");
		sb.append("' with id ");
		sb.append(auction.getId());
		sb.append(" has been created and will end on ");
		sb.append(Formats.getDateFormat().format(
				auction.getEndTimestamp().getTime()));
		sb.append('.');

		context.getChannel().write(sb.toString());
	}
}
