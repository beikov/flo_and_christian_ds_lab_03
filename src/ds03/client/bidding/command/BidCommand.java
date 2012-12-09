package ds03.client.bidding.command;

import java.io.File;
import java.math.BigDecimal;
import java.security.Key;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.bouncycastle.openssl.PasswordFinder;

import ds03.client.bidding.BiddingUserContext;
import ds03.client.bidding.BiddingUserContextDecorator;
import ds03.client.util.ClientConsole;
import ds03.command.Command;
import ds03.io.AuctionProtocolChannel;
import ds03.io.ProtocolException;
import ds03.model.SingleBid;
import ds03.model.TimestampMessage;
import ds03.util.HandshakeUtils;
import ds03.util.NotificationEndpoint;
import ds03.util.SecurityUtils;

public class BidCommand extends AbstractBiddingCommand {

	@Override
	public void execute(final BiddingUserContext context, String[] args) {
		if (args.length != 3) {
			throw new IllegalArgumentException("Invalid parameters");
		}

		long auctionId = -1;
		BigDecimal amount = null;

		try {
			auctionId = Long.parseLong(args[1]);
			amount = new BigDecimal(args[2]);

		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid parameters", ex);
		}

		if (auctionId < 1) {
			throw new IllegalArgumentException("Invalid auction id");
		}

		if (amount == null || amount.compareTo(BigDecimal.ZERO) < 1) {
			throw new IllegalArgumentException("Invalid amount");
		}

		try {
			synchronized (context) {
				context.getChannel().write(join(args));
				String read = null;
				
				try {
					read = context.getChannel().read();
				} catch (ProtocolException e) {
					
				}

				if (read == null) {
					if (context.getChannel().isClosed()) {
						context.getQueuedSingleBids().put(
								new SingleBid(auctionId, amount),
								new HashSet<TimestampMessage>());
						/*
						 * Maybe a message to inform the client that the server is
						 * currently not online and the bid was queued
						 */
						
						
						context.getOut()
								.writeln(
										"Server currently not available. You can continue bidding.");
					}
				} else {
					context.getOut().writeln(read);
				}
			}
		} catch (Exception ex) {
			if (context.getChannel().isClosed()) {
				context.getQueuedSingleBids().put(
						new SingleBid(auctionId, amount),
						new HashSet<TimestampMessage>());
				/*
				 * Maybe a message to inform the client that the server is
				 * currently not online and the bid was queued
				 */
			}
			
			context.getOut()
					.writeln(
							"Server currently not available. You can continue bidding.");
		}
	}

}
