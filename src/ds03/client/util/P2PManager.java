package ds03.client.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;

public class P2PManager {

	private static final String MESSAGE_NAMESPACE = "defaultNamespace";
	private final Map<String, PipeManager> pipeAdvertisements = new ConcurrentHashMap<String, PipeManager>();
	private final Map<String, String> advertisementsForScan = new ConcurrentHashMap<String, String>();

	private final String name;
	private final NetworkManager manager;
	private final DiscoveryService discovery;
	private final PublisherThread publisherThread;
	private final ScannerThread scannerThread;

	private volatile boolean closed = false;

	private final long lifetime = 60000;
	private final long expiration = 60000;
	private final long waittime = 3000;

	static {
		LogManager.getLogManager().reset();
//		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
//
//		while (names.hasMoreElements()) {
//			Logger.getLogger(names.nextElement()).setLevel(Level.OFF);
//		}
	}

	public P2PManager(String name, int port) {
		try {
			manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, name,
					new File(new File(".cache"), name).toURI());
			// NetworkConfigurator configurator = manager.getConfigurator();
			// configurator.setTcpPort(port);
			// configurator.setTcpEnabled(true);
			// configurator.setTcpIncoming(true);
			// configurator.setTcpOutgoing(true);
			// configurator.setUseMulticast(true);
			manager.startNetwork();
			this.name = name;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		PeerGroup netPeerGroup = manager.getNetPeerGroup();

		discovery = netPeerGroup.getDiscoveryService();
		// discovery.addDiscoveryListener(new DiscoveryListener() {
		//
		// @Override
		// public void discoveryEvent(DiscoveryEvent event) {
		// Enumeration<Advertisement> ads =
		// event.getResponse().getAdvertisements();
		//
		// System.out.println("Discovered Advertisements:");
		//
		// while(ads.hasMoreElements()){
		// System.out.println(ads.nextElement().getID().toURI());
		// }
		//
		// System.out.println();
		// }
		// });
		publisherThread = new PublisherThread();
		scannerThread = new ScannerThread();

		publisherThread.start();
		scannerThread.start();

	}

	public void close() {
		if (!closed) {
			closed = true;
			publisherThread.interrupt();
			scannerThread.interrupt();

			for (PipeManager pipeManager : pipeAdvertisements.values()) {
				pipeManager.interrupt();
			}

			manager.stopNetwork();
		}
	}

	public void registerService(String id, RequestCallback callback) {
		PipeManager pipeManager = new PipeManager(id, callback);
		pipeAdvertisements.put(id, pipeManager);
		pipeManager.start();
	}

	public void scanService(String attribute, String value) {
		advertisementsForScan.put(attribute, value);
	}

	public Map<String, String> requestService(String attribute, String value,
			final String request, final RequestStopCondition predicate,
			long timeoutInMillis) {
		final Map<String, String> result = new ConcurrentHashMap<String, String>();
		final Object lock = new Object();
		final PipeMsgListener responseListener = new PipeMsgListener() {

			@Override
			public void pipeMsgEvent(PipeMsgEvent event) {
				MessageElement messageElement = event.getMessage()
						.getMessageElement(MESSAGE_NAMESPACE);

				try {
					String message = messageElement.toString();
					String[] part = message.split("\\s", 2);
					result.put(part[0], part[1]);

					if (predicate.shouldStop(result)) {
						synchronized (lock) {
							lock.notifyAll();
						}
					}
				} catch (Exception e) {

				}
			}
		};
		DiscoveryListener l = new DiscoveryListener() {

			@Override
			public void discoveryEvent(DiscoveryEvent event) {
				if (!predicate.shouldStop(result)) {
					Enumeration<Advertisement> advertisements = event
							.getResponse().getAdvertisements();
					JxtaBiDiPipe bidiPipe = null;

					while (!predicate.shouldStop(result)
							&& advertisements.hasMoreElements()) {

						Advertisement ad = advertisements.nextElement();

						if (!(ad instanceof PipeAdvertisement)) {
							continue;
						}

						try {
							bidiPipe = new JxtaBiDiPipe(
									manager.getNetPeerGroup(),
									(PipeAdvertisement) ad, responseListener);
							Message msg = new Message();
							msg.addMessageElement(new StringMessageElement(
									MESSAGE_NAMESPACE, request, null));
							bidiPipe.sendMessage(msg);
						} catch (IOException e) {

						}
					}
				}
			}
		};

		discovery.getRemoteAdvertisements(null, DiscoveryService.ADV,
				attribute, value, 1, l);

		try {
			final long enterTime = System.currentTimeMillis();

			synchronized (lock) {
				while (!predicate.shouldStop(result)
						&& System.currentTimeMillis() - enterTime < timeoutInMillis) {
					lock.wait(timeoutInMillis);
				}
			}
		} catch (Exception e) {

		}

		return result;
	}

	private PipeAdvertisement getPipeAdvertisement(String serviceName) {
		PipeAdvertisement advertisement = (PipeAdvertisement) AdvertisementFactory
				.newAdvertisement(PipeAdvertisement.getAdvertisementType());

		advertisement.setPipeID(IDFactory.newPipeID(
				PeerGroupID.defaultNetPeerGroupID, name.getBytes()));
		advertisement.setType(PipeService.UnicastType);
		advertisement.setName(serviceName);
		return advertisement;

	}

	private class PublisherThread extends Thread {
		public void run() {
			try {
				while (!Thread.interrupted() && !closed) {
					for (PipeManager pipeManager : pipeAdvertisements.values()) {
						try {
							discovery.publish(pipeManager.pipeAdvertisement,
									lifetime, expiration);
							discovery.remotePublish(
									pipeManager.pipeAdvertisement, expiration);
						} catch (Exception e) {

						}
					}

					try {
						Thread.sleep(waittime);
					} catch (Exception e) {

					}
				}
			} catch (Exception e) {

			}
		}
	}

	private class ScannerThread extends Thread {
		public void run() {
			try {
				while (!Thread.interrupted() && !closed) {
					try {
						for (Map.Entry<String, String> entry : advertisementsForScan
								.entrySet()) {
							discovery.getRemoteAdvertisements(null,
									DiscoveryService.ADV, entry.getKey(),
									entry.getValue(), 1, null);
						}
					} catch (Exception e) {

					}

					try {
						Thread.sleep(waittime);
					} catch (Exception e) {

					}
				}
			} catch (Exception e) {

			}
		}
	}

	private class PipeManager extends Thread {

		private final PipeAdvertisement pipeAdvertisement;
		private final JxtaServerPipe serverPipe;
		private final RequestCallback callback;

		public PipeManager(String id, RequestCallback callback) {
			this.pipeAdvertisement = getPipeAdvertisement(id);
			this.callback = callback;

			try {
				serverPipe = new JxtaServerPipe(manager.getNetPeerGroup(),
						pipeAdvertisement);
				serverPipe.setPipeTimeout(0);

				discovery.publish(pipeAdvertisement);
				discovery.remotePublish(pipeAdvertisement);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void run() {
			try {
				JxtaBiDiPipe newBidiPipe;

				while (!Thread.interrupted()
						&& (newBidiPipe = serverPipe.accept()) != null) {
					final JxtaBiDiPipe bidiPipe = newBidiPipe;

					newBidiPipe.setMessageListener(new PipeMsgListener() {

						@Override
						public void pipeMsgEvent(PipeMsgEvent event) {
							MessageElement messageElement = event.getMessage()
									.getMessageElement(MESSAGE_NAMESPACE);

							try {
								String message = messageElement.toString();
								String response = callback.service(message);
								
								Message msg = new Message();
								msg.addMessageElement(new StringMessageElement(
										MESSAGE_NAMESPACE, response, null));
								bidiPipe.sendMessage(msg);
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								try {
									bidiPipe.close();
								} catch (IOException e) {
									// don't care
								}
							}
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (serverPipe != null) {
					try {
						serverPipe.close();
					} catch (IOException e) {
						// don't care
					}
				}
			}
		}

	}

}
