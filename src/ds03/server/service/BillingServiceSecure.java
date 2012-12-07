package ds03.server.service;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import ds03.model.Bill;
import ds03.model.PriceSteps;
import ds03.server.exception.PriceStepException;

public interface BillingServiceSecure extends Remote, Serializable {
	public PriceSteps getPriceSetps() throws RemoteException;

	public void createPriceStep(double startPrice, double endPrice,
			double fixedPrice, double variablePricePercent)
			throws RemoteException, PriceStepException;

	public void deletePriceStep(double startPrice, double endPrice)
			throws RemoteException, PriceStepException;

	public void billAuction(String user, long auctionId, double price)
			throws RemoteException;

	public Bill getBill(String user) throws RemoteException;

}
