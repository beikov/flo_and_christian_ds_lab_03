package ds03.server;

import ds03.server.service.impl.BillingServiceImpl;
import ds03.util.RegistryUtils;
import ds03.util.RuntimeUtils;

public class BillingServer {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			usage();
		}

		try {
			RegistryUtils.bindService(args[0], BillingServiceImpl.class);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		RuntimeUtils.waitForExitCommand();
	}

	private static void usage() {
		System.out.println("Usage: " + BillingServer.class.getSimpleName()
				+ " <billingServerBinding>");
		System.exit(1);
	}
}
