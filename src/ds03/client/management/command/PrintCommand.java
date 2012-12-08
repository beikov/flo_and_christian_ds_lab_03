package ds03.client.management.command;

import ds03.client.management.ManagementUserContext;
import ds03.event.Event;

public class PrintCommand extends AbstractManagementCommand {

	@Override
	public void execute(ManagementUserContext context, String[] args) {
		if (args.length != 0) {
			throw new RuntimeException("Usage: !print");
		}
		StringBuilder sb = new StringBuilder();
		for (Event e : context.popEventQueue()) {
			sb.append(e.toString());
			sb.append("\n");
		}

		context.getOut().writeln(sb.toString());
	}

}
