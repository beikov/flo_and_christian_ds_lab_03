package ds03.client.management.command;

import java.rmi.RemoteException;

import ds03.client.management.ManagementUserContext;
import ds03.util.ServiceLocator;

public class UnsubscribeCommand extends AbstractManagementCommand {

	@Override
	public void execute(ManagementUserContext context, String[] args) {
		if (args.length != 1) {
			throw new RuntimeException("Usage: !unsubscribe <subscriptionID>");
		}

		try {
			ServiceLocator.INSTANCE.getAnalyticsService().unsubscribe(args[0]);
			System.out.println("subscription " + args[0] + " terminated");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
	}

}
