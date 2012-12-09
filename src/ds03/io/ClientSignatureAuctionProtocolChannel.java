package ds03.io;

import java.security.Key;
import java.security.PrivateKey;

import ds03.util.SecurityUtils;

public class ClientSignatureAuctionProtocolChannel extends
		AuctionProtocolChannelDecorator {

	private final PrivateKey key;

	public ClientSignatureAuctionProtocolChannel(AuctionProtocolChannel delegate,
			PrivateKey key) {
		super(delegate);
		this.key = key;
	}

	@Override
	public void write(String response) {
		String signature = SecurityUtils.createSignature(response, key);
		super.write(response + " " + signature);
	}
}
