package ds03.client.util;

import java.util.Map;

public interface RequestStopCondition {
	public boolean shouldStop(Map<String, String> results);
}
