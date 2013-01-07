package ds03.client.bidding;

import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ds03.client.util.ClientConsole;
import ds03.client.util.P2PManager;
import ds03.io.AuctionProtocolChannel;
import ds03.io.AuctionProtocolChannelDecorator;
import ds03.io.AuctionProtocolChannelImpl;
import ds03.model.SingleBid;
import ds03.model.TimestampMessage;
import ds03.util.NotificationEndpoint;

public class BiddingUserContextImpl implements BiddingUserContext {
	private volatile String username;
	private volatile P2PManager manager;
	private final ClientConsole clientConsole;
	private final String host;
	private final int port;
	private volatile AuctionProtocolChannel channel;
	private final NotificationEndpoint notificationEndpoint;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private Map<String, NotificationEndpoint> clients = new HashMap<String, NotificationEndpoint>();
	private ConcurrentMap<SingleBid, Set<TimestampMessage>> queuedSingleBids = new ConcurrentHashMap<SingleBid, Set<TimestampMessage>>();
	private volatile boolean closed = false;

	public BiddingUserContextImpl(ClientConsole clientConsole, String host,
			int port, int notificationPort) {
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("Invalid port");
		}

		if (notificationPort < 1 || notificationPort > 65535) {
			throw new IllegalArgumentException("Invalid notification port");
		}

		this.clientConsole = clientConsole;
		this.host = host;
		this.port = port;

		try {
			Socket socket = new Socket(host, port);
			this.channel = new AuctionProtocolChannelImpl(socket);

			this.notificationEndpoint = new NotificationEndpoint(socket
					.getLocalAddress().getHostAddress(), notificationPort);
		} catch (Exception ex) {
			close();
			throw new RuntimeException("Could not setup server connection.", ex);
		}
	}

	@Override
	public NotificationEndpoint getNotificationEndpoint() {
		return notificationEndpoint;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.bidding.BiddingUserContext#login(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public boolean login(String username, String password) {
		this.username = username;

		if (this.manager == null) {
			// Only re instantiate when logout was done
			this.manager = new P2PManager(username,
					notificationEndpoint.getPort());
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.bidding.BiddingUserContext#logout()
	 */
	@Override
	public void logout() {
		this.username = null;
		this.manager.close();
		AuctionProtocolChannel channel = this.channel;

		while (channel != null
				&& channel instanceof AuctionProtocolChannelDecorator) {
			channel = ((AuctionProtocolChannelDecorator) channel).getDelegate();
		}

		this.channel = channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.bidding.BiddingUserContext#isLoggedIn()
	 */
	@Override
	public boolean isLoggedIn() {

		return username != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.bidding.BiddingUserContext#getUsername()
	 */
	@Override
	public String getUsername() {
		return this.username;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.bidding.BiddingUserContext#getOut()
	 */
	@Override
	public ClientConsole getOut() {
		return clientConsole;
	}

	@Override
	public synchronized AuctionProtocolChannel getChannel() {
		return this.channel;
	}

	@Override
	public synchronized void setChannel(AuctionProtocolChannel channel) {
		this.channel = channel;
	}

	@Override
	public void setClients(Map<String, NotificationEndpoint> clients) {
		lock.writeLock().lock();
		try {
			this.clients = clients;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Map<String, NotificationEndpoint> getClients() {
		lock.readLock().lock();
		try {
			return this.clients;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void close() {
		logout();
		closed = true;
		channel.close();
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public String getServerHost() {
		return host;
	}

	@Override
	public int getServerPort() {
		return port;
	}

	@Override
	public void queueSingleBid(SingleBid singleBid) {
		queuedSingleBids.put(singleBid, new HashSet<TimestampMessage>());
	}

	@Override
	public Map<SingleBid, Set<TimestampMessage>> getQueuedSingleBids() {
		return queuedSingleBids;
	}

	@Override
	public P2PManager getP2PManager() {
		return this.manager;
	}

}
