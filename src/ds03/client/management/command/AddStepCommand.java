package ds03.client.management.command;

import java.util.logging.Logger;

import ds03.client.management.ManagementUserContext;
import ds03.server.exception.PriceStepException;

public class AddStepCommand extends AbstractManagementCommand {

	private static final Logger LOG = Logger.getLogger("AddStepCommand");

	@Override
	public void execute(ManagementUserContext context, String[] args) {
		if (args.length != 4) {
			throw new RuntimeException(
					"Usage: !addStep <startPrice> <endPrice> <fixedPrice> <variablePricePercent>");
		}
		double startPrice;
		double endPrice;
		double fixedPrice;
		double variablePricePercent;
		try {
			startPrice = Double.parseDouble(args[0]);
			endPrice = Double.parseDouble(args[1]);
			fixedPrice = Double.parseDouble(args[2]);
			variablePricePercent = Double.parseDouble(args[3]);
		} catch (NumberFormatException e) {
			System.err.println("ERROR: Parameters are expected as numbers");
			return;
		}

		if (endPrice == 0 && endPrice < startPrice) {
			endPrice = Double.POSITIVE_INFINITY;
		}
		try {
			context.getBillingServiceSecure().createPriceStep(startPrice,
					endPrice, fixedPrice, variablePricePercent);
			context.getOut()
					.write(
							"Step ["
									+ startPrice
									+ " "
									+ (endPrice == Double.POSITIVE_INFINITY ? "INFINITY"
											: endPrice)
									+ "] successfully added");
		} catch (PriceStepException e) {
			System.err.println(e.getMessage());
		} catch (Exception e) {
			LOG.warning("Exception caught");
		}
	}

}
