package ds03.event;

public interface EventHandler<T> {

	public void handle(T event);
}
