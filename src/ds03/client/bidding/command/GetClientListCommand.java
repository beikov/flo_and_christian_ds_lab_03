package ds03.client.bidding.command;

import java.io.File;
import java.security.Key;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ds03.client.bidding.BiddingUserContext;
import ds03.io.ProtocolException;
import ds03.util.HandshakeUtils;
import ds03.util.NotificationEndpoint;
import ds03.util.SecurityUtils;

public class GetClientListCommand extends AbstractBiddingCommand {

	@Override
	public void execute(final BiddingUserContext context, String[] args) {
		String response = null;

		synchronized (context) {

			context.getChannel().write("!getClientList");

			try {
				response = context.getChannel().read();
			} catch (ProtocolException e) {

			}

			if (response == null) {
				context.getOut()
						.writeln(
								"Server currently not available. You can continue bidding.");
				return;
			}
		}

		Map<String, NotificationEndpoint> clients = new HashMap<String, NotificationEndpoint>();

		String[] rows = response.split("\n");

		for (String row : rows) {
			final String endpoint = row.substring(0, row.indexOf(" "));
			final String ipAddress = endpoint.substring(0,
					endpoint.indexOf(":"));
			final int port = Integer.parseInt(endpoint.substring(ipAddress
					.length() + 1));

			final NotificationEndpoint notificationEndpoint = new NotificationEndpoint(
					ipAddress, port);
			clients.put(row.substring(row.lastIndexOf(" ") + 1),
					notificationEndpoint);
		}

		context.setClients(clients);

		response = "Active Clients\n" + response;

		context.getOut().writeln(response);
	}

}
