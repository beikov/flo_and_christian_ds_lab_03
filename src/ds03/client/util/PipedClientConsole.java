package ds03.client.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;

public class PipedClientConsole extends ClientConsole {

	private final OutputStream os;
	private final InputStream is;

	private final BufferedReader in;

	public PipedClientConsole(InputStream in) {
		this.in = new BufferedReader(new InputStreamReader(in));

		PipedOutputStream pos = new PipedOutputStream();

		try {
			this.os = new BufferedOutputStream(pos);
			this.is = new BufferedInputStream(new PipedInputStream(pos));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeln(String s) {
		try {
			final byte[] bytes = s.getBytes();

			ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
			sizeBuffer.putInt(bytes.length);

			os.write(sizeBuffer.array());
			os.write(bytes);
			os.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void write(String s) {
		try {
			final byte[] bytes = s.getBytes();

			ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
			sizeBuffer.putInt(bytes.length);

			os.write(sizeBuffer.array());
			os.write(bytes);
			os.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String read() {
		try {
			return in.readLine();
		} catch (IOException ex) {
			return null;
		}
	}

	public String readFromPipe() {
		try {
			final byte[] sizeBytes = new byte[4];

			if (is.read(sizeBytes) != 4) {
				throw new IllegalStateException(
						"Protocol error, not enough bytes are available to read");
			}

			final ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
			sizeBuffer.put(sizeBytes);
			final int size = sizeBuffer.getInt(0);

			if (size < 0) {
				return null;
			}

			final byte[] bytes = new byte[size];

			if (is.read(bytes, 0, size) != size) {
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
