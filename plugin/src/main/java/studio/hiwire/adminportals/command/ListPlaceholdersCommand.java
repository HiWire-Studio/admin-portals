package studio.hiwire.adminportals.command;

import static studio.hiwire.adminportals.AdminPortalsPlugin.PREFIX;
import static studio.hiwire.adminportals.TranslationKeys.CHAT_MESSAGES;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import studio.hiwire.adminportals.TranslationKeys.Params;
import studio.hiwire.adminportals.placeholder.PlaceholderManager;

public class ListPlaceholdersCommand extends AbstractCommand {

  private static final String MSG_PLACEHOLDERS_LIST = CHAT_MESSAGES + ".Command.Placeholders.List";
  private static final String MSG_NO_PLACEHOLDERS_REGISTERED =
      MSG_PLACEHOLDERS_LIST + ".NoPlaceHoldersRegistered";
  private static final String MSG_OUTPUT = MSG_PLACEHOLDERS_LIST + ".Output";

  private final PlaceholderManager placeholderManager;

  public ListPlaceholdersCommand(PlaceholderManager placeholderManager) {
    super("list", "List all registered placeholders", false);
    this.placeholderManager = placeholderManager;
  }

  @NullableDecl
  @Override
  protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
    Set<String> placeholders = placeholderManager.getRegisteredNames();

    if (placeholders.isEmpty()) {
      final var noPlaceholdersRegisteredMessage =
          Message.translation(MSG_NO_PLACEHOLDERS_REGISTERED).param(Params.PLUGIN_PREFIX, PREFIX);
      context.sender().sendMessage(noPlaceholdersRegisteredMessage);
      return null;
    }

    String formatted =
        placeholders.stream()
            .sorted()
            .map(name -> "{" + name + "}")
            .collect(Collectors.joining(", "));

    final var outputMessage =
        Message.translation(MSG_OUTPUT)
            .param(Params.PLUGIN_PREFIX, PREFIX)
            .param(Params.PLACEHOLDER_AMOUNT, placeholders.size())
            .param(Params.PLACEHOLDER_LIST, formatted);

    context.sender().sendMessage(outputMessage);
    return null;
  }
}
