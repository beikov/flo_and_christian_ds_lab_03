package ds03.model;

import java.math.BigDecimal;

public class TimestampMessage {

	private final String username;
	private final long auctionId;
	private final BigDecimal amount;
	private final long timestamp;
	private final String signature;

	public TimestampMessage(String message, String username) {
		super();
		this.username = username;

		if (message == null) {
			auctionId = -1;
			amount = null;
			timestamp = -1;
			signature = null;
		} else {
			// !timestamp <auctionID> <price> <timestamp> <signature>
			String[] messageParts = message.split("\\s");
			if (!"!timestamp".equals(messageParts[0])) {
				throw new IllegalArgumentException("Invalid params");
			}

			try {
				auctionId = Long.parseLong(messageParts[1]);
				amount = new BigDecimal(messageParts[2]);
				timestamp = Long.parseLong(messageParts[3]);
				signature = messageParts[4];
			} catch (Exception ex) {
				throw new IllegalArgumentException("Invalid params");
			}
		}
	}

	private TimestampMessage(String username, long timestamp, String signature) {
		this.username = username;
		this.timestamp = timestamp;
		this.signature = signature;
		this.auctionId = -1;
		this.amount = null;
	}

	public String getSignedBidFormat() {
		StringBuilder sb = new StringBuilder();
		sb.append(username).append(":").append(timestamp).append(":")
				.append(signature);
		return sb.toString();
	}

	public static TimestampMessage fromSignedBidFormat(String signedBidFormat) {
		String[] messageParts = signedBidFormat.split(":");

		try {
			String username = messageParts[0];
			long timestamp = Long.parseLong(messageParts[1]);
			String signature = messageParts[2];
			return new TimestampMessage(username, timestamp, signature);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid params");
		}
	}

	public String getUsername() {
		return username;
	}

	public long getAuctionId() {
		return auctionId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getSignature() {
		return signature;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimestampMessage other = (TimestampMessage) obj;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

}
