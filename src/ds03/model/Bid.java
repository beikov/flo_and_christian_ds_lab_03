package ds03.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ds03.event.Event;
import ds03.event.EventCallback;
import ds03.event.EventHandler;
import ds03.event.GroupBidEndedEvent;

public class Bid implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;
	private final Date createdDate;
	private BigDecimal bidValue = BigDecimal.ZERO;
	private String bidUser;
	private final Map<String, Date> confirmingUsers = new HashMap<String, Date>();
	private GroupBidEndedEvent endEvent;
	

	public Bid(BigDecimal bidValue, String bidUser) {
		super();
		this.bidValue = bidValue;
		this.bidUser = bidUser;
		this.createdDate = new Date();
	}

	public BigDecimal getBidValue() {
		return bidValue;
	}

	public void setBidValue(BigDecimal bidValue) {
		this.bidValue = bidValue;
	}

	public String getBidUser() {
		return bidUser;
	}

	public void setBidUser(String bidUser) {
		this.bidUser = bidUser;
	}

	public Map<String, Date> getConfirmingUsers() {
		return confirmingUsers;
	}

	public Date getCreatedDate() {
		return createdDate;
	}
	
	public synchronized void awaitEnd(EventHandler<GroupBidEndedEvent> onEndCallback) {
		try {
			wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		onEndCallback.handle(endEvent);
	}
	
	public synchronized void notifyEnd(GroupBidEndedEvent event){
		this.endEvent = event;
		notifyAll();
	}
	
}
