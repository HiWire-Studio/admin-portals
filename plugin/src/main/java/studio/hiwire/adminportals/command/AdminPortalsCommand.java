package studio.hiwire.adminportals.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import studio.hiwire.adminportals.configmode.ConfigurationModeManager;
import studio.hiwire.adminportals.placeholder.PlaceholderManager;

public class AdminPortalsCommand extends AbstractCommandCollection {

  public AdminPortalsCommand(
      PlaceholderManager placeholderManager, ConfigurationModeManager configurationModeManager) {
    super("adminportals", "AdminPortals management commands");
    addSubCommand(new PlaceholderCommand(placeholderManager));
    addSubCommand(new ConfigModeCommand(configurationModeManager));
  }
}
