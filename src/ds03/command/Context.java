package ds03.command;

import ds03.client.util.ClientConsole;
import ds03.io.AuctionProtocolChannel;

public interface Context {
	public boolean login(String username, String password);

	public void logout();

	public boolean isLoggedIn();

	public String getUsername();

	public ClientConsole getOut();
	
	public AuctionProtocolChannel getChannel();

}
