package ds03.event.handler;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import ds03.event.AuctionEndedEvent;
import ds03.event.BidWonEvent;
import ds03.model.Auction;
import ds03.server.service.AuctionService;
import ds03.server.service.BillingServiceSecure;
import ds03.util.ServiceLocator;

public class AuctionEndedEventHandler extends
		AbstractEventHandler<AuctionEndedEvent> {

	private static final Logger LOG = Logger
			.getLogger(AuctionEndedEventHandler.class);

	@Override
	public void handle(AuctionEndedEvent event) {
		Auction auction = AuctionService.INSTANCE.getAuction(event
				.getAuctionId());

		if (auction.getBidUser() != null) {
			DefaultEventHandler.INSTANCE.handle(new BidWonEvent(auction
					.getBidUser(), auction.getId(), auction.getBidValue()
					.doubleValue()));
		}

		super.handle(event);

		try {
			BillingServiceSecure billingServiceSecure = ServiceLocator.INSTANCE
					.getBillingService().login("john", "dslab2012");

			billingServiceSecure.billAuction(auction.getUser(),
					event.getAuctionId(), auction.getBidValue().doubleValue());
		} catch (RemoteException e) {
			LOG.warn("BillingService is not reachable!");
		}

	}

}
