package ds03.server;

import java.net.Socket;

import ds03.client.util.ClientConsole;
import ds03.event.DisconnectedEvent;
import ds03.event.EventBus;
import ds03.event.EventHandler;
import ds03.event.LogoutEvent;
import ds03.event.handler.DefaultEventHandler;
import ds03.io.AuctionProtocolChannel;
import ds03.io.AuctionProtocolChannelDecorator;
import ds03.io.AuctionProtocolChannelImpl;

public class AuctionServerUserContextImpl implements AuctionServerUserContext {

	private final EventBus<DisconnectedEvent> onClose;
	private final EventBus<LogoutEvent> onLogout;
	private final Socket tcpSocket;
	private AuctionProtocolChannel channel;
	private String username;

	public AuctionServerUserContextImpl(Socket tcpSocket) {
		this.onClose = new EventBus<DisconnectedEvent>();
		this.onLogout = new EventBus<LogoutEvent>();
		this.tcpSocket = tcpSocket;

		try {
			this.channel = new AuctionProtocolChannelImpl(
					tcpSocket.getOutputStream(), tcpSocket.getInputStream());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public ClientConsole getOut() {
		return ClientConsole.sio;
	}

	@Override
	public AuctionProtocolChannel getChannel() {
		return channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.AuctionServerUserContext#getUsername()
	 */
	@Override
	public String getUsername() {
		return username;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.AuctionServerUserContext#login(java.lang.String)
	 */
	@Override
	public boolean login(final String username, final String password) {
		if (username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Invalid username");
		}

		if (isLoggedIn()) {
			throw new IllegalStateException("Already logged in");
		}

		try {
			this.username = username;

			/* Make sure the user is logged out when the connection is closed */
			addCloseListener(new EventHandler<DisconnectedEvent>() {
				@Override
				public void handle(DisconnectedEvent event) {
					AuctionServerUserContextImpl.this.logout();
					DefaultEventHandler.INSTANCE.handle(event);
				}
			});
			return true;
		} catch (Exception ex) {
			throw new IllegalArgumentException("Could not connect to client",
					ex);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.AuctionServerUserContext#logout()
	 */
	@Override
	public void logout() {
		if (isLoggedIn()) {
			AuctionProtocolChannel channel = this.channel;

			while (channel instanceof AuctionProtocolChannelDecorator) {
				channel = ((AuctionProtocolChannelDecorator) channel)
						.getDelegate();
			}

			this.channel = channel;

			Throwable t = null;

			try {
				onLogout.notify(new LogoutEvent(username));
			} catch (Throwable ex) {
				t = ex;
			}

			/*
			 * Always remove the handlers, otherwise we will create a memory
			 * leak
			 */
			onLogout.removeHandlers();
			username = null;

			if (t != null) {
				/* Checked exceptions are not possible */
				if (t instanceof RuntimeException) {
					throw (RuntimeException) t;
				} else if (t instanceof Error) {
					throw (Error) t;
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.AuctionServerUserContext#isLoggedIn()
	 */
	@Override
	public boolean isLoggedIn() {
		return username != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.AuctionServerUserContext#addLogoutListener(ds03.event.
	 * EventHandler)
	 */
	@Override
	public void addLogoutListener(EventHandler<LogoutEvent> handler) {
		onLogout.addHandler(handler);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ds03.server.AuctionServerUserContext#addCloseListener(ds03.event.EventHandler
	 * )
	 */
	@Override
	public void addCloseListener(EventHandler<DisconnectedEvent> handler) {
		onClose.addHandler(handler);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.AuctionServerUserContext#close()
	 */
	@Override
	public void close() {
		if (!tcpSocket.isClosed()) {
			Throwable t = null;

			if (username != null) {
				try {
					onClose.notify(new DisconnectedEvent(username));
				} catch (Throwable ex) {
					t = ex;
				}

				username = null;
			}

			/*
			 * Always remove the handlers, otherwise we will create a memory
			 * leak
			 */
			onClose.removeHandlers();

			try {
				tcpSocket.close();
			} catch (Exception ex) {
				// Ignore
			}

			if (t != null) {
				/* Checked exceptions are not possible */
				if (t instanceof RuntimeException) {
					throw (RuntimeException) t;
				} else if (t instanceof Error) {
					throw (Error) t;
				}
			}
		}
	}

	@Override
	public void setChannel(AuctionProtocolChannel channel) {
		this.channel = channel;

	}
}
