package ds03.io;

public class MessageIntegrityException extends RuntimeException {

	public MessageIntegrityException() {
		super();
	}

	public MessageIntegrityException(String message, Throwable cause) {
		super(message, cause);
	}

	public MessageIntegrityException(String message) {
		super(message);
	}

	public MessageIntegrityException(Throwable cause) {
		super(cause);
	}

}
