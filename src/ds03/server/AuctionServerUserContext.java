package ds03.server;

import ds03.command.Context;
import ds03.event.DisconnectedEvent;
import ds03.event.EventHandler;
import ds03.event.LogoutEvent;

public interface AuctionServerUserContext extends Context {

	
	public void addLogoutListener(EventHandler<LogoutEvent> handler);

	public void addCloseListener(
			EventHandler<DisconnectedEvent> handler);

	public void close();

}