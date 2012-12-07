package ds03.server.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import ds03.model.Auction;
import ds03.server.service.impl.AuctionServiceImpl;
import ds03.server.util.AuctionRemoveTask;

public interface AuctionService extends Serializable {

	public static final AuctionService INSTANCE = new AuctionServiceImpl();
	public static final AuctionRemoveTask REMOVE_TASK = new AuctionRemoveTask();

	public List<Auction> getAuctions();

	public Auction getAuction(long auctionId);

	public Auction createAuction(String user, Integer duration,
			String description);

	public Auction bid(String user, Long id, BigDecimal amount);

}