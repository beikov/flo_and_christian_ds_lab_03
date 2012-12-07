package ds03.server.service.impl;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import ds03.event.AuctionEvent;
import ds03.event.AuctionSuccessRatioEvent;
import ds03.event.AuctionTimeAvgEvent;
import ds03.event.BidCountPerMinuteEvent;
import ds03.event.BidEvent;
import ds03.event.BidPriceMaxEvent;
import ds03.event.Event;
import ds03.event.EventCallback;
import ds03.event.UserEvent;
import ds03.event.UserSessiontimeAvgEvent;
import ds03.event.UserSessiontimeMaxEvent;
import ds03.event.UserSessiontimeMinEvent;
import ds03.server.service.AnalyticsService;

public class AnalyticsServiceImpl implements AnalyticsService {

	private static final long serialVersionUID = 1L;
	private final Map<String, ConcurrentMap<String, EventCallback>> eventMap = new HashMap<String, ConcurrentMap<String, EventCallback>>();
	private final ConcurrentMap<String, Long> startValue = new ConcurrentHashMap<String, Long>();
	private final ConcurrentMap<Long, Long> auctionBegin = new ConcurrentHashMap<Long, Long>();
	private final AtomicLong subscribeSequence = new AtomicLong();

	public AnalyticsServiceImpl() {
		eventMap.put("USER_LOGIN",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("USER_LOGOUT",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("USER_DISCONNECTED",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("USER_SESSIONTIME_MIN",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("USER_SESSIONTIME_MAX",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("USER_SESSIONTIME_AVG",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("AUCTION_STARTED",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("AUCTION_ENDED",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("AUCTION_SUCCESS_RATIO",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("AUCTION_TIME_AVG",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("BID_WON", new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("BID_PLACED",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("BID_PRICE_MAX",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("BID_OVERBID",
				new ConcurrentHashMap<String, EventCallback>());
		eventMap.put("BID_COUNT_PER_MINUTE",
				new ConcurrentHashMap<String, EventCallback>());

		subscribe0("USER_LOGIN", new EventCallback() {

			@Override
			public void handle(Event event) {
				UserEvent userEvent = (UserEvent) event;
				startValue.put(userEvent.getUser(), userEvent.getTimeStamp());
				StatisticDataServiceImpl.INSTANCE.incrementSessionCount();

			}
		});
		subscribe0("(USER_LOGOUT|USER_DISCONNECTED)", new EventCallback() {

			@Override
			public void handle(Event event) {
				UserEvent userEvent = (UserEvent) event;

				if (startValue.containsKey(userEvent.getUser())) {
					StatisticDataServiceImpl.INSTANCE
							.addUserSessionTime(userEvent.getTimeStamp()
									- startValue.remove(userEvent.getUser()));

				}

				processEvent0(new UserSessiontimeMinEvent(
						StatisticDataServiceImpl.INSTANCE
								.getMinUserSessionTime()));
				processEvent0(new UserSessiontimeMaxEvent(
						StatisticDataServiceImpl.INSTANCE
								.getMaxUserSessionTime()));
				processEvent0(new UserSessiontimeAvgEvent(
						StatisticDataServiceImpl.INSTANCE
								.getAverageUserSessionTime()));
			}
		});

		subscribe0("AUCTION_STARTED", new EventCallback() {

			@Override
			public void handle(Event event) {
				auctionBegin.put(((AuctionEvent) event).getAuctionId(),
						event.getTimeStamp());
				StatisticDataServiceImpl.INSTANCE.incrementAuctionCount();
			}
		});

		subscribe0("BID_WON", new EventCallback() {

			@Override
			public void handle(Event event) {
				StatisticDataServiceImpl.INSTANCE
						.incrementSuccessfulAuctionCount();
			}
		});

		subscribe0("AUCTION_ENDED", new EventCallback() {

			@Override
			public void handle(Event event) {
				if (auctionBegin.containsKey(((AuctionEvent) event)
						.getAuctionId())) {
					StatisticDataServiceImpl.INSTANCE.addAuctionDuration(event
							.getTimeStamp()
							- auctionBegin.remove(((AuctionEvent) event)
									.getAuctionId()));
				}

				processEvent0(new AuctionTimeAvgEvent(
						StatisticDataServiceImpl.INSTANCE
								.getAverageAuctionTime()));
				processEvent0(new AuctionSuccessRatioEvent(
						StatisticDataServiceImpl.INSTANCE
								.getAuctionSuccessRatio()));
			}
		});

		subscribe0("BID_PLACED", new EventCallback() {

			@Override
			public void handle(Event event) {
				StatisticDataServiceImpl.INSTANCE.incrementBidCount();
				StatisticDataServiceImpl.INSTANCE
						.offerHighestBid(((BidEvent) event).getPrice());

				processEvent0(new BidPriceMaxEvent(
						StatisticDataServiceImpl.INSTANCE.getHighestBid()));
				processEvent0(new BidCountPerMinuteEvent(
						StatisticDataServiceImpl.INSTANCE
								.getBidCountPerMinute()));

			}
		});
	}

	@Override
	public String subscribe(final String pattern, final EventCallback handler)
			throws RemoteException {
		return subscribe0(pattern, handler);
	}

	private String subscribe0(final String pattern, final EventCallback handler) {

		final Pattern p = Pattern.compile(pattern);
		final String id = Long.toString(subscribeSequence.incrementAndGet());

		for (Map.Entry<String, ConcurrentMap<String, EventCallback>> entry : eventMap
				.entrySet()) {
			if (p.matcher(entry.getKey()).matches()) {
				entry.getValue().put(id, handler);
			}
		}

		return id;
	}

	@Override
	public void processEvent(Event event) throws RemoteException {
		processEvent0(event);
	}

	private void processEvent0(Event event) {
		Iterator<EventCallback> it = eventMap.get(event.getType()).values()
				.iterator();

		while (it.hasNext()) {
			final EventCallback eventCallback = it.next();

			try {
				eventCallback.handle(event);
			} catch (Exception e) {
				// e.printStackTrace();
				/* Remove the eventhandler as soon it is no longer available */
				it.remove();
			}
		}
	}

	@Override
	public void unsubscribe(String identifier) throws RemoteException {
		for (Map.Entry<String, ConcurrentMap<String, EventCallback>> entry : eventMap
				.entrySet()) {
			entry.getValue().remove(identifier);
		}
	}

}
