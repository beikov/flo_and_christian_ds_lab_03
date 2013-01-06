package ds03.io;

public interface AuctionProtocolChannel {

	public void write(String response);

	public String read();

	public void close();

	public boolean isClosed();

}