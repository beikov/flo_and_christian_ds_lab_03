package ds03.io;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

public class AESAuctionProtocolChannel extends AuctionProtocolChannelDecorator {

	private final Cipher encryptCipher;
	private final Cipher decryptCipher;

	public AESAuctionProtocolChannel(AuctionProtocolChannel delegate,
			Key secretKey, byte[] iv) {
		super(delegate);

		try {

			encryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
			encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey,
					new IvParameterSpec(iv));

			decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
			decryptCipher.init(Cipher.DECRYPT_MODE, secretKey,
					new IvParameterSpec(iv));
		} catch (Exception e) {
			throw new RuntimeException("Unable to create ciphers", e);
		}
	}

	@Override
	public void write(String response) {
		try {
			super.write(new String(encryptCipher.doFinal(response.getBytes())));
		} catch (Exception e) {
			throw new ProtocolException("Protocol error", e);
		}
	}

	@Override
	public String read() {
		String read = super.read();

		if (read == null) {
			return null;
		} else if (read.isEmpty()) {
			return read;
		}

		try {
			return new String(decryptCipher.doFinal(read.getBytes()));
		} catch (Exception e) {
			throw new ProtocolException("Protocol error", e);
		}

	}

}
