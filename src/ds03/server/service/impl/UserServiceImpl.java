package ds03.server.service.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ds03.event.EventHandler;
import ds03.event.LoginEvent;
import ds03.event.LogoutEvent;
import ds03.event.handler.DefaultEventHandler;
import ds03.server.AuctionServerUserContext;
import ds03.server.service.UserService;

public class UserServiceImpl implements UserService {

	private static final long serialVersionUID = 1L;
	private final ConcurrentMap<String, AuctionServerUserContext> users = new ConcurrentHashMap<String, AuctionServerUserContext>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.service.impl.UserService#getUser(java.lang.String)
	 */
	@Override
	public AuctionServerUserContext getUser(String username) {
		checkUsername(username);
		return users.get(username);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.service.impl.UserService#login(java.lang.String,
	 * ds03.server.UserConnection)
	 */
	@Override
	public boolean login(String username,
			AuctionServerUserContext userConnection) {
		checkUsername(username);

		if (users.putIfAbsent(username, userConnection) == null) {
			DefaultEventHandler.INSTANCE.handle(new LoginEvent(username));
			userConnection.addLogoutListener(new EventHandler<LogoutEvent>() {

				@Override
				public void handle(LogoutEvent event) {
					UserServiceImpl.this.logout(event.getUser());
				}
			});

			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.service.impl.UserService#logout(java.lang.String)
	 */
	@Override
	public void logout(String username) {
		checkUsername(username);
		users.remove(username);
		DefaultEventHandler.INSTANCE.handle(new LogoutEvent(username));
	}

	private static void checkUsername(String username) {
		if (username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Invalid username");
		}
	}
}
