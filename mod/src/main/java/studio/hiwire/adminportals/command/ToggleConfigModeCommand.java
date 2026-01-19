package studio.hiwire.adminportals.command;

import static studio.hiwire.adminportals.AdminPortalsPlugin.PREFIX;
import static studio.hiwire.adminportals.TranslationKeys.CHAT_MESSAGES;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import studio.hiwire.adminportals.TranslationKeys.Params;
import studio.hiwire.adminportals.configmode.ConfigurationModeManager;

public class ToggleConfigModeCommand extends AbstractPlayerCommand {

  private static final String MSG_CONFIG_MODE_TOGGLE =
      CHAT_MESSAGES + ".Command.Placeholders.ConfigMode.Toggle";
  private static final String MSG_ENABLED = MSG_CONFIG_MODE_TOGGLE + ".Enabled";
  private static final String MSG_DISABLED = MSG_CONFIG_MODE_TOGGLE + ".Disabled";

  private final ConfigurationModeManager configurationModeManager;

  public ToggleConfigModeCommand(ConfigurationModeManager configurationModeManager) {
    super("toggle", "Toggle configuration mode for yourself", false);
    this.configurationModeManager = configurationModeManager;
  }

  @Override
  protected void execute(
      @NonNullDecl CommandContext context,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef,
      @Nonnull World world) {
    boolean enabled = configurationModeManager.toggle(playerRef);

    if (enabled) {
      final var enabledMessage = Message.translation(MSG_ENABLED).param(Params.MOD_PREFIX, PREFIX);
      playerRef.sendMessage(enabledMessage);
    } else {
      final var disabledMessage =
          Message.translation(MSG_DISABLED).param(Params.MOD_PREFIX, PREFIX);
      playerRef.sendMessage(disabledMessage);
    }
  }
}
