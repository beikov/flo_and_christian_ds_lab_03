package ds03.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class AuctionProtocolChannelImpl implements AuctionProtocolChannel {

	private final Socket socket;
	private DataOutputStream out;
	private DataInputStream in;
	private volatile boolean closed = false;

	public AuctionProtocolChannelImpl(Socket socket) {
		this.socket = socket;

		try {
			this.out = new DataOutputStream(socket.getOutputStream());
			this.in = new DataInputStream(socket.getInputStream());
		} catch (Exception e) {
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
		if (!closed) {
			try {
				out.writeUTF(response);
				out.flush();
			} catch (Exception ex) {
				close();
			}
		}
	}

	@Override
	public void write(byte[] response) {
		if (!closed) {
			try {
				out.writeInt(response.length);
				out.write(response);
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
		if (!closed) {
			try {
				return in.readUTF();
			} catch (Exception ex) {
				close();
				return null;
			}
		}

		return null;
	}

	@Override
	public byte[] readBytes() {
		if (!closed) {
			try {
				int size = in.readInt();
				
				if (size == -1) {
					return null;
				}
				
				final byte[] bytes = new byte[size];

				if (in.read(bytes, 0, size) != size) {
					throw new ProtocolException(
							"Protocol error, not enough bytes are available to read");
				}
			} catch (Exception ex) {
				close();
				return null;
			}
		}

		return null;
	}

	public void close() {
		closed = true;

		if (socket != null) {
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
