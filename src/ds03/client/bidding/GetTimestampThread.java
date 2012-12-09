package ds03.client.bidding;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.bouncycastle.openssl.PasswordFinder;

import ds03.io.AuctionProtocolChannel;
import ds03.io.AuctionProtocolChannelImpl;
import ds03.io.ClientSignatureAuctionProtocolChannel;
import ds03.util.SecurityUtils;

public class GetTimestampThread extends Thread {
	private final BiddingUserContext context;
	private volatile ServerSocket serverSocket = null;
	private volatile boolean running = true;

	public GetTimestampThread(BiddingUserContext context) {
		super();
		this.context = context;
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(context
					.getNotificationEndpoint().getPort()));

			while (running) {
				Socket socket = null;

				try {
					socket = serverSocket.accept();

					if (!context.isLoggedIn()) {
						throw new RuntimeException("User not logged in.");
					}

					File clientPrivateKeyFile = new File(
							SecurityUtils.getPathToClientKeyDir(), context
									.getUsername().toLowerCase() + ".pem");

					if (!clientPrivateKeyFile.exists()) {
						throw new RuntimeException("ERROR: No private key for "
								+ context.getUsername() + " exists.");
					}
					final AuctionProtocolChannel channel = new ClientSignatureAuctionProtocolChannel(
							new AuctionProtocolChannelImpl(socket),
							SecurityUtils.getPrivateKey(
									clientPrivateKeyFile.getAbsolutePath(),
									new PasswordFinder() {
										@Override
										public char[] getPassword() {

											return context
													.getOut()
													.prompt("Enter Passphrase: ")
													.toCharArray();
										}
									}));

					final String request = channel.read();
					final String[] requestParts = request.split("\\s");
					long auctionId = -1;
					BigDecimal amount = null;

					if (requestParts.length != 3
							|| !"!getTimestamp".equals(requestParts[0])) {
						throw new RuntimeException("Invalid request");
					}

					try {
						auctionId = Long.parseLong(requestParts[1]);
						amount = new BigDecimal(requestParts[2]);
					} catch (Exception e) {
						throw new RuntimeException("Invalid params");
					}

					StringBuilder sb = new StringBuilder();
					sb.append("!timestamp ").append(auctionId).append(" ")
							.append(amount).append(" ")
							.append(System.currentTimeMillis());
					channel.write(sb.toString());
				} catch (Exception e) {
				} finally {
					if (socket != null) {
						try {
							socket.close();
						} catch (IOException e) {

						}
					}
				}

			}
		} catch (Exception e) {

		} finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {

				}
			}
		}
	}

	public void kill() {
		running = false;

		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {

			}
		}
	}
}
