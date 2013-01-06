package ds03.client.bidding;

import java.util.Map;
import java.util.Set;

import ds03.command.Context;
import ds03.model.SingleBid;
import ds03.model.TimestampMessage;
import ds03.util.NotificationEndpoint;

public interface BiddingUserContext extends Context {

	public void setClients(Map<String, NotificationEndpoint> clients);

	public Map<String, NotificationEndpoint> getClients();

	public void close();

	public boolean isClosed();

	public String getServerHost();

	public int getServerPort();

	public void queueSingleBid(SingleBid singleBid);

	public Map<SingleBid, Set<TimestampMessage>> getQueuedSingleBids();

}