package ds03.event.handler;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import ds03.event.Event;
import ds03.event.EventHandler;
import ds03.server.service.AnalyticsService;
import ds03.util.ServiceLocator;

public abstract class AbstractEventHandler<T extends Event> implements
		EventHandler<T> {

	private static final Logger LOG = Logger
			.getLogger(AbstractEventHandler.class);

	@Override
	public void handle(T event) {
		try {
			final AnalyticsService service = ServiceLocator.INSTANCE
					.getAnalyticsService();

			if (service == null) {
				LOG.warn("AnalyticsService is not reachable!");
			}

			service.processEvent(event);
		} catch (RemoteException e) {
			LOG.warn("AnalyticsService is not reachable!");
		}

	}
}
