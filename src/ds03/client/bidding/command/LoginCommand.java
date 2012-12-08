package ds03.client.bidding.command;

import java.io.File;
import java.security.Key;
import java.security.PrivateKey;

import ds03.client.bidding.BiddingUserContext;
import ds03.util.HandshakeUtils;
import ds03.util.SecurityUtils;

public class LoginCommand extends AbstractBiddingCommand {

	@Override
	public void execute(final BiddingUserContext context, String[] args) {
		if (args.length != 2) {
			throw new RuntimeException("ERROR: Invalid parameters");
		}
		if (context.isLoggedIn()) {
			throw new RuntimeException("ERROR: Already logged in");
		}

		File clientPrivateKeyFile = new File(SecurityUtils.getPathToClientKeyDir(),
				args[1].toLowerCase() + ".pem");

		if (!clientPrivateKeyFile.exists()) {
			throw new RuntimeException("ERROR: No private key for " + args[1]
					+ " exists.");
		}
		
		File clientSecretKeyFile = new File(SecurityUtils.getPathToClientKeyDir(),
				args[1].toLowerCase() + ".key");

		if (!clientSecretKeyFile.exists()) {
			throw new RuntimeException("ERROR: No secret key for " + args[1]
					+ " exists.");
		}
		
		PrivateKey privateKey = SecurityUtils.getPrivateKey(
				clientPrivateKeyFile.getAbsolutePath(),
				context.getOut().prompt("Enter Passphrase: "));

		if (privateKey == null) {
			throw new RuntimeException(
					"ERROR: Wrong user/password combination.");
		}
		
		Key secretKey = SecurityUtils.getSecretKey(clientSecretKeyFile.getAbsolutePath());
		
		if (secretKey == null) {
			throw new RuntimeException(
					"ERROR: Secretkey could not be read.");
		}

		context.setChannel(HandshakeUtils.startHandshake(context, args[1],
				privateKey, secretKey));

		final String result = context.getChannel().read();

		if (result.contains("Successfully")) {
			context.login(args[1], "");
		}

		context.getOut().writeln(result);
	}

}
