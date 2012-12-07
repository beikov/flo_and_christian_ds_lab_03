package ds03.client.management;

import java.util.Set;

import ds03.command.Context;
import ds03.event.Event;
import ds03.server.service.BillingServiceSecure;

public interface ManagementUserContext extends Context {

	public void addSubscription(String subscription);

	public void removeSubscription(String subscription);

	public boolean isAuto();

	public void setAuto(boolean auto);

	public void addEvent(Event event);

	public Set<Event> popEventQueue();

	public BillingServiceSecure getBillingServiceSecure();

}