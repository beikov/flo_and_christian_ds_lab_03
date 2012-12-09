package ds03.server;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import ds03.event.DisconnectedEvent;
import ds03.event.EventHandler;

public class ClientDispatcherThread extends Thread {

	/*
	 * We use concurrent hash map for performance and because there is no
	 * ConcurrentHashSet
	 */
	private final Map<ClientHandler, Object> clientHandlers = new ConcurrentHashMap<ClientHandler, Object>();
	private final ExecutorService threadPool;
	private final int port;
	private volatile boolean closed = false;
	private volatile boolean paused = true;
	private volatile ServerSocket serverSocket;
	
	public ClientDispatcherThread(int port, ExecutorService threadPool) {
		super();
		this.port = port;
		this.threadPool = threadPool;
		this.activate();
	}
	
	public void close() {
		if(closed) {
			return;
		}
		
		synchronized (this) {
			closed = true;
			deactivate();
		}
	}

	public void activate() {
		if (closed) {
			return;
		}
		if (paused) {
			synchronized (this) {
				if (paused) {
					try {
						serverSocket = new ServerSocket();
						serverSocket.bind(new InetSocketAddress(port));
						paused = false;
						notifyAll();
					} catch (Exception ex) {
						throw new RuntimeException(
								"Could not create server socket!", ex);
					}
				}
			}
		}
	}

	public void deactivate() {
		if (closed) {
			return;
		}

		if (!paused) {
			synchronized (this) {
				if (!paused) {
					paused = true;
					
					if (serverSocket != null) {
						try {
							serverSocket.close();
						} catch (Exception ex) {
							// Ignore
						}
					}
					
					try {
						final Iterator<Map.Entry<ClientHandler, Object>> iter = clientHandlers
								.entrySet().iterator();

						while (iter.hasNext()) {
							final ClientHandler handler = iter.next().getKey();
							try {
								handler.stop();
							} catch (Exception e) {
								
							}
							iter.remove();
						}
					} catch (Exception e) {
						
					}
				}
			}
		}
	}

	public void run() {
		while (!closed) {
			try {
				if(paused) {
					synchronized (this) {	
						while(paused) {
							try {
								wait();
							} catch (Exception e) {
								
							}
						}
					}
					
					continue;
				}
				
				final AuctionServerUserContextImpl connection = new AuctionServerUserContextImpl(
						serverSocket.accept());
				final ClientHandler handler = new ClientHandler(connection);

				clientHandlers.put(handler, new Object());

				connection
						.addCloseListener(new EventHandler<DisconnectedEvent>() {

							@Override
							public void handle(DisconnectedEvent event) {
								clientHandlers.remove(handler);
							}

						});

				threadPool.execute(handler);
			} catch (Exception ex) {
			}
		}
	}
}
