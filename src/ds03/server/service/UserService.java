package ds03.server.service;

import java.io.Serializable;
import java.util.List;

import ds03.server.AuctionServerUserContext;
import ds03.server.service.impl.UserServiceImpl;

public interface UserService extends Serializable {

	public static final UserService INSTANCE = new UserServiceImpl();

	public AuctionServerUserContext getUser(String username);

	public boolean login(String username,
			AuctionServerUserContext userConnection);

	public void logout(String username);

	public List<String> getLoggedInUsers();

}