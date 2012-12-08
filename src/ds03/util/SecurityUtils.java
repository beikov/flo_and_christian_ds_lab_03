package ds03.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

public class SecurityUtils {

	private static int udpPort;
	private static String pathToPublicKey;
	private static String pathToClientKeyDir;

	private static PrivateKey serverPrivateKey;
	private static PublicKey serverPublicKey;

	public static void init(int udpPort, String pathToPublicKey,
			String pathToClientKeyDir) {
		SecurityUtils.udpPort = udpPort;
		SecurityUtils.pathToClientKeyDir = pathToClientKeyDir;
		SecurityUtils.pathToPublicKey = pathToPublicKey;
		Security.addProvider(new BouncyCastleProvider());
	}

	public static int getUdpPort() {
		return udpPort;
	}

	public static String getPathToServerKey() {
		return pathToPublicKey;
	}

	public static PublicKey getPublicKey(String pathToPublicKey) {
		PEMReader in = null;

		try {
			in = new PEMReader(new FileReader(pathToPublicKey));
			return (PublicKey) in.readObject();
		} catch (Exception e) {
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}

	}

	public static PrivateKey getPrivateKey(String pathToPrivateKey,
			final String passphrase) {
		PEMReader in = null;

		try {
			in = new PEMReader(new FileReader(pathToPrivateKey),
					new PasswordFinder() {
						@Override
						public char[] getPassword() {

							return passphrase.toCharArray();
						}
					});

			KeyPair keyPair = (KeyPair) in.readObject();
			return keyPair.getPrivate();
		} catch (Exception e) {
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}

	}

	public static String getRandomBytes(int bytes) {
		SecureRandom rs = new SecureRandom();
		final byte[] number = new byte[bytes];
		rs.nextBytes(number);

		return new String(Base64.encode(number));
	}

	public static SecretKey generateAESKey() {
		try {
			KeyGenerator generator = KeyGenerator.getInstance("AES");
			generator.init(256);
			return generator.generateKey();
		} catch (Exception e) {
			return null;
		}
	}

	public static synchronized PublicKey getServerPublicKey() {
		if (serverPublicKey == null) {
			serverPublicKey = getPublicKey(pathToPublicKey);

			if (serverPublicKey == null) {
				throw new RuntimeException("Key not found.");
			}
		}

		return serverPublicKey;
	}

	public static synchronized PrivateKey getServerPrivateKey() {
		if (serverPrivateKey == null) {
			serverPrivateKey = getPrivateKey(pathToPublicKey, "23456");

			if (serverPrivateKey == null) {
				throw new RuntimeException("Key not found.");
			}
		}

		return serverPrivateKey;
	}

	public static String decryptRsa(String cipherText, Key key)
			throws Exception {
		Cipher cipher = Cipher
				.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
		cipher.init(Cipher.DECRYPT_MODE, key);
		return new String(cipher.doFinal(Base64.decode(cipherText)));
	}

	public static String encryptRsa(String plainText, Key key) throws Exception {
		Cipher cipher = Cipher
				.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");

		cipher.init(Cipher.ENCRYPT_MODE, key);
		return new String(Base64.encode(cipher.doFinal(plainText.getBytes())));
	}

	public static String getPathToClientKeyDir() {
		return pathToClientKeyDir;
	}

	public static String createHmac(String response, Key key) {
		try {
			Mac hMac = Mac.getInstance("HmacSHA256");
			hMac.init(key);

			return new String(Base64.encode(hMac.doFinal(response.getBytes())));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 
	 * @param text
	 * @param key
	 * @param hmac
	 * @return
	 */
	public static boolean verifyHmac(String text, Key key, String hmac) {
		byte[] hmacBytes = Base64.decode(hmac);
		String calculatedHmac = createHmac(text, key);
		return calculatedHmac != null
				&& MessageDigest.isEqual(hmacBytes, Base64.decode(calculatedHmac));
	}

	public static Key getSecretKey(String pathToSecretKey) {
		byte[] keyBytes = new byte[1024];
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(pathToSecretKey);
			fis.read(keyBytes);
			fis.close();
			byte[] input = Hex.decode(keyBytes);
			return new SecretKeySpec(input, "HmacSHA256");
		} catch (Exception e) {
			return null;
		} finally {
			if(fis != null){
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	
	}

}
