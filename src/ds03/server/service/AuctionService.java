package ds03.server.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import ds03.model.Auction;
import ds03.server.service.impl.AuctionServiceImpl;

public interface AuctionService extends Serializable {

	public static final AuctionService INSTANCE = new AuctionServiceImpl();

	public List<Auction> getAuctions();

	public Auction getAuction(long auctionId);

	public Auction createAuction(String user, Integer duration,
			String description);

	public Auction bid(String user, Long id, BigDecimal amount);
	
	public void groupBid(String user, Long id, BigDecimal amount);
	
	public void confirm(String user, Long id, BigDecimal amount, String bidUser);

	public void setSchedulerService(ScheduledExecutorService schedulerService);

}