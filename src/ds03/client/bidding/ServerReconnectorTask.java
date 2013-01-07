package ds03.client.bidding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ds03.client.util.ClientConsole;
import ds03.client.util.P2PManager;
import ds03.client.util.RequestStopCondition;
import ds03.command.Command;
import ds03.io.AuctionProtocolChannel;
import ds03.io.AuctionProtocolChannelImpl;
import ds03.model.SingleBid;
import ds03.model.TimestampMessage;
import ds03.util.NotificationEndpoint;

public class ServerReconnectorTask implements Runnable {

	private final BiddingUserContext context;
	private final Command loginCommand;

	public ServerReconnectorTask(BiddingUserContext context,
			Command loginCommand) {
		super();
		this.context = context;
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

				synchronized (context) {
					context.setChannel(channel);
					tryLogin();
					context.notifyAll();
				}
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
							tryGetTimestampMessagesViaP2P(singleBid,
									timestampMessages);

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

				return;
			}

			if (context.isClosed()) {
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
							context.getOut()
									.write(context.getUsername() + "> ");

							queuedSingleBids.remove(bidsToSend.get(i));
						}
					}
				}
			} catch (Exception ex) {

			}
		}
	}

	private void tryGetTimestampMessagesViaP2P(final SingleBid singleBid,
			final Set<TimestampMessage> timestampMessages) {
		Map<String, String> results = context.getP2PManager().requestService("Name", "getTimeStampMessage", getTimeStampRequestMessage(singleBid), new RequestStopCondition() {
			
			@Override
			public boolean shouldStop(Map<String, String> results) {
				Iterator<Map.Entry<String, String>> it = results.entrySet().iterator();
				
				while(it.hasNext()) {
					Map.Entry<String, String> entry = it.next();
					if(timestampMessages
							.contains(new TimestampMessage(null,
									entry.getKey()))) {
						results.remove(entry.getKey());
					}
				}
				
				return results.size() + timestampMessages.size() >= 2;
			}
		}, 10000);
		Iterator<Map.Entry<String, String>> it = results.entrySet().iterator();
		
		while(it.hasNext()) {
			Map.Entry<String, String> entry = it.next();
			timestampMessages.add(new TimestampMessage(entry.getValue(), entry.getKey()));
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
			
			channel.write(getTimeStampRequestMessage(singleBid));
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

	private String getTimeStampRequestMessage(SingleBid singleBid) {
		StringBuilder sb = new StringBuilder();
		sb.append("!getTimestamp ").append(singleBid.getAuctionId())
				.append(" ").append(singleBid.getAmount());
		return sb.toString();
	}

}
