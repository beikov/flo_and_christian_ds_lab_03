package ds03.io;

import org.bouncycastle.util.encoders.Base64;

public class Base64AuctionProtocolChannel extends
		AuctionProtocolChannelDecorator {

	public Base64AuctionProtocolChannel(AuctionProtocolChannel delegate) {
		super(delegate);
	}

	@Override
	public void write(String response) {
		super.write(new String(Base64.encode(response.getBytes())));
	}

	@Override
	public String read() {
		String read = super.read();

		if (read == null) {
			return null;
		} else if (read.isEmpty()) {
			return read;
		}

		return new String(Base64.decode(read.getBytes()));
	}
}
