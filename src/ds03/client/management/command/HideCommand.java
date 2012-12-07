package ds03.client.management.command;

import ds03.client.management.ManagementUserContext;

public class HideCommand extends AbstractManagementCommand {

	@Override
	public void execute(ManagementUserContext context, String[] args) {
		if (args.length != 0) {
			throw new RuntimeException("Usage: !hide");
		}
		context.setAuto(false);
	}

}
