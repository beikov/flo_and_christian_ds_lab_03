package ds03.server.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import ds03.event.AuctionEndedEvent;
import ds03.event.AuctionStartedEvent;
import ds03.event.BidOverbidEvent;
import ds03.event.BidPlacedEvent;
import ds03.event.Event;
import ds03.event.EventHandler;
import ds03.event.GroupBidEndedEvent;
import ds03.event.handler.AuctionEndedEventHandler;
import ds03.event.handler.DefaultEventHandler;
import ds03.model.Auction;
import ds03.model.Bid;
import ds03.server.exception.RejectedException;
import ds03.server.service.AuctionService;
import ds03.server.service.UserService;
import ds03.server.util.Formats;
import ds03.server.util.TimedTask;

public class AuctionServiceImpl implements AuctionService {

	private static final long serialVersionUID = 1L;
	private static final long MAXIMUM_CONFIRMATION_TIMEOUT = 20000;

	private final AtomicLong currentId = new AtomicLong(0);
	private final ConcurrentMap<Long, Auction> auctions = new ConcurrentHashMap<Long, Auction>();
	private final ConcurrentMap<Long, Bid> groupBids = new ConcurrentHashMap<Long, Bid>();
	private transient EventHandler<Event> overbidHandler = DefaultEventHandler.INSTANCE;
	private transient EventHandler<AuctionEndedEvent> auctionEndHandler = new AuctionEndedEventHandler();
	private ScheduledExecutorService schedulerService;

	@Override
	public void setSchedulerService(ScheduledExecutorService schedulerService) {
		this.schedulerService = schedulerService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.service.impl.BidService#getAuctions()
	 */
	@Override
	public List<Auction> getAuctions() {
		final List<Auction> list = new ArrayList<Auction>();

		/* Clone the data so that for thread safety */
		for (Auction auction : auctions.values()) {
			list.add(auction.clone());
		}

		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.service.impl.BidService#getAuction(long)
	 */
	@Override
	public Auction getAuction(long auctionId) {
		final Auction auction = auctions.get(auctionId);
		if (auction != null) {
			return auction.clone();
		}
		return null;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.service.impl.BidService#createAuction(java.lang.String,
	 * java.lang.Integer, java.lang.String)
	 */
	@Override
	public Auction createAuction(String user, Integer duration,
			String description) {
		if (user == null || user.isEmpty()) {
			throw new IllegalArgumentException("Invalid user");
		}

		if (duration == null || duration < 1) {
			throw new IllegalArgumentException("Invalid duration");
		}

		if (description == null || description.isEmpty()) {
			throw new IllegalArgumentException("Invalid description");
		}

		final Calendar end = Calendar.getInstance();
		end.add(Calendar.SECOND, duration);
		final long id = currentId.incrementAndGet();

		final Auction auction = new Auction(id, description, user, end);

		/* Schedule a handler for notification */
		schedulerService.schedule(new Runnable() {

			@Override
			public void run() {
				final EventHandler<AuctionEndedEvent> handler = auctionEndHandler;

				if (handler != null) {
					final Auction auction = auctions.get(id);

					if (auction != null) {
						synchronized (auction) {
							try {
								removeGroupBid(id, "Auction ended");
							} catch (Exception ex) {
								/* We can not do anything here */
							}
							handler.handle(new AuctionEndedEvent(id));
							auctions.remove(id);
						}

					}
				}
			}
		}, end.getTimeInMillis() - new Date().getTime(), TimeUnit.MILLISECONDS);

		auctions.put(id, auction);
		DefaultEventHandler.INSTANCE.handle(new AuctionStartedEvent(id));
		return auction;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ds03.server.service.impl.BidService#bid(java.lang.String,
	 * java.lang.Integer, java.math.BigDecimal)
	 */
	@Override
	public Auction bid(String user, Long id, BigDecimal amount) {
		return bid(user, id, amount, false);
	}

	private Auction bid(String user, Long id, BigDecimal amount,
			boolean isGroupBid) {
		if (user == null || user.isEmpty()) {
			throw new IllegalArgumentException("Invalid user");
		}

		if (id == null) {
			throw new IllegalArgumentException("Invalid id");
		}

		if (amount == null || amount.compareTo(BigDecimal.ZERO) < 1) {
			throw new IllegalArgumentException("Invalid amount");
		}

		final Auction auction = auctions.get(id);

		if (auction == null) {
			throw new IllegalArgumentException("Auction for id " + id
					+ " does not exist");
		}

		/* Not specified if a user may bid on his own auction, so we allow it */

		final Auction result;
		String overbidUser = null;

		synchronized (auction) {
			if (!auctions.containsKey(id)) {
				throw new IllegalArgumentException("Auction for id " + id
						+ " does not exist");
			}

			result = auction.clone();

			/* Avoid partially updated auction, also see {@link Auction#clone} */
			if (amount.compareTo(auction.getBidValue()) > 0) {
				overbidUser = auction.getBidUser();
				auction.setBidUser(user);
				auction.setBidValue(amount);

				if (!isGroupBid && groupBids.containsKey(id)) {
					try {
					removeGroupBid(id,
							"Please use a higher bid price.");
					} catch (Exception e) {
						/* We can not do anything here */
					}
				}
			}
		}

		DefaultEventHandler.INSTANCE.handle(new BidPlacedEvent(user, id, amount
				.doubleValue()));

		if (overbidUser != null) {

			/* Notify the user who has been overbidden */
			final EventHandler<Event> handler = overbidHandler;

			if (handler != null) {
				handler.handle(new BidOverbidEvent(overbidUser, result.getId(),
						amount.doubleValue()));
			}
		}

		return result;
	}

	public void groupBid(final String user, final Long id,
			final BigDecimal amount) {
		if (user == null || user.isEmpty()) {
			throw new IllegalArgumentException("Invalid user");
		}

		if (id == null) {
			throw new IllegalArgumentException("Invalid id");
		}

		if (amount == null || amount.compareTo(BigDecimal.ZERO) < 1) {
			throw new IllegalArgumentException("Invalid amount");
		}

		final Auction auction = auctions.get(id);

		if (auction == null) {
			throw new IllegalArgumentException("Auction for id " + id
					+ " does not exist");
		}

		synchronized (auction) {
			if (!auctions.containsKey(id)) {
				throw new IllegalArgumentException("Auction for id " + id
						+ " does not exist");
			}

			if (groupBids.containsKey(id)) {
				throw new RejectedException("You may not overbid yourself.");
			}

			if (amount.compareTo(auction.getBidValue()) < 0) {
				throw new IllegalStateException(
						"ERROR: You unsucessfully bid with "
								+ amount
								+ " on '"
								+ auction.getDescription()
								+ "'. Current highest bid is "
								+ Formats.getNumberFormat().format(
										auction.getBidValue()));
			}

			if (groupBids.size() == UserService.INSTANCE.getLoggedInUsers()
					.size()) {
				throw new RejectedException(
						"No more group bids allowed for now. Try again later.");
			}
			
			boolean hasSlot = false;
			
			for(Bid b : groupBids.values()) {
				if(b.getBidUser().equals(user)) {
					hasSlot = true;
					break;
				}
			}

			if(hasSlot){
				throw new RejectedException("No more group bids allowed for now. Try again later.");
			}
			
			// queue a group bid
			final Bid bid = new Bid(amount, user);
			groupBids.put(id, bid);

		}
	}

	public void confirm(String user, final Long id, BigDecimal amount,
			String bidUser) {
		if (user == null || user.isEmpty()) {
			throw new IllegalArgumentException("Invalid user");
		}

		if (id == null) {
			throw new IllegalArgumentException("Invalid id");
		}

		if (amount == null || amount.compareTo(BigDecimal.ZERO) < 1) {
			throw new IllegalArgumentException("Invalid amount");
		}

		if (bidUser == null || bidUser.isEmpty()) {
			throw new IllegalArgumentException("Invalid bid user");
		}

		final Auction auction = auctions.get(id);

		if (auction == null) {
			throw new IllegalArgumentException("Auction for id " + id
					+ " does not exist");
		}

		synchronized (auction) {
			if (!auctions.containsKey(id)) {
				throw new IllegalArgumentException("Auction for id " + id
						+ " does not exist");
			}

			final Bid groupBid = groupBids.get(id);

			if (groupBid == null) {
				throw new RejectedException("Group bid does not exist.");
			}

			if (bidUser.equals(user)) {
				throw new RejectedException(
						"You must not confirm your own group bid");
			}

			if (!groupBid.getBidUser().equals(bidUser)) {
				throw new RejectedException(
						"User for confirmation is different than the group bid user");
			}
			if (!groupBid.getBidValue().equals(amount)) {
				throw new RejectedException(
						"Amount for confirmation is different from the group bid amount");
			}

			if (groupBid.getBidValue().compareTo(auction.getBidValue()) < 0) {
				removeGroupBid(id, "Please use a higher bid price");
				throw new RejectedException("Please use a higher bid price");
			}

			final Map<String, Date> confirmingUsers = groupBid
					.getConfirmingUsers();
			if (confirmingUsers.put(user, new Date()) != null) {
				throw new RejectedException(
						"You must not confirm a group bid twice");
			}

			if (confirmingUsers.size() == 2) {
				/*
				 * We already have the lock on the auction and checked all the
				 * parameters, so there can't be any exceptions thrown.
				 * Therefore pass through the values for the bid and complete
				 * it.
				 */
				bid(groupBid.getBidUser(), id, groupBid.getBidValue(), true);
				completeGroupBid(id);
				return;
			} else {
				long timeoutInMilliseconds = Math.min(
						MAXIMUM_CONFIRMATION_TIMEOUT, auction.getEndTimestamp()
								.getTimeInMillis() - new Date().getTime());

				final ScheduledExecutorService schedulerService = this.schedulerService;
				schedulerService.schedule(new Runnable() {

					@Override
					public void run() {
						final Auction auction = auctions.get(id);

						if (auction == null) {
							try {
								removeGroupBid(id, "Auction ended");
							} catch (Exception ex) {
								/* We can not do anything here */
							}
							return;
						}

						synchronized (auction) {
							if (!auctions.containsKey(id)) {
								try {
									removeGroupBid(id, "Auction ended");
								} catch (Exception ex) {
									/* We can not do anything here */
								}
								return;
							}

							NavigableSet<Date> confirmDates = new TreeSet<Date>(
									groupBid.getConfirmingUsers().values());
							Date lastConfirmationDate = confirmDates.pollLast();

							if (lastConfirmationDate == null) {
								lastConfirmationDate = groupBid
										.getCreatedDate();
							}

							long inactiveTime = new Date().getTime()
									- lastConfirmationDate.getTime();

							if (inactiveTime >= MAXIMUM_CONFIRMATION_TIMEOUT) {
								/* cancel */
								cancelGroupBidConfirmation(id, "Timeout");
							}
						}
					}
				}, timeoutInMilliseconds, TimeUnit.MILLISECONDS);

			}
		}

		/* wait for group bid completed or canceled */
		final Bid groupBid = groupBids.get(id);

		if (groupBid == null) {
			throw new RejectedException("Group bid does not exist.");
		}

		synchronized (groupBid) {
			if (!groupBids.containsKey(id)) {
				throw new RejectedException("Group bid does not exist.");
			}

			final StringBuilder sb = new StringBuilder();

			groupBid.awaitEnd(new EventHandler<GroupBidEndedEvent>() {

				@Override
				public void handle(GroupBidEndedEvent event) {
					if (!event.isSuccess()) {
						sb.append(event.getMessage());
					}
				}

			});

			if (sb.length() > 0) {
				throw new RejectedException(sb.toString());
			}
		}

	}

	private void removeGroupBid(long id, String message) {
		final Bid groupBid = groupBids.get(id);

		if (groupBid == null) {
			throw new RejectedException("Group bid does not exist.");
		}

		synchronized (groupBid) {
			if (!groupBids.containsKey(id)) {
				throw new RejectedException("Group bid does not exist.");
			}

			cancelGroupBidConfirmation(id, message);
			groupBids.remove(id);
		}
	}

	private void completeGroupBid(long id) {
		final Bid groupBid = groupBids.get(id);

		if (groupBid == null) {
			throw new RejectedException("Group bid does not exist.");
		}

		synchronized (groupBid) {
			if (!groupBids.containsKey(id)) {
				throw new RejectedException("Group bid does not exist.");
			}

			groupBids.remove(id);
			groupBid.notifyEnd(new GroupBidEndedEvent(true, null));
		}
	}

	private void cancelGroupBidConfirmation(long id, String message) {
		final Bid groupBid = groupBids.get(id);

		if (groupBid == null) {
			throw new RejectedException("Group bid does not exist.");
		}

		synchronized (groupBid) {
			if (!groupBids.containsKey(id)) {
				throw new RejectedException("Group bid does not exist.");
			}

			groupBid.getConfirmingUsers().clear();
			groupBid.notifyEnd(new GroupBidEndedEvent(false, message));
		}
	}
}
