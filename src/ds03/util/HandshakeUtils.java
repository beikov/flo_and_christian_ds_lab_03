package ds03.util;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

import ds03.command.Context;
import ds03.io.AESAuctionProtocolChannel;
import ds03.io.AuctionProtocolChannel;
import ds03.io.Base64AuctionProtocolChannel;
import ds03.io.ClientHMACAuctionProtocolChannel;
import ds03.io.ServerHMACAuctionProtocolChannel;

public class HandshakeUtils {

	private static final Pattern handshakeOkPattern = Pattern
			.compile("!ok [a-zA-Z0-9/+]{43}= [a-zA-Z0-9/+]{43}= [a-zA-Z0-9/+]{43}= [a-zA-Z0-9/+]{22}==");

	public static AuctionProtocolChannel startHandshake(Context context,
			String username, PrivateKey privateKey, Key hmacKey) {
		try {

			/*
			 * 1st step of the handshake: Send handshake request encrypted with
			 * server public key
			 */
			String clientChallenge = SecurityUtils.getRandomBytes(32);
			String handshakeRequest = SecurityUtils
					.encryptRsa(
							new StringBuilder("!login ")
									.append(username)
									.append(" ")
									.append(context.getNotificationEndpoint()
											.getPort()).append(" ")
									.append(clientChallenge).toString(),
							SecurityUtils.getServerPublicKey());

			context.getChannel().write(handshakeRequest);

			/* 2nd step of the handshake: Read challenge with client private key */
			String answer = SecurityUtils.decryptRsa(context.getChannel()
					.read(), privateKey);

			if (!handshakeOkPattern.matcher(answer).matches()) {
				throw new RuntimeException("Mismatching pattern");
			}

			String[] answerParts = answer.split("\\s");

			if (!answerParts[1].equals(clientChallenge)) {
				throw new RuntimeException("Client Challenge");
			}

			String serverChallenge = answerParts[2];

			Key secretKey = new SecretKeySpec(Base64.decode(answerParts[3]
					.getBytes()), "AES");

			AuctionProtocolChannel encryptedChannel = new ClientHMACAuctionProtocolChannel(
					new AESAuctionProtocolChannel(
							new Base64AuctionProtocolChannel(
									context.getChannel()), secretKey,
							Base64.decode(answerParts[4])), hmacKey);

			/* 3rd step of handshake: Write challenge back encrypted with AES */
			encryptedChannel.write(serverChallenge);

			return encryptedChannel;
		} catch (Exception e) {
			throw new RuntimeException("Handshake failed", e);
		}
	}

	public static AuctionProtocolChannel receiveHandshake(Context context,
			String clientChallenge, PublicKey publicKey, Key hmacKey) {

		try {
			/*
			 * 1st step of handshake: Read handshake request with server private
			 * key
			 */
			String serverChallenge = SecurityUtils.getRandomBytes(32);
			String iv = SecurityUtils.getRandomBytes(16);
			SecretKey key = SecurityUtils.generateAESKey();
			String encodedKey = new String(Base64.encode(key.getEncoded()));

			/*
			 * 2nd step of handshake: Send client and server challenge encrypted
			 * with client public key
			 */
			String response = SecurityUtils.encryptRsa(
					new StringBuilder().append("!ok ").append(clientChallenge)
							.append(" ").append(serverChallenge).append(" ")
							.append(encodedKey).append(" ").append(iv)
							.toString(), publicKey);

			context.getChannel().write(response);

			AuctionProtocolChannel encryptedChannel = new ServerHMACAuctionProtocolChannel(
					new AESAuctionProtocolChannel(
							new Base64AuctionProtocolChannel(
									context.getChannel()), key,
							Base64.decode(iv)), hmacKey);

			/* 3rd step of handshake: Read server challenge with AES */
			String readChallenge = encryptedChannel.read();

			if (!serverChallenge.equals(readChallenge)) {
				throw new RuntimeException("Serverchallenge failed");
			}

			return encryptedChannel;
		} catch (Exception e) {
			throw new RuntimeException("Handshake failed", e);
		}

	}
}
