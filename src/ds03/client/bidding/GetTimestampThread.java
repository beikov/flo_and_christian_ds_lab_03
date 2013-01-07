package ds03.client.bidding;

import java.io.File;
import java.math.BigDecimal;
import java.security.PrivateKey;

import org.bouncycastle.openssl.PasswordFinder;

import ds03.client.util.RequestCallback;
import ds03.util.SecurityUtils;

public class GetTimestampThread extends Thread implements RequestCallback {
	private final BiddingUserContext context;
	private volatile boolean running = true;
	private Object lock = new Object();

	public GetTimestampThread(BiddingUserContext context) {
		super();
		this.context = context;
	}

	@Override
	public void run() {
		try {
			context.getP2PManager()
					.registerService("getTimeStampMessage", this);
			context.getP2PManager().scanService("Name", "getTimeStampMessage");

			synchronized (lock) {
				while (running) {
					lock.wait();
				}
			}
		} catch (Exception e) {

		} finally {
			context.getP2PManager().close();
		}
	}

	@Override
	public String service(String message) {
		if (!context.isLoggedIn()) {
			throw new RuntimeException("User not logged in.");
		}

		File clientPrivateKeyFile = new File(
				SecurityUtils.getPathToClientKeyDir(), context.getUsername()
						.toLowerCase() + ".pem");

		if (!clientPrivateKeyFile.exists()) {
			throw new RuntimeException("ERROR: No private key for "
					+ context.getUsername() + " exists.");
		}

		PrivateKey privateKey = SecurityUtils.getPrivateKey(
				clientPrivateKeyFile.getAbsolutePath(), new PasswordFinder() {
					@Override
					public char[] getPassword() {

						return context.getOut().prompt("Enter Passphrase: ")
								.toCharArray();
					}
				});

		final String[] requestParts = message.split("\\s");
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
		sb.append("!timestamp ").append(auctionId).append(" ").append(amount)
				.append(" ").append(System.currentTimeMillis());
		String response = sb.toString();
		String signature = SecurityUtils.createSignature(response, privateKey);
		return context.getUsername() + " " + response + " " + signature;
	}

	public void kill() {
		running = false;

		synchronized (lock) {
			lock.notifyAll();
		}

		context.getP2PManager().close();
	}
}
