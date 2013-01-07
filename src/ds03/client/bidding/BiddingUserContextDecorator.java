package ds03.client.bidding;

import java.util.Map;
import java.util.Set;

import ds03.client.util.ClientConsole;
import ds03.client.util.P2PManager;
import ds03.io.AuctionProtocolChannel;
import ds03.model.SingleBid;
import ds03.model.TimestampMessage;
import ds03.util.NotificationEndpoint;

public abstract class BiddingUserContextDecorator implements BiddingUserContext {

	private final BiddingUserContext delegate;

	public BiddingUserContextDecorator(BiddingUserContext delegate) {
		super();
		this.delegate = delegate;
	}

	public boolean login(String username, String password) {
		return delegate.login(username, password);
	}

	public void setClients(Map<String, NotificationEndpoint> clients) {
		delegate.setClients(clients);
	}

	public void logout() {
		delegate.logout();
	}

	public boolean isLoggedIn() {
		return delegate.isLoggedIn();
	}

	public String getUsername() {
		return delegate.getUsername();
	}

	public Map<String, NotificationEndpoint> getClients() {
		return delegate.getClients();
	}

	public ClientConsole getOut() {
		return delegate.getOut();
	}

	public void close() {
		delegate.close();
	}

	public AuctionProtocolChannel getChannel() {
		return delegate.getChannel();
	}

	public boolean isClosed() {
		return delegate.isClosed();
	}

	public void setChannel(AuctionProtocolChannel channel) {
		delegate.setChannel(channel);
	}

	public String getServerHost() {
		return delegate.getServerHost();
	}

	public int getServerPort() {
		return delegate.getServerPort();
	}

	public NotificationEndpoint getNotificationEndpoint() {
		return delegate.getNotificationEndpoint();
	}

	@Override
	public void queueSingleBid(SingleBid singleBid) {
		delegate.queueSingleBid(singleBid);
	}

	@Override
	public Map<SingleBid, Set<TimestampMessage>> getQueuedSingleBids() {
		return delegate.getQueuedSingleBids();
	}

	public P2PManager getP2PManager() {
		return delegate.getP2PManager();
	}

}
