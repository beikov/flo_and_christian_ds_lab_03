package ds03.io;

public interface AuctionProtocolChannel {

	public void write(String response);

	public void write(byte[] response);

	public String read();
	
	public byte[] readBytes();

	public void close();

	public boolean isClosed();

}