package ds03.client.management.command;

import java.rmi.RemoteException;

import ds03.client.management.ManagementUserContext;
import ds03.event.Event;
import ds03.event.EventCallback;
import ds03.util.RegistryUtils;
import ds03.util.ServiceLocator;

public class SubscribeCommand extends AbstractManagementCommand {

	@Override
	public void execute(final ManagementUserContext context, String[] args) {
		if (args.length != 1) {
			throw new RuntimeException("Usage: !subscribe <filterRegex>");
		}

		EventCallback ec = RegistryUtils.exportObject(new EventCallback() {

			@Override
			public void handle(Event event) {
				context.addEvent(event);
			}
		});

		try {
			String subscriptionID = ServiceLocator.INSTANCE
					.getAnalyticsService().subscribe(args[0], ec);
			System.out.println("Created subscription with ID " + subscriptionID
					+ " for events using filter '" + args[0] + "'");
		} catch (RemoteException e) {
		}
	}

}
