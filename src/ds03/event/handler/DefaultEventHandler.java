package ds03.event.handler;

import ds03.event.Event;

public class DefaultEventHandler extends AbstractEventHandler<Event> {

	public static DefaultEventHandler INSTANCE = new DefaultEventHandler();

	private DefaultEventHandler() {

	}
}
