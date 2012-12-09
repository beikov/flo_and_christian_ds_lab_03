package ds03.io;

public abstract class AuctionProtocolChannelDecorator implements
		AuctionProtocolChannel {
	private final AuctionProtocolChannel delegate;

	public AuctionProtocolChannelDecorator(AuctionProtocolChannel delegate) {
		super();
		this.delegate = delegate;
	}

	public AuctionProtocolChannel getDelegate() {
		return delegate;
	}

	public void write(String response) {
		delegate.write(response);
	}

	public String read() {
		return delegate.read();
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

}
