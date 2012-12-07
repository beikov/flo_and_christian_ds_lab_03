package ds03.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class AuctionProtocolStreamImpl implements AuctionProtocolChannel {

	private final OutputStream out;
	private final InputStream in;

	public AuctionProtocolStreamImpl(OutputStream out, InputStream in) {
		this.out = new BufferedOutputStream(out);
		this.in = new BufferedInputStream(in);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.util.AuctionProtocolStream#write(java.lang.String)
	 */
	@Override
	public void write(String response) {
		try {
			final byte[] bytes = response.getBytes();

			ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
			sizeBuffer.putInt(bytes.length);

			out.write(sizeBuffer.array());
			out.write(bytes);
			out.flush();
		} catch (IOException ex) {
			// Don't care about the errors since logging is not required
			// if(!tcpSocket.isClosed()){
			// ex.printStackTrace(System.err);
			// }
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.util.AuctionProtocolStream#read()
	 */
	@Override
	public String read() {
		try {
			final byte[] sizeBytes = new byte[4];

			if (in.read(sizeBytes) != 4) {
				throw new IllegalStateException(
						"Protocol error, not enough bytes are available to read");
			}

			final ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
			sizeBuffer.put(sizeBytes);
			final int size = sizeBuffer.getInt();

			if (size == -1) {
				return null;
			}

			final byte[] bytes = new byte[size];

			if (in.read(bytes, 0, size) != size) {
				throw new IllegalStateException(
						"Protocol error, not enough bytes are available to read");
			}

			return new String(bytes);
		} catch (IOException ex) {
			// Don't care about the errors since logging is not required
			// if(!tcpSocket.isClosed()){
			// ex.printStackTrace(System.err);
			// }

			return null;
		}
	}
}
