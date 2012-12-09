package ds03.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class AuctionProtocolChannelImpl implements AuctionProtocolChannel {

	private final Socket socket;
	private OutputStream out;
	private InputStream in;
	private volatile boolean closed = false;

	public AuctionProtocolChannelImpl(Socket socket) {
		this.socket = socket;
		
		try{
			this.out = new BufferedOutputStream(socket.getOutputStream());
			this.in = new BufferedInputStream(socket.getInputStream());
		} catch(Exception e){
			close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.util.AuctionProtocolStream#write(java.lang.String)
	 */
	@Override
	public void write(String response) {
		if(!closed){
			try {
				final byte[] bytes = response.getBytes();
	
				ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
				sizeBuffer.putInt(bytes.length);
	
				out.write(sizeBuffer.array());
				out.write(bytes);
				out.flush();
			} catch (Exception ex) {
				close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.util.AuctionProtocolStream#read()
	 */
	@Override
	public String read() {
		if(!closed){
			try {
				final byte[] sizeBytes = new byte[4];
	
				if (in.read(sizeBytes) != 4) {
					throw new ProtocolException(
							"Protocol error, not enough bytes are available to read");
				}
	
				final ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
				sizeBuffer.put(sizeBytes);
				final int size = sizeBuffer.getInt(0);
	
				if (size == -1) {
					return null;
				}
	
				final byte[] bytes = new byte[size];
	
				if (in.read(bytes, 0, size) != size) {
					throw new ProtocolException(
							"Protocol error, not enough bytes are available to read");
				}
	
				return new String(bytes);
			} catch (Exception ex) {
				close();
				return null;
			}
		}
		
		return null;
	}
	
	public void close(){
		closed = true;
		
		if(socket != null){
			try {
				socket.close();
			} catch (IOException e) {
				
			}
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}
	
	
}
