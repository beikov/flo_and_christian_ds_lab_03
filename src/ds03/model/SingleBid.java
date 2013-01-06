package ds03.model;

import java.math.BigDecimal;

public class SingleBid {

	private final long auctionId;
	private final BigDecimal amount;

	public SingleBid(long auctionId, BigDecimal amount) {
		super();
		this.auctionId = auctionId;
		this.amount = amount;
	}

	public long getAuctionId() {
		return auctionId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

}
