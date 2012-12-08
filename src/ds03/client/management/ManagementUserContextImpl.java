package ds03.client.management;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import ds03.client.util.ClientConsole;
import ds03.event.Event;
import ds03.io.AuctionProtocolChannel;
import ds03.server.service.BillingServiceSecure;
import ds03.util.ServiceLocator;

public class ManagementUserContextImpl implements ManagementUserContext {

	private final ClientConsole clientConsole;
	private final Set<String> subscriptions = new HashSet<String>();
	private Set<Event> eventSet = new LinkedHashSet<Event>();
	private BillingServiceSecure billingServiceSecure;
	private String username = null;
	private boolean auto;

	public ManagementUserContextImpl(ClientConsole clientConsole) {
		this.clientConsole = clientConsole;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.management.ManagementUserContext#login(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public boolean login(String username, String password) {
		try {
			if ((billingServiceSecure = ServiceLocator.INSTANCE
					.getBillingService().login(username, password)) != null) {
				this.username = username;
				return true;
			}
		} catch (Exception e) {

		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.management.ManagementUserContext#logout()
	 */
	@Override
	public void logout() {
		this.username = null;
		this.billingServiceSecure = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.management.ManagementUserContext#isLoggedIn()
	 */
	@Override
	public boolean isLoggedIn() {
		return (username != null && billingServiceSecure != null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ds03.client.management.ManagementUserContext#addSubscription(java.lang
	 * .String)
	 */
	@Override
	public void addSubscription(String subscription) {
		subscriptions.add(subscription);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ds03.client.management.ManagementUserContext#removeSubscription(java.
	 * lang.String)
	 */
	@Override
	public void removeSubscription(String subscription) {
		subscriptions.remove(subscription);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.management.ManagementUserContext#isAuto()
	 */
	@Override
	public boolean isAuto() {
		return auto;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.management.ManagementUserContext#setAuto(boolean)
	 */
	@Override
	public void setAuto(boolean auto) {
		this.auto = auto;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ds03.client.management.ManagementUserContext#addEvent(ds03.server.event
	 * .Event)
	 */
	@Override
	public void addEvent(Event event) {
		if (!isAuto()) {
			eventSet.add(event);
		} else {
			clientConsole.writeln(event.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.management.ManagementUserContext#popEventQueue()
	 */
	@Override
	public Set<Event> popEventQueue() {
		Set<Event> tempEvents = eventSet;
		eventSet = new LinkedHashSet<Event>();
		return tempEvents;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.management.ManagementUserContext#getUsername()
	 */
	@Override
	public String getUsername() {
		return username;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ds03.client.management.ManagementUserContext#getBillingServiceSecure()
	 */
	@Override
	public BillingServiceSecure getBillingServiceSecure() {
		return billingServiceSecure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.client.management.ManagementUserContext#getOut()
	 */
	@Override
	public ClientConsole getOut() {
		return clientConsole;
	}

	@Override
	public AuctionProtocolChannel getChannel() {
		return null;
	}

	public void setChannel(AuctionProtocolChannel apc) {

	}

}
