package ds03.server;

import ds03.server.service.impl.AnalyticsServiceImpl;
import ds03.util.RegistryUtils;
import ds03.util.RuntimeUtils;

public class AnalyticsServer {

	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
		}

		try {
			RegistryUtils.bindService(args[0], AnalyticsServiceImpl.class);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		RuntimeUtils.waitForExitCommand();
	}

	private static void usage() {
		System.out.println("Usage: " + AnalyticsServer.class.getSimpleName()
				+ " <analyticsServerBinding>");
		System.exit(1);
	}
}
