package ds03.client.management.command;

import ds03.client.management.ManagementUserContext;
import ds03.command.Command;
import ds03.command.Context;

public abstract class AbstractManagementCommand implements Command {
	public void execute(Context context, String[] params) {
		execute((ManagementUserContext) context, params);
	}

	public abstract void execute(ManagementUserContext context, String[] params);

}
