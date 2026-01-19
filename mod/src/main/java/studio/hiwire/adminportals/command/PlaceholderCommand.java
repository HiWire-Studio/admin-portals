package studio.hiwire.adminportals.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import studio.hiwire.adminportals.placeholder.PlaceholderManager;

public class PlaceholderCommand extends AbstractCommandCollection {

  public PlaceholderCommand(PlaceholderManager placeholderManager) {
    super("placeholder", "Placeholder management commands");
    addSubCommand(new ListPlaceholdersCommand(placeholderManager));
  }
}
