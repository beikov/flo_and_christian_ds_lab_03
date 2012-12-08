package ds03.io;

import java.security.Key;

import ds03.util.SecurityUtils;

public class ClientHMACAuctionProtocolChannel extends
		AuctionProtocolChannelDecorator {

	private final Key key;

	public ClientHMACAuctionProtocolChannel(AuctionProtocolChannel delegate,
			Key key) {
		super(delegate);
		this.key = key;
	}

	@Override
	public String read() {
		String read = super.read();

		if (read == null) {
			return null;
		} else if (read.isEmpty()) {
			return read;
		}

		String hmac = read.substring(read.length() - 44, read.length());
		read = read.substring(0, read.length() - 44);

		if (!SecurityUtils.verifyHmac(read, key, hmac)) {
			throw new MessageIntegrityException(read);
		}

		return read;
	}
}
