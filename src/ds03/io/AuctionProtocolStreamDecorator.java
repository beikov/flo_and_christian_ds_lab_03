package ds03.io;

public abstract class AuctionProtocolStreamDecorator implements
		AuctionProtocolChannel {
	private final AuctionProtocolChannel delegate;

	public AuctionProtocolStreamDecorator(AuctionProtocolChannel delegate) {
		super();
		this.delegate = delegate;
	}

	public void write(String response) {
		delegate.write(response);
	}

	public String read() {
		return delegate.read();
	}

}
