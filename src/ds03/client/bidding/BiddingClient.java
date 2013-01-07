package ds03.client.bidding;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import ds03.client.Client;
import ds03.client.bidding.command.BidCommand;
import ds03.client.bidding.command.DefaultBiddingCommand;
import ds03.client.bidding.command.GetClientListCommand;
import ds03.client.bidding.command.LoginCommand;
import ds03.client.bidding.command.LogoutCommand;
import ds03.client.util.ClientConsole;
import ds03.client.util.P2PManager;
import ds03.command.Command;
import ds03.command.util.CommandUtils;
import ds03.command.util.ExceptionHandler;
import ds03.io.MessageIntegrityException;
import ds03.util.SecurityUtils;

public class BiddingClient implements Client {

	private static final int RECONNECT_INTERVAL = 3;
	private static final Logger LOG = Logger.getLogger(BiddingClient.class);
	private static final Command loginCommand;
	private static Map<String, Command> loggedOutCommandMap = new HashMap<String, Command>();
	private static Map<String, Command> loggedInCommandMap = new HashMap<String, Command>();
	private final BiddingUserContext context;
	private GetTimestampThread getTimeStampThread;
	private final ServerReconnectorTask reconnectorTask;
	private final ScheduledExecutorService schedulerService;

	private final Runnable shutdownHook;

	static {
		long waitTimeOut = RECONNECT_INTERVAL * 1000 + 500;
		Command getClientListCommand = new GetClientListCommand(waitTimeOut);
		loginCommand = new LoginCommand(waitTimeOut, getClientListCommand);
		Command logoutCommand = new LogoutCommand(waitTimeOut);
		Command defaulCommand = new DefaultBiddingCommand(waitTimeOut);

		loggedOutCommandMap.put("!login", loginCommand);
		loggedOutCommandMap.put("!logout", logoutCommand);
		loggedOutCommandMap.put("!list", defaulCommand);

		loggedInCommandMap.put("!login", loginCommand);
		loggedInCommandMap.put("!logout", logoutCommand);
		loggedInCommandMap.put("!list", defaulCommand);
		loggedInCommandMap.put("!create", defaulCommand);
		loggedInCommandMap.put("!bid", new BidCommand(waitTimeOut));
		loggedInCommandMap.put("!groupBid", defaulCommand);
		loggedInCommandMap.put("!confirm", defaulCommand);
		loggedInCommandMap.put("!getClientList", getClientListCommand);
	}

	public BiddingClient(ClientConsole out, String host, int tcpPort,
			int notificationPort) {
		try {
			context = new BiddingUserContextImpl(out, host, tcpPort,
					notificationPort);
			reconnectorTask = new ServerReconnectorTask(
					context, loginCommand);
			schedulerService = Executors.newScheduledThreadPool(1);
		} catch (Exception ex) {
			throw new RuntimeException("Could not connect to server", ex);
		}

		shutdownHook = new Runnable() {

			@Override
			public void run() {
				context.close();
				getTimeStampThread.kill();
				schedulerService.shutdownNow();
			}
		};

		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
	}

	public void run() {

		final BiddingUserContext con = context; /* avoid getfield opcode */
		schedulerService.scheduleAtFixedRate(reconnectorTask, 0,
				RECONNECT_INTERVAL, TimeUnit.SECONDS);

		while (true) {
			final String request = readRequest();

			try {
				if (!con.isLoggedIn()) {
					if (!CommandUtils.invokeCommand(request,
							loggedOutCommandMap, con)) {
						break;
					}
					
					if(con.isLoggedIn()){
						getTimeStampThread = new GetTimestampThread(con);
						getTimeStampThread.start();
					}
				} else {
					if (!CommandUtils.invokeCommand(request,
							loggedInCommandMap, con, new ExceptionHandler() {

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
					
					if(!con.isLoggedIn() && getTimeStampThread != null){
						getTimeStampThread.kill();
						getTimeStampThread = null;
					}
				}
			} catch (Exception e) {
				con.getOut().write("ERROR: Connection to server terminated");
				break;
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
		int notificationPort = 0;
		String pathToPublicKey = args[3];
		String pathToClientKeyDir = args[4];

		try {
			tcpPort = Integer.parseInt(args[1]);
			notificationPort = Integer.parseInt(args[2]);
		} catch (NumberFormatException ex) {
			LOG.error(ex);
			usage();
		}

		final Client client;

		try {
			client = new BiddingClient(ClientConsole.sio, host, tcpPort,
					notificationPort);
			SecurityUtils.init(notificationPort, pathToPublicKey,
					pathToClientKeyDir);
		} catch (Exception ex) {
			throw new RuntimeException("Could not instantiate client", ex);
		}

		client.run();
	}

	private static void usage() {
		System.out
				.println("Usage: "
						+ BiddingClient.class.getSimpleName()
						+ " <host> <tcpPort> <notificationPort> <pathToServerPublickey> <pathToClientKeysDir>");
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
