package ds03.client.management.command;

import java.rmi.RemoteException;

import ds03.client.management.ManagementUserContext;
import ds03.model.Bill;
import ds03.model.BillLine;

public class BillCommand extends AbstractManagementCommand {

	@Override
	public void execute(ManagementUserContext context, String[] args) {
		if (args.length != 1) {
			throw new RuntimeException("Usage: !bill <username>");
		}
		try {
			Bill bill = context.getBillingServiceSecure().getBill(args[0]);
			if (bill == null) {
				context.getOut().writeln(
						"No bills for user " + args[0] + " exist");
				return;
			}
			context.getOut().writeln(
					"auction_ID strike_price fee_fixed fee_variable fee_total");
			for (BillLine billLine : bill.getBillLines()) {
				context.getOut().writeln(
						String.format("%-11d%-13.0f%-10.1f%-13.1f%-9.1f%n",
								billLine.getAuctionId(),
								billLine.getStrikePrice(),
								billLine.getFeeFixed(),
								billLine.getFeeVariable(),
								billLine.getFeeTotal()));
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
