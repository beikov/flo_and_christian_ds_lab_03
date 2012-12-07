package ds03.client.management.command;

import java.util.logging.Logger;

import ds03.client.management.ManagementUserContext;
import ds03.server.exception.PriceStepException;

public class RemoveStepCommand extends AbstractManagementCommand {

	private static final Logger LOG = Logger.getLogger("RemoveStepCommand");

	@Override
	public void execute(ManagementUserContext context, String[] args) {
		if (args.length != 2) {
			throw new RuntimeException(
					"Usage: !removeStep <startPrice> <endPrice>");
		}
		
		double startPrice;
		double endPrice;

		try {
			startPrice = Double.parseDouble(args[0]);
			endPrice = Double.parseDouble(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("ERROR: Parameters are expected as numbers");
			return;
		}

		if (endPrice == 0 && endPrice < startPrice) {
			endPrice = Double.POSITIVE_INFINITY;
		}
		try {
			context.getBillingServiceSecure().deletePriceStep(startPrice,
					endPrice);

			context.getOut()
					.write(
							"Price step ["
									+ startPrice
									+ " "
									+ (endPrice == Double.POSITIVE_INFINITY ? "INFINITY"
											: endPrice)
									+ "] successfully removed");
		} catch (PriceStepException e) {
			System.err.println(e.getMessage());
		} catch (Exception e) {
			LOG.warning("Exception caught");
		}
	}

}
