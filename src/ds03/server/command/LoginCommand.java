package ds03.server.command;

import java.io.File;
import java.security.Key;
import java.security.PublicKey;

import ds03.command.Command;
import ds03.command.Context;
import ds03.server.AuctionServerUserContext;
import ds03.server.service.UserService;
import ds03.util.HandshakeUtils;
import ds03.util.SecurityUtils;

public class LoginCommand implements Command {

	private final UserService userService;

	public LoginCommand(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void execute(Context context, String[] args) {
		String username = null;

		if (args.length > 0) {
			username = args[0];
		}

		if (context.isLoggedIn()) {
			context.getChannel().write("You have to log out first!");
		} else if (userService.login(username,
				(AuctionServerUserContext) context)) {
			try {
				/* get user public key */
				File clientPublicKeyFile = new File(SecurityUtils.getPathToClientKeyDir(),
						username.toLowerCase() + ".pub.pem");
				/* check if the public key exists */
				if (!clientPublicKeyFile.exists()) {
					throw new RuntimeException("ERROR: Could not find client public key");
				}
				
				File clientSecretKeyFile = new File(SecurityUtils.getPathToClientKeyDir(),
						username.toLowerCase() + ".key");

				if (!clientSecretKeyFile.exists()) {
					throw new RuntimeException("ERROR: No secret key for " + args[1]
							+ " exists.");
				}

				PublicKey publicKey = SecurityUtils.getPublicKey(clientPublicKeyFile
						.getAbsolutePath());
				
				if (publicKey == null) {
					throw new RuntimeException(
							"ERROR: Client public key could not be read.");
				}
				
				Key secretKey = SecurityUtils.getSecretKey(clientSecretKeyFile.getAbsolutePath());
				
				if (secretKey == null) {
					throw new RuntimeException(
							"ERROR: Secretkey could not be read.");
				}


				context.setChannel(HandshakeUtils.receiveHandshake(context,
						args[1], publicKey, secretKey));

				context.login(username, args[1]);
				context.getChannel().write(
						"Successfully logged in as " + username + "!");
			} catch (RuntimeException ex) {
				userService.logout(username);
				throw ex;
			} catch (Exception ex) {
				userService.logout(username);
				// don't care
			}
		} else {
			context.getChannel().write("Already logged in");
		}
	}
}
