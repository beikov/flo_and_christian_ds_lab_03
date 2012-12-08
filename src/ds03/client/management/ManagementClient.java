package ds03.client.management;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import ds03.client.Client;
import ds03.client.management.command.AddStepCommand;
import ds03.client.management.command.AutoCommand;
import ds03.client.management.command.BillCommand;
import ds03.client.management.command.HideCommand;
import ds03.client.management.command.LoginCommand;
import ds03.client.management.command.LogoutCommand;
import ds03.client.management.command.PrintCommand;
import ds03.client.management.command.RemoveStepCommand;
import ds03.client.management.command.StepsCommand;
import ds03.client.management.command.SubscribeCommand;
import ds03.client.management.command.UnsubscribeCommand;
import ds03.client.util.ClientConsole;
import ds03.command.Command;
import ds03.command.util.CommandUtils;
import ds03.io.ProtocolException;
import ds03.util.RuntimeUtils;
import ds03.util.ServiceLocator;

public class ManagementClient implements Client {

	private final Map<String, Command> loggedInCommandMap = new HashMap<String, Command>();
	private final Map<String, Command> loggedOutCommandMap = new HashMap<String, Command>();
	private final ManagementUserContext context;

	public ManagementClient(InputStream in, PrintStream out) {
		this.context = new ManagementUserContextImpl(ClientConsole.fromStreams(
				out, in));
		assembleCommands();
	}

	public void run() {
		final ManagementUserContext con = context; /* avoid getfield opcode */

		while (true) {
			try {
				if (!CommandUtils.invokeCommand(readRequest(),
						con.isLoggedIn() ? loggedInCommandMap
								: loggedOutCommandMap, con)) {
					break;
				}
			} catch (ProtocolException ex) {
				break;
			}

		}
		RuntimeUtils.invokeShutdownHooks();
	}

	private String readRequest() {
		StringBuilder sb = new StringBuilder();

		if (context.isLoggedIn()) {
			sb.append(context.getUsername());

		}

		sb.append("> ");

		context.getOut().write(sb.toString());

		return context.getOut().read();
	}

	private void assembleCommands() {
		loggedOutCommandMap.put("!login", new LoginCommand());
		loggedOutCommandMap.put("!logout", new LogoutCommand());
		loggedOutCommandMap.put("!subscribe", new SubscribeCommand());
		loggedOutCommandMap.put("!unsubscribe", new UnsubscribeCommand());
		loggedOutCommandMap.put("!print", new PrintCommand());
		loggedOutCommandMap.put("!auto", new AutoCommand());
		loggedOutCommandMap.put("!hide", new HideCommand());

		loggedInCommandMap.put("!login", new LoginCommand());
		loggedInCommandMap.put("!logout", new LogoutCommand());
		loggedInCommandMap.put("!steps", new StepsCommand());
		loggedInCommandMap.put("!addStep", new AddStepCommand());
		loggedInCommandMap.put("!removeStep", new RemoveStepCommand());
		loggedInCommandMap.put("!bill", new BillCommand());
		loggedInCommandMap.put("!subscribe", new SubscribeCommand());
		loggedInCommandMap.put("!unsubscribe", new UnsubscribeCommand());
		loggedInCommandMap.put("!print", new PrintCommand());
		loggedInCommandMap.put("!auto", new AutoCommand());
		loggedInCommandMap.put("!hide", new HideCommand());
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			usage();
		}

		ServiceLocator.init(args[0], args[1]);
		new ManagementClient(System.in, System.out).run();
	}

	private static void usage() {
		System.err.println("Usage: " + ManagementClient.class.getSimpleName()
				+ " <analyticsServerBinding> <billingServerBinding>");
		System.exit(1);
	}
}
