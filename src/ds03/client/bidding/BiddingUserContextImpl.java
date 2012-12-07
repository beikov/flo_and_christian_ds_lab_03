package ds03.client.bidding;

import ds03.client.util.ClientConsole;
import ds03.io.AuctionProtocolChannel;

public class BiddingUserContextImpl implements BiddingUserContext {
	private String username;
	private final ClientConsole clientConsole;
	private final AuctionProtocolChannel channel;

	public BiddingUserContextImpl(ClientConsole clientConsole,
			AuctionProtocolChannel channel) {
		this.clientConsole = clientConsole;
		this.channel = channel;
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
	public AuctionProtocolChannel getChannel() {
		return this.channel;
	}

}
