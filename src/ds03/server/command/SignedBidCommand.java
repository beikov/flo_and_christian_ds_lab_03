package ds03.server.command;

import java.io.File;
import java.math.BigDecimal;
import java.security.PublicKey;

import ds03.command.Context;
import ds03.model.Auction;
import ds03.model.TimestampMessage;
import ds03.server.service.AuctionService;
import ds03.util.SecurityUtils;

public class SignedBidCommand extends BidCommand {

	public SignedBidCommand(AuctionService bidService) {
		super(bidService);
	}

	@Override
	public void execute(Context context, String[] args) {
		/* check params */
		long auctionId = -1;
		BigDecimal amount = null;
		TimestampMessage firstEvidence = null;
		TimestampMessage secondEvidence = null;

		if (args.length != 4) {
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
		if (!checkEvidence(firstEvidence, auctionId, amount)
				|| !checkEvidence(secondEvidence, auctionId, amount)) {
			throw new RuntimeException("Invalid evidences.");
		}

		/* calc arithmetic mean */
		long arithmeticMeanFromTimestamp = (firstEvidence.getTimestamp() + secondEvidence
				.getTimestamp()) / 2;
		response(context, auctionId, amount,
				doBid(context, auctionId, amount, arithmeticMeanFromTimestamp));
	}

	protected Auction doBid(Context context, Long id, BigDecimal amount,
			long bidTimestamp) {
		return bidService.bid(context.getUsername(), id, amount, bidTimestamp);
	}

	private boolean checkEvidence(TimestampMessage evidence, long auctionId,
			BigDecimal amount) {
		StringBuilder sb = new StringBuilder();

		sb.append("!timestamp ").append(auctionId).append(" ").append(amount)
				.append(" ").append(evidence.getTimestamp());

		File evidencePublicKeyFile = new File(
				SecurityUtils.getPathToClientKeyDir(), evidence.getUsername()
						.toLowerCase() + ".pub.pem");

		if (!evidencePublicKeyFile.exists()) {
			throw new RuntimeException("ERROR: No public key for "
					+ evidence.getUsername() + " exists.");
		}

		PublicKey evidencePublicKey = SecurityUtils
				.getPublicKey(evidencePublicKeyFile.getAbsolutePath());

		return SecurityUtils.verifySignature(sb.toString(), evidencePublicKey,
				evidence.getSignature());
	}
}
