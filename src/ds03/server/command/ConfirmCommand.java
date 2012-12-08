package ds03.server.command;

import java.math.BigDecimal;

import ds03.command.Command;
import ds03.command.Context;
import ds03.model.Auction;
import ds03.server.service.AuctionService;
import ds03.server.util.Formats;

public class ConfirmCommand implements Command {

	protected final AuctionService bidService;

	public ConfirmCommand(AuctionService bidService) {
		this.bidService = bidService;
	}

	@Override
	public void execute(Context userContext, String[] args) {
		Long auctionId = null;
		BigDecimal amount = null;
		String user = null;

		if (args.length > 0) {
			try {
				auctionId = Long.parseLong(args[0]);
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

		if (args.length > 2) {
			user = args[2];
		}
		
		bidService.confirm(userContext.getUsername(), auctionId, amount, user);
		userContext.getChannel().write("!confirmed");
	}

}
