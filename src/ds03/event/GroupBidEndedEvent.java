package ds03.event;

public class GroupBidEndedEvent {

	private final boolean success;
	private final String message;
	
	public GroupBidEndedEvent(boolean success, String message) {
		super();
		this.success = success;
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}
}
