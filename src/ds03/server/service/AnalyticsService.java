package ds03.server.service;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import ds03.event.Event;
import ds03.event.EventCallback;

public interface AnalyticsService extends Remote, Serializable {
	public String subscribe(String pattern, EventCallback handler)
			throws RemoteException;

	public void processEvent(Event event) throws RemoteException;

	public void unsubscribe(String identifier) throws RemoteException;
}
