package ds03.io;

import java.security.Key;

import ds03.util.SecurityUtils;

public class ServerHMACAuctionProtocolChannel extends
		AuctionProtocolChannelDecorator {

	private final Key key;

	public ServerHMACAuctionProtocolChannel(AuctionProtocolChannel delegate,
			Key key) {
		super(delegate);
		this.key = key;
	}

	@Override
	public void write(String response) {
		String hmac = SecurityUtils.createHmac(response, key);
		super.write(response + hmac);
	}
}
