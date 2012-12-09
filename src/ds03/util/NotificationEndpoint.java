package ds03.util;

public class NotificationEndpoint {

	private final String ipAddress;
	private final int port;

	public NotificationEndpoint(String ipAddress, int port) {
		super();
		this.ipAddress = ipAddress;
		this.port = port;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public int getPort() {
		return port;
	}

	public String toString() {
		return ipAddress + ":" + port;
	}

}
