package ds03.client.bidding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ds03.client.util.ClientConsole;
import ds03.command.Command;
import ds03.io.AuctionProtocolChannel;
import ds03.io.AuctionProtocolChannelImpl;
import ds03.model.SingleBid;
import ds03.model.TimestampMessage;
import ds03.util.NotificationEndpoint;

public class ServerReconnectorTask implements Runnable {

	private static final int RECONNECT_INTERVAL = 3;
	private final BiddingUserContext context;
	private final ScheduledExecutorService schedulerService;
	private final Command loginCommand;

	public ServerReconnectorTask(BiddingUserContext context,
			ScheduledExecutorService schedulerService, Command loginCommand) {
		super();
		this.context = context;
		this.schedulerService = schedulerService;
		this.loginCommand = loginCommand;
	}

	@Override
	public void run() {
		if (!context.isClosed() && context.getChannel().isClosed()) {
			/* try reconnect */
			try {
				Socket socket = new Socket(context.getServerHost(),
						context.getServerPort());
				AuctionProtocolChannel channel = new AuctionProtocolChannelImpl(
						socket);
				context.setChannel(channel);
			} catch (Exception ex) {
				// Connect maybe failed, but leave old channel as is
			}

			if (context.getChannel().isClosed()) {
				/*
				 * Lookup open bids and send getTimestamp messages to clients
				 * for evidence
				 */
				Map<SingleBid, Set<TimestampMessage>> queuedSingleBids = context
						.getQueuedSingleBids();
				Iterator<Map.Entry<SingleBid, Set<TimestampMessage>>> iter = queuedSingleBids
						.entrySet().iterator();

				while (iter.hasNext()) {
					Map.Entry<SingleBid, Set<TimestampMessage>> entry = iter
							.next();
					final SingleBid singleBid = entry.getKey();
					final Set<TimestampMessage> timestampMessages = entry
							.getValue();

					synchronized (singleBid) {
						if (timestampMessages.size() != 2) {
							/* Ask clients etc... */
							Iterator<Map.Entry<String, NotificationEndpoint>> clientIter = context
									.getClients().entrySet().iterator();

							while (clientIter.hasNext()
									&& timestampMessages.size() != 2) {
								Map.Entry<String, NotificationEndpoint> clientEntry = clientIter
										.next();

								if (context.getUsername().equals(
										clientEntry.getKey())) {
									continue;
								}

								if (timestampMessages
										.contains(new TimestampMessage(null,
												clientEntry.getKey()))) {
									/* This client already verified that bid */
									continue;
								}

								TimestampMessage message = getTimestamp(
										singleBid, clientEntry.getKey(),
										clientEntry.getValue());

								if (message != null) {
									timestampMessages.add(message);
								}
							}

							if (timestampMessages.size() != 2) {
								/*
								 * What shall we do with the drunken sailor? =>
								 * we kill him :D
								 */
								iter.remove();
							}
						}
					}
				}

				schedulerService.scheduleAtFixedRate(this, 0,
						RECONNECT_INTERVAL, TimeUnit.SECONDS);
				return;
			}

			if (!tryLogin()) {
				return;
			}

			try {
				List<SingleBid> bidsToSend = new ArrayList<SingleBid>();
				Map<SingleBid, Set<TimestampMessage>> queuedSingleBids = context
						.getQueuedSingleBids();
				List<StringBuilder> sbList = new ArrayList<StringBuilder>();
				Iterator<Map.Entry<SingleBid, Set<TimestampMessage>>> iter = queuedSingleBids
						.entrySet().iterator();
				boolean hasDeferedBids = false;

				while (iter.hasNext()) {
					StringBuilder sb = new StringBuilder();

					Map.Entry<SingleBid, Set<TimestampMessage>> entry = iter
							.next();
					final SingleBid singleBid = entry.getKey();
					final Set<TimestampMessage> timestampMessages = entry
							.getValue();

					synchronized (singleBid) {
						if (timestampMessages.size() == 2) {
							sbList.add(sb);

							sb.append("!signedBid ");
							// send correctly formatted command
							sb.append(singleBid.getAuctionId()).append(" ")
									.append(singleBid.getAmount());

							Iterator<TimestampMessage> timestampMsgIter = timestampMessages
									.iterator();

							while (timestampMsgIter.hasNext()) {
								sb.append(" ").append(
										timestampMsgIter.next()
												.getSignedBidFormat());
							}

							// remove at the end when sent to server
							bidsToSend.add(singleBid);
							hasDeferedBids = true;
						}
					}
				}

				if (hasDeferedBids) {
					synchronized (context) {
						for (int i = 0; i < bidsToSend.size(); i++) {
							context.getChannel()
									.write(sbList.get(i).toString());
							String answer = context.getChannel().read();

							context.getOut().writeln("");
							context.getOut().writeln(answer);
							context.getOut().write(context.getUsername() + "> ");
							
							queuedSingleBids.remove(bidsToSend.get(i));
						}
					}
				}
			} catch (Exception ex) {

			}
		}
	}

	public boolean tryLogin() {
		try {
			loginCommand.execute(new BiddingUserContextDecorator(context) {

				@Override
				public ClientConsole getOut() {
					return new ClientConsole() {

						@Override
						public void writeln(String s) {

						}

						@Override
						public void write(String s) {

						}

						@Override
						public String read() {
							return null;
						}
					};
				}

				@Override
				public boolean isLoggedIn() {
					return false;
				}

				@Override
				public void setClients(Map<String, NotificationEndpoint> clients) {
					/*
					 * This will be set when the automatic re-login is
					 * successful which is inappropriate since probably no other
					 * users are logged in at this point
					 */
				}

			}, new String[] { "!login", context.getUsername() });
		} catch (Exception ex) {
			if (context.getChannel().isClosed()) {
				schedulerService.scheduleAtFixedRate(this, 0,
						RECONNECT_INTERVAL, TimeUnit.SECONDS);
				return false;
			} else {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ex.printStackTrace(new PrintStream(baos));
				context.getOut().writeln(baos.toString());
				context.close();
				return false;
			}
		}

		return true;
	}

	public TimestampMessage getTimestamp(SingleBid singleBid, String username,
			NotificationEndpoint notificationEndpoint) {
		Socket socket = null;

		try {
			socket = new Socket(notificationEndpoint.getIpAddress(),
					notificationEndpoint.getPort());
			AuctionProtocolChannel channel = new AuctionProtocolChannelImpl(
					socket);
			StringBuilder sb = new StringBuilder();
			sb.append("!getTimestamp ").append(singleBid.getAuctionId())
					.append(" ").append(singleBid.getAmount());
			channel.write(sb.toString());
			return new TimestampMessage(channel.read(), username);
		} catch (Exception ex) {

		} finally {

			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}

		return null;
	}

}
