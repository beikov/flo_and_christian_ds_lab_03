package ds03.client.bidding.command;

import java.io.File;
import java.security.Key;
import java.security.PrivateKey;
import java.util.Map;

import org.bouncycastle.openssl.PasswordFinder;

import ds03.client.bidding.BiddingUserContext;
import ds03.client.bidding.BiddingUserContextDecorator;
import ds03.client.util.ClientConsole;
import ds03.command.Command;
import ds03.io.AuctionProtocolChannel;
import ds03.io.ProtocolException;
import ds03.util.HandshakeUtils;
import ds03.util.NotificationEndpoint;
import ds03.util.SecurityUtils;

public class LoginCommand extends AbstractBiddingCommand {

	private final Command getClientListCommand;

	public LoginCommand(Command getClientListCommand) {
		this.getClientListCommand = getClientListCommand;
	}

	@Override
	public void execute(final BiddingUserContext context, String[] args) {
		if (args.length != 2) {
			throw new RuntimeException("ERROR: Invalid parameters");
		}
		if (context.isLoggedIn()) {
			throw new RuntimeException("ERROR: Already logged in");
		}

		File clientPrivateKeyFile = new File(
				SecurityUtils.getPathToClientKeyDir(), args[1].toLowerCase()
						+ ".pem");

		if (!clientPrivateKeyFile.exists()) {
			throw new RuntimeException("ERROR: No private key for " + args[1]
					+ " exists.");
		}

		File clientSecretKeyFile = new File(
				SecurityUtils.getPathToClientKeyDir(), args[1].toLowerCase()
						+ ".key");

		if (!clientSecretKeyFile.exists()) {
			throw new RuntimeException("ERROR: No secret key for " + args[1]
					+ " exists.");
		}

		PrivateKey privateKey = SecurityUtils.getPrivateKey(
				clientPrivateKeyFile.getAbsolutePath(), new PasswordFinder() {
					@Override
					public char[] getPassword() {

						return context.getOut().prompt("Enter Passphrase: ")
								.toCharArray();
					}
				});

		if (privateKey == null) {
			throw new RuntimeException(
					"ERROR: Wrong user/password combination.");
		}

		Key secretKey = SecurityUtils.getSecretKey(clientSecretKeyFile
				.getAbsolutePath());

		if (secretKey == null) {
			throw new RuntimeException("ERROR: Secretkey could not be read.");
		}

		AuctionProtocolChannel newChannel = null;
		String result = null;

		synchronized (context) {
			try {
				newChannel = HandshakeUtils.startHandshake(context, args[1],
						privateKey, secretKey);
			} catch (ProtocolException e) {
				throw e;
			} catch (Exception e) {
				throw new ProtocolException();
			}

			result = newChannel.read();
			
			if(result == null) {
				//server becomes unavailable directly after log in
				throw new ProtocolException();
			}
		}

		context.setChannel(newChannel);

		if (result.contains("Successfully")) {
			context.login(args[1], "");
		}

		context.getOut().writeln(result);

		/* get client list */
		getClientListCommand.execute(new BiddingUserContextDecorator(context) {

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

		}, new String[0]);
	}

}
