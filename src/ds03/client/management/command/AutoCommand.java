package ds03.client.management.command;

import ds03.client.management.ManagementUserContext;

public class AutoCommand extends AbstractManagementCommand {

	@Override
	public void execute(ManagementUserContext context, String[] args) {

		if (args.length != 0) {
			throw new RuntimeException("Usage: !auto");
		}
		new PrintCommand().execute(context, args);
		context.setAuto(true);
	}

}
