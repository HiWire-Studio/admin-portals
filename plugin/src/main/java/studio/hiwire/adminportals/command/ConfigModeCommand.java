package studio.hiwire.adminportals.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import studio.hiwire.adminportals.configmode.ConfigurationModeManager;

public class ConfigModeCommand extends AbstractCommandCollection {

  public ConfigModeCommand(ConfigurationModeManager configurationModeManager) {
    super("configmode", "Configuration mode commands");
    addSubCommand(new ToggleConfigModeCommand(configurationModeManager));
  }
}
