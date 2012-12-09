package ds03.server.exception;

public class RejectedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RejectedException() {
		super();
	}

	public RejectedException(String message, Throwable cause) {
		super(message, cause);
	}

	public RejectedException(String message) {
		super(message);
	}

	public RejectedException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getMessage() {
		return "!rejected " + super.getMessage();
	}

}
