package ds03.client.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public abstract class ClientConsole {

	public static final ClientConsole sio = new PrintStreamClientConsole(
			System.out, System.in);

	public static PipedClientConsole piped(InputStream in) {
		return new PipedClientConsole(in);
	}

	public String prompt(String prompt) {
		write(prompt);
		return read();
	}

	public abstract String read();

	public abstract void writeln(String s);

	public abstract void write(String s);

	private static class PrintStreamClientConsole extends ClientConsole {

		private final PrintStream ps;
		private final BufferedReader in;

		public PrintStreamClientConsole(PrintStream ps, InputStream in) {
			this.ps = ps;
			this.in = new BufferedReader(new InputStreamReader(in));
		}

		public String read() {
			try {
				return in.readLine();
			} catch (IOException e) {
				return null;
			}
		}

		@Override
		public void writeln(String s) {
			ps.println(s);
			ps.flush();
		}

		@Override
		public void write(String s) {
			ps.print(s);
			ps.flush();
		}
	}

	public static ClientConsole fromStreams(PrintStream out, InputStream in) {
		return new PrintStreamClientConsole(out, in);
	}
}
