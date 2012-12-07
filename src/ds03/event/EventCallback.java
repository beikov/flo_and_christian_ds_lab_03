package ds03.event;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface EventCallback extends Remote {
	public void handle(Event event) throws RemoteException;
}
