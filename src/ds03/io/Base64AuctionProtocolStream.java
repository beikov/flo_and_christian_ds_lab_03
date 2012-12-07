package ds03.io;

import org.bouncycastle.util.encoders.Base64;

public class Base64AuctionProtocolStream extends AuctionProtocolStreamDecorator {

	public Base64AuctionProtocolStream(AuctionProtocolChannel delegate) {
		super(delegate);
	}

	@Override
	public void write(String response) {
		super.write(new String(Base64.encode(response.getBytes())));
	}

	@Override
	public String read() {
		// TODO Auto-generated method stub
		return new String(Base64.decode(super.read().getBytes()));
	}
}
