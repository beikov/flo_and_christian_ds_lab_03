package ds03.client.management.command;

import java.rmi.RemoteException;
import java.text.DecimalFormat;

import ds03.client.management.ManagementUserContext;
import ds03.model.PriceStep;

public class StepsCommand extends AbstractManagementCommand {

	@Override
	public void execute(ManagementUserContext context, String[] args) {
		if (args.length != 0) {
			throw new RuntimeException("Usage: !steps");
		}
		try {
			DecimalFormat decimalFormat = new DecimalFormat(".0");
			context.getOut().writeln(
					"Min_Price Max_Price Fee_Fixed Fee_Variable");
			for (PriceStep priceStep : context.getBillingServiceSecure()
					.getPriceSetps().getPriceSteps()) {
				context.getOut()
						.writeln(
								String.format(
										"%-10.0f%-10s"
												+ (priceStep.getEndPrice() == Double.POSITIVE_INFINITY ? ""
														: "")
												+ "%-10.1f%-12s%n",
										priceStep.getStartPrice(),
										(priceStep.getEndPrice() == Double.POSITIVE_INFINITY ? "INIFINITY"
												: (int) priceStep.getEndPrice()),
										priceStep.getFixedPrice(),
										decimalFormat.format(priceStep
												.getVariablePricePercent())
												+ "%"));
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
