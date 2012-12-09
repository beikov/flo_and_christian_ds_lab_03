package ds03.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import ds03.event.DisconnectedEvent;
import ds03.event.EventHandler;
import ds03.server.service.AuctionService;
import ds03.util.SecurityUtils;
import ds03.util.ServiceLocator;

public class AuctionServer {

	private static final int THREADS = 10000;

	public static void main(String[] args) {
		if (args.length != 5) {
			usage();
		}

		int port = 0;
		SecurityUtils.init(0, args[3], args[4]);

		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException ex) {
			ex.printStackTrace(System.err);
			usage();
		}

		ServiceLocator.init(args[1], args[2]);
		BufferedReader in = null;

		try {
			in = new BufferedReader(new InputStreamReader(System.in));
		} catch (Exception ex) {
			// This normally should not happen
		}

		final ExecutorService threadPool = Executors
				.newFixedThreadPool(THREADS);
		final ScheduledExecutorService schedulerService = Executors
				.newScheduledThreadPool(2);

		AuctionService.INSTANCE.setSchedulerService(schedulerService);
		

		/* The thread for accepting connections */
		final ClientDispatcherThread clientDispatcherThread = new ClientDispatcherThread(port, threadPool);
		clientDispatcherThread.start();

		final Runnable shutdownHook = new Runnable() {

			@Override
			public void run() {
				if(clientDispatcherThread != null) {
					clientDispatcherThread.close();
				}

				if (threadPool != null) {
					threadPool.shutdown();
				}

				schedulerService.shutdownNow();
			}
		};

		/* Also use runtime shutdown hook */
		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));

		try {
			// Requirement states that a simple enter hit should end the server
			String command = null;
			
			while(!"".equals((command = in.readLine()))){
				if("!pause".equals(command)) {
					clientDispatcherThread.deactivate();
				} else if("!resume".equals(command)) {
					clientDispatcherThread.activate();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

		shutdownHook.run();
	}

	private static void usage() {
		System.out
				.println("Usage: "
						+ AuctionServer.class.getSimpleName()
						+ " <tcpPort> <analyticsBindingName> <billingBindingName> <pathToServerPrivateKey> <pathToClientKeysDir>");
		System.exit(1);
	}
}
