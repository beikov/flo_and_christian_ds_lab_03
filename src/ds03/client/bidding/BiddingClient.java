package ds03.client.bidding;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ds03.client.Client;
import ds03.client.bidding.command.DefaultBiddingCommand;
import ds03.client.bidding.command.LoginCommand;
import ds03.client.bidding.command.LogoutCommand;
import ds03.client.util.ClientConsole;
import ds03.command.Command;
import ds03.command.util.CommandUtils;
import ds03.command.util.ExceptionHandler;
import ds03.io.AuctionProtocolChannelImpl;
import ds03.io.MessageIntegrityException;
import ds03.util.SecurityUtils;

public class BiddingClient implements Client {

	private static final Logger LOG = Logger.getLogger(BiddingClient.class);
	private static Map<String, Command> loggedOutCommandMap = new HashMap<String, Command>();
	private static Map<String, Command> loggedInCommandMap = new HashMap<String, Command>();
	private final BiddingUserContext context;
	/* Socket connection to server */
	private final Socket socket;

	private final Runnable shutdownHook;

	static {

		loggedOutCommandMap.put("!login", new LoginCommand());
		loggedOutCommandMap.put("!logout", new LogoutCommand());
		loggedOutCommandMap.put("!list", new DefaultBiddingCommand());

		loggedInCommandMap.put("!login", new LoginCommand());
		loggedInCommandMap.put("!logout", new LogoutCommand());
		loggedInCommandMap.put("!list", new DefaultBiddingCommand());
		loggedInCommandMap.put("!create", new DefaultBiddingCommand());
		loggedInCommandMap.put("!bid", new DefaultBiddingCommand());
	}

	public BiddingClient(ClientConsole out, String host, int tcpPort) {
		try {
			this.socket = new Socket(host, tcpPort);

			context = new BiddingUserContextImpl(out,
					new AuctionProtocolChannelImpl(socket.getOutputStream(),
							socket.getInputStream()));

		} catch (Exception ex) {
			throw new RuntimeException("Could not connect to server", ex);
		}

		shutdownHook = new Runnable() {

			@Override
			public void run() {

				if (BiddingClient.this.socket != null) {
					try {
						BiddingClient.this.socket.close();
					} catch (Exception ex1) {
						// Ignore
					}
				}

			}
		};

		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
	}

	public void run() {

		final BiddingUserContext con = context; /* avoid getfield opcode */

		while (true) {
			final String request = readRequest();

			if (!con.isLoggedIn()) {
				if (!CommandUtils.invokeCommand(request, loggedOutCommandMap,
						con)) {
					break;
				}
			} else {

				if (!CommandUtils.invokeCommand(request, loggedInCommandMap,
						con, new ExceptionHandler() {

							@Override
							public void onException(Exception ex) {
								if (ex instanceof MessageIntegrityException) {
									con.getOut().writeln(ex.getMessage());
									CommandUtils.invokeCommand(request,
											loggedInCommandMap, con);
								} else {
									con.getOut().write(ex.getMessage());
								}
							}
						})) {
					break;
				}

			}
		}

		shutdownHook.run();
	}

	public static void main(String[] args) {
		if (args.length != 5) {
			usage();
		}

		String host = args[0];
		int tcpPort = 0;
		int udpPort = 0;
		String pathToPublicKey = args[3];
		String pathToClientKeyDir = args[4];

		try {
			tcpPort = Integer.parseInt(args[1]);
			udpPort = Integer.parseInt(args[2]);
		} catch (NumberFormatException ex) {
			LOG.error(ex);
			usage();
		}

		final Client client;

		try {
			client = new BiddingClient(ClientConsole.sio, host, tcpPort);
			SecurityUtils.init(udpPort, pathToPublicKey, pathToClientKeyDir);
		} catch (Exception ex) {
			throw new RuntimeException("Could not instantiate client", ex);
		}

		client.run();
	}

	private static void usage() {
		System.out.println("Usage: " + BiddingClient.class.getSimpleName()
				+ " <host> <tcpPort>");
		System.exit(1);
	}

	private String readRequest() {
		StringBuilder sb = new StringBuilder();

		if (context.isLoggedIn()) {
			sb.append(context.getUsername());

		}

		sb.append("> ");

		context.getOut().write(sb.toString());

		final String request = context.getOut().read();
		final String[] parts = request.split("\\s");
		final StringBuilder resultSb = new StringBuilder(request.length()
				+ parts[0].length() + 1);

		resultSb.append(parts[0]).append(" ");

		if (parts.length > 1) {
			for (int i = 0; i < parts.length - 1; i++) {
				resultSb.append(parts[i]);
				resultSb.append(" ");
			}
		}

		resultSb.append(parts[parts.length - 1]);
		return resultSb.toString();
	}
}
