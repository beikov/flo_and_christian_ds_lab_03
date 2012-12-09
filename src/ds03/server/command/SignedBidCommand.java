package ds03.server.command;

import java.math.BigDecimal;

import ds03.command.Command;
import ds03.command.Context;
import ds03.model.Auction;
import ds03.model.TimestampMessage;
import ds03.server.service.AuctionService;
import ds03.server.util.Formats;

public class SignedBidCommand implements Command {

	protected final Command bidCommand;

	public SignedBidCommand(Command bidCommand) {
		this.bidCommand = bidCommand;
	}

	@Override
	public void execute(Context context, String[] args) {
		/* check params */
		long auctionId = -1;
		BigDecimal amount = null;
		TimestampMessage firstEvidence = null;
		TimestampMessage secondEvidence = null;
		
		if(args.length != 4) {
			throw new RuntimeException("Invalid params for !signed command.");
		}
		
		try {
			auctionId = Long.parseLong(args[0]);
			amount = new BigDecimal(args[1]);
			firstEvidence = TimestampMessage.fromSignedBidFormat(args[2]);
			secondEvidence = TimestampMessage.fromSignedBidFormat(args[3]);
		} catch (Exception ex) {
			throw new RuntimeException("Invalid params for !signed command.");	
		}
		
		/* Integrity check */
		
		/* check that bid was placed before auction end */
		//long arithmeticMeanFromTimestamp
		bidCommand.execute(context, new String[]{args[0], args[1]});
	}
}
