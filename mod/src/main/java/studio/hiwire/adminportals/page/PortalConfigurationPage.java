package studio.hiwire.adminportals.page;

import static studio.hiwire.adminportals.AdminPortalsPlugin.PREFIX;
import static studio.hiwire.adminportals.TranslationKeys.CHAT_MESSAGES;
import static studio.hiwire.adminportals.TranslationKeys.UI;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.state.BlockMapMarker;
import com.hypixel.hytale.server.core.universe.world.meta.state.BlockMapMarkersResource;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.ToString;
import org.joml.Vector3i;
import studio.hiwire.adminportals.AdminPortalsPlugin;
import studio.hiwire.adminportals.Permissions;
import studio.hiwire.adminportals.TranslationKeys.Params;
import studio.hiwire.adminportals.component.PortalConfigComponent;

public class PortalConfigurationPage
    extends InteractiveCustomUIPage<PortalConfigurationPage.PageData> {

  public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  @Nonnull private final Ref<ChunkStore> blockRef;

  private static final String DEFAULT_MARKER_ICON = "Warp.png";

  private static final String MSG_CONFIG_PORTAL = CHAT_MESSAGES + ".Configuration.Portal";
  private static final String MSG_CMD_NOT_SAVED = MSG_CONFIG_PORTAL + ".Command.NotSaved";
  private static final String MSG_CMD_NOT_SAVED_DETAIL_CMD_MISSING =
      MSG_CONFIG_PORTAL + ".Command.NotSaved.Detail.CommandMissing";
  private static final String MSG_SERVER_NOT_SAVED = MSG_CONFIG_PORTAL + ".Server.NotSaved";
  private static final String MSG_SERVER_NOT_SAVED_DETAIL_HOST_MISSING =
      MSG_CONFIG_PORTAL + ".Server.NotSaved.Detail.HostMissing";
  private static final String MSG_SERVER_NOT_SAVED_DETAIL_PORT_INVALID =
      MSG_CONFIG_PORTAL + ".Server.NotSaved.Detail.PortInvalid";
  private static final String MSG_WORLD_NOT_SAVED = MSG_CONFIG_PORTAL + ".World.NotSaved";
  private static final String MSG_WORLD_NOT_SAVED_DETAIL_NAME_MISSING =
      MSG_CONFIG_PORTAL + ".World.NotSaved.Detail.NameMissing";
  private static final String MSG_WORLD_NOT_SAVED_DETAIL_COORDINATES_INVALID =
      MSG_CONFIG_PORTAL + ".World.NotSaved.Detail.CoordinatesInvalid";
  private static final String MSG_PORTAL_SAVED = MSG_CONFIG_PORTAL + ".Saved";
  private static final String MSG_UNKNOWN_PLACEHOLDERS = MSG_CONFIG_PORTAL + ".UnknownPlaceholders";
  private static final String MSG_NO_PERMISSION = MSG_CONFIG_PORTAL + ".Edit.NoPermission";
  private static final String UI_PORTAL_TYPE = UI + ".PortalType.";
  private static final String UI_COMMAND_SENDER = UI + ".CommandSender.";

  private PortalConfigComponent.Type currentType;
  private final List<PortalConfigComponent.CommandEntry> currentCommands = new ObjectArrayList<>();
  private String currentServerHost;
  private int currentServerPort;
  private String currentWorldName;
  @Nullable private Double currentWorldX;
  @Nullable private Double currentWorldY;
  @Nullable private Double currentWorldZ;
  private String currentWorldXText;
  private String currentWorldYText;
  private String currentWorldZText;
  private String currentMapMarkerName;
  private String currentMapMarkerIcon;
  private String currentInteractionSoundEffectId;
  private boolean currentCollisionInteraction;
  private boolean currentUseInteraction;

  public PortalConfigurationPage(
      @Nonnull PlayerRef playerRef,
      @Nonnull Ref<ChunkStore> blockRef,
      @Nullable PortalConfigComponent config) {
    super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
    this.blockRef = blockRef;

    initFromConfig(
        config != null ? config.migrated().normalized() : new PortalConfigComponent().normalized());
    initMapMarkerData();
  }

  @Override
  public void build(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull UICommandBuilder commandBuilder,
      @Nonnull UIEventBuilder eventBuilder,
      @Nonnull Store<EntityStore> store) {

    commandBuilder.append("Pages/HiWire_AdminPortals_PortalConfigurationPage.ui");

    // Build Type dropdown
    ObjectArrayList<DropdownEntryInfo> typeEntries = new ObjectArrayList<>();
    for (PortalConfigComponent.Type type : PortalConfigComponent.Type.values()) {
      typeEntries.add(
          new DropdownEntryInfo(
              LocalizableString.fromMessageId(UI_PORTAL_TYPE + type.name()), type.name()));
    }
    commandBuilder.set("#Type #Input.Entries", (List<?>) typeEntries);
    commandBuilder.set("#Type #Input.Value", currentType.name());

    // Build command list
    buildCommandList(commandBuilder, eventBuilder);

    commandBuilder.set("#ServerHost #Input.Value", currentServerHost);
    commandBuilder.set(
        "#ServerPort #Input.Value",
        currentServerPort == 0 ? "" : String.valueOf(currentServerPort));
    commandBuilder.set("#WorldName #Input.Value", currentWorldName);
    commandBuilder.set("#WorldX #Input.Value", currentWorldXText);
    commandBuilder.set("#WorldY #Input.Value", currentWorldYText);
    commandBuilder.set("#WorldZ #Input.Value", currentWorldZText);

    commandBuilder.set("#MapMarkerName #Input.Value", currentMapMarkerName);
    commandBuilder.set("#MapMarkerIcon #Input.Value", currentMapMarkerIcon);

    commandBuilder.set("#InteractionSoundEffectId #Input.Value", currentInteractionSoundEffectId);

    commandBuilder.set("#CollisionInteraction #CheckBox.Value", currentCollisionInteraction);
    commandBuilder.set("#UseInteraction #CheckBox.Value", currentUseInteraction);

    // Update visibility based on type
    updateSectionVisibility(commandBuilder);

    // Event: Type changed
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#Type #Input",
        new EventData().append("Action", "TypeChanged").append("@Type", "#Type #Input.Value"),
        false);

    // Event: Add Command button
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#AddCommandButton",
        new EventData().append("Action", "AddCommand"),
        false);

    // Event: Save button (commands are tracked server-side, only collect non-command fields)
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SaveButton",
        new EventData()
            .append("Action", "Save")
            .append("@Type", "#Type #Input.Value")
            .append("@ServerHost", "#ServerHost #Input.Value")
            .append("@ServerPort", "#ServerPort #Input.Value")
            .append("@WorldName", "#WorldName #Input.Value")
            .append("@WorldX", "#WorldX #Input.Value")
            .append("@WorldY", "#WorldY #Input.Value")
            .append("@WorldZ", "#WorldZ #Input.Value")
            .append("@MapMarkerName", "#MapMarkerName #Input.Value")
            .append("@MapMarkerIcon", "#MapMarkerIcon #Input.Value")
            .append("@InteractionSoundEffectId", "#InteractionSoundEffectId #Input.Value")
            .append("@CollisionInteraction", "#CollisionInteraction #CheckBox.Value")
            .append("@UseInteraction", "#UseInteraction #CheckBox.Value"));
  }

  private void buildCommandList(
      @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
    commandBuilder.clear("#CommandList");

    ObjectArrayList<DropdownEntryInfo> senderEntries = new ObjectArrayList<>();
    for (PortalConfigComponent.CommandSender sender :
        PortalConfigComponent.CommandSender.values()) {
      senderEntries.add(
          new DropdownEntryInfo(
              LocalizableString.fromMessageId(UI_COMMAND_SENDER + sender.name()), sender.name()));
    }

    // Top spacer (scrolls with content)
    commandBuilder.append("#CommandList", "Pages/HiWire_AdminPortals_Spacer.ui");

    for (int i = 0; i < currentCommands.size(); i++) {
      PortalConfigComponent.CommandEntry entry = currentCommands.get(i);
      // Offset by 1 to account for the top spacer
      String selector = "#CommandList[" + (i + 1) + "]";

      commandBuilder.append("#CommandList", "Pages/HiWire_AdminPortals_CommandEntry.ui");
      commandBuilder.set(selector + " #CommandInput.Value", entry.getCommand());
      commandBuilder.set(selector + " #SenderInput.Entries", (List<?>) senderEntries);
      commandBuilder.set(selector + " #SenderInput.Value", entry.getCommandSender().name());

      eventBuilder.addEventBinding(
          CustomUIEventBindingType.ValueChanged,
          selector + " #CommandInput",
          new EventData()
              .append("Action", "UpdateCommand")
              .append("Index", String.valueOf(i))
              .append("@Command", selector + " #CommandInput.Value"),
          false);

      eventBuilder.addEventBinding(
          CustomUIEventBindingType.ValueChanged,
          selector + " #SenderInput",
          new EventData()
              .append("Action", "UpdateCommandSender")
              .append("Index", String.valueOf(i))
              .append("@CommandSender", selector + " #SenderInput.Value"),
          false);

      eventBuilder.addEventBinding(
          CustomUIEventBindingType.Activating,
          selector + " #DeleteButton",
          new EventData().append("Action", "DeleteCommand").append("Index", String.valueOf(i)),
          false);
    }

    // Bottom spacer (scrolls with content)
    commandBuilder.append("#CommandList", "Pages/HiWire_AdminPortals_Spacer.ui");
  }

  private static int parseIndex(@Nullable String value) {
    if (value == null) {
      return -1;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private void updateSectionVisibility(@Nonnull UICommandBuilder commandBuilder) {
    boolean isCommand = currentType == PortalConfigComponent.Type.Command;
    boolean isServer = currentType == PortalConfigComponent.Type.Server;
    boolean isWorld = currentType == PortalConfigComponent.Type.World;
    commandBuilder.set("#CommandSection.Visible", isCommand);
    commandBuilder.set("#ServerSection.Visible", isServer);
    commandBuilder.set("#WorldSection.Visible", isWorld);
  }

  @Override
  public void handleDataEvent(
      @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
    LOGGER.at(Level.FINE).log("Handling data event: %s", data);

    if (data.action == null) {
      return;
    }

    Player playerComponent = store.getComponent(ref, Player.getComponentType());

    switch (data.action) {
      case "TypeChanged":
        if (data.type != null) {
          currentType = data.type;
          UICommandBuilder commandBuilder = new UICommandBuilder();
          updateSectionVisibility(commandBuilder);
          sendUpdate(commandBuilder);
        }
        break;

      case "AddCommand":
        {
          currentCommands.add(
              new PortalConfigComponent.CommandEntry(
                  "", PortalConfigComponent.CommandSender.Server));
          UICommandBuilder commandBuilder = new UICommandBuilder();
          UIEventBuilder eventBuilder = new UIEventBuilder();
          buildCommandList(commandBuilder, eventBuilder);
          sendUpdate(commandBuilder, eventBuilder, false);
        }
        break;

      case "DeleteCommand":
        {
          int index = parseIndex(data.index);
          if (index >= 0 && index < currentCommands.size()) {
            currentCommands.remove(index);
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            buildCommandList(commandBuilder, eventBuilder);
            sendUpdate(commandBuilder, eventBuilder, false);
          }
        }
        break;

      case "UpdateCommand":
        {
          int index = parseIndex(data.index);
          if (index >= 0 && index < currentCommands.size() && data.command != null) {
            currentCommands.get(index).setCommand(data.command);
          }
        }
        break;

      case "UpdateCommandSender":
        {
          int index = parseIndex(data.index);
          if (index >= 0 && index < currentCommands.size() && data.commandSender != null) {
            currentCommands.get(index).setCommandSender(data.commandSender);
          }
        }
        break;

      case "Save":
        if (!PermissionsModule.get()
            .hasPermission(playerRef.getUuid(), Permissions.PORTAL_CONFIG_EDIT)) {
          playerRef.sendMessage(
              Message.translation(MSG_NO_PERMISSION)
                  .param(Params.MOD_PREFIX, PREFIX)
                  .param(Params.PERMISSION, Permissions.PORTAL_CONFIG_EDIT));
          sendUpdate();
          return;
        }

        boolean saved = false;
        if (data.type == PortalConfigComponent.Type.Command) {
          saved = handleCommandSave(data);
        } else if (data.type == PortalConfigComponent.Type.Server) {
          saved = handleServerSave(data);
        } else if (data.type == PortalConfigComponent.Type.World) {
          saved = handleWorldSave(data);
        }

        finishSaveEvent(ref, store, playerComponent, saved);
        break;
    }
  }

  void finishSaveEvent(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Store<EntityStore> store,
      @Nullable Player playerComponent,
      boolean saved) {
    if (saved && playerComponent != null) {
      playerComponent.getPageManager().setPage(ref, store, Page.None);
    } else if (!saved) {
      sendUpdate();
    }
  }

  private void initFromConfig(@Nonnull PortalConfigComponent config) {
    // Config is expected to be normalized (all fields non-null)
    this.currentType = config.getType();
    this.currentInteractionSoundEffectId = config.getInteractionSoundEffectId();
    this.currentCollisionInteraction = config.getCollisionInteraction();
    this.currentUseInteraction = config.getUseInteraction();
    this.currentServerHost = config.getServerHost();
    this.currentServerPort = config.getServerPort();
    this.currentWorldName = config.getWorldName();
    this.currentWorldX = config.getWorldX();
    this.currentWorldY = config.getWorldY();
    this.currentWorldZ = config.getWorldZ();
    this.currentWorldXText = formatCoordinate(currentWorldX);
    this.currentWorldYText = formatCoordinate(currentWorldY);
    this.currentWorldZText = formatCoordinate(currentWorldZ);

    this.currentCommands.clear();
    for (PortalConfigComponent.CommandEntry entry : config.getCommands()) {
      this.currentCommands.add(
          new PortalConfigComponent.CommandEntry(entry.getCommand(), entry.getCommandSender()));
    }
  }

  private void initMapMarkerData() {
    // Load existing map marker from the block ref
    BlockMapMarker existingMapMarker =
        blockRef.getStore().getComponent(blockRef, BlockMapMarker.getComponentType());
    this.currentMapMarkerName =
        existingMapMarker != null && existingMapMarker.getName() != null
            ? existingMapMarker.getName()
            : "";
    this.currentMapMarkerIcon =
        existingMapMarker != null && existingMapMarker.getIcon() != null
            ? existingMapMarker.getIcon()
            : "";
  }

  private void updateBlockMapMarker(
      @Nonnull Store<ChunkStore> blockStore,
      @Nullable String mapMarkerName,
      @Nullable String mapMarkerIcon) {
    // Get block position from BlockStateInfo
    final var blockInfo =
        blockStore.getComponent(blockRef, BlockModule.BlockStateInfo.getComponentType());
    if (blockInfo == null) {
      return;
    }

    final var chunkRef = blockInfo.getChunkRef();
    if (!chunkRef.isValid()) {
      return;
    }

    WorldChunk worldChunk = blockStore.getComponent(chunkRef, WorldChunk.getComponentType());
    if (worldChunk == null) {
      return;
    }

    // Calculate world position
    int blockIndex = blockInfo.getIndex();
    Vector3i blockPosition =
        new Vector3i(
            ChunkUtil.worldCoordFromLocalCoord(
                worldChunk.getX(), ChunkUtil.xFromBlockInColumn(blockIndex)),
            ChunkUtil.yFromBlockInColumn(blockIndex),
            ChunkUtil.worldCoordFromLocalCoord(
                worldChunk.getZ(), ChunkUtil.zFromBlockInColumn(blockIndex)));

    // Get the markers resource
    BlockMapMarkersResource resource =
        blockStore.getResource(BlockMapMarkersResource.getResourceType());

    boolean hasMarkerName = mapMarkerName != null && !mapMarkerName.trim().isEmpty();

    if (hasMarkerName) {
      // Use provided icon or default
      String icon =
          (mapMarkerIcon != null && !mapMarkerIcon.trim().isEmpty())
              ? mapMarkerIcon
              : DEFAULT_MARKER_ICON;

      // Add or update the BlockMapMarker component
      BlockMapMarker newMarker = new BlockMapMarker(mapMarkerName, icon);
      blockStore.putComponent(blockRef, BlockMapMarker.getComponentType(), newMarker);

      // Update the resource
      resource.removeMarker(blockPosition);
      resource.addMarker(blockPosition, mapMarkerName, icon);
    } else {
      // Remove the BlockMapMarker component if it exists
      BlockMapMarker existingMarker =
          blockStore.getComponent(blockRef, BlockMapMarker.getComponentType());
      if (existingMarker != null) {
        blockStore.removeComponent(blockRef, BlockMapMarker.getComponentType());
      }

      // Remove from the resource
      resource.removeMarker(blockPosition);
    }

    // Mark chunk as needing save
    worldChunk.markNeedsSaving();
  }

  private boolean handleCommandSave(PageData data) {
    // Filter to non-empty commands
    boolean hasNonEmptyCommand =
        currentCommands.stream()
            .anyMatch(entry -> entry.getCommand() != null && !entry.getCommand().isBlank());

    if (!hasNonEmptyCommand) {
      playerRef.sendMessage(
          Message.translation(MSG_CMD_NOT_SAVED)
              .param(Params.DETAIL, Message.translation(MSG_CMD_NOT_SAVED_DETAIL_CMD_MISSING))
              .param(Params.MOD_PREFIX, PREFIX));
      return false;
    }

    // Remove empty commands
    currentCommands.removeIf(entry -> entry.getCommand() == null || entry.getCommand().isBlank());

    PortalConfigComponent.CommandEntry[] commandsArray =
        currentCommands.toArray(new PortalConfigComponent.CommandEntry[0]);

    PortalConfigComponent newConfig =
        createConfig(
            data.type != null ? data.type : PortalConfigComponent.DEFAULT_TYPE,
            commandsArray,
            currentServerHost,
            currentServerPort,
            currentWorldName,
            currentWorldX,
            currentWorldY,
            currentWorldZ,
            data.interactionSoundEffectId,
            data.collisionInteraction,
            data.useInteraction);

    Store<ChunkStore> blockStore = blockRef.getStore();
    blockStore.putComponent(blockRef, PortalConfigComponent.getComponentType(), newConfig);

    // Handle BlockMapMarker
    updateBlockMapMarker(blockStore, data.mapMarkerName, data.mapMarkerIcon);

    playerRef.sendMessage(Message.translation(MSG_PORTAL_SAVED).param(Params.MOD_PREFIX, PREFIX));

    // Check for unknown placeholders in any command
    for (PortalConfigComponent.CommandEntry entry : currentCommands) {
      if (entry.getCommand() != null && !entry.getCommand().isEmpty()) {
        String missingPlaceholders =
            AdminPortalsPlugin.get()
                .getPlaceholderManager()
                .findMissingPlaceholders(entry.getCommand())
                .stream()
                .map(s -> String.format("{%s}", s))
                .collect(Collectors.joining());

        if (!missingPlaceholders.isEmpty()) {
          playerRef.sendMessage(
              Message.translation(MSG_UNKNOWN_PLACEHOLDERS)
                  .param(Params.MOD_PREFIX, PREFIX)
                  .param(Params.PLACEHOLDER_LIST, missingPlaceholders));
        }
      }
    }
    return true;
  }

  private boolean handleServerSave(PageData data) {
    String serverHost = data.serverHost != null ? data.serverHost.trim() : "";
    Integer serverPort = parseServerPort(data.serverPort);

    if (serverHost.isEmpty()) {
      playerRef.sendMessage(
          Message.translation(MSG_SERVER_NOT_SAVED)
              .param(Params.DETAIL, Message.translation(MSG_SERVER_NOT_SAVED_DETAIL_HOST_MISSING))
              .param(Params.MOD_PREFIX, PREFIX));
      return false;
    }

    if (serverPort == null || serverPort < 1 || serverPort > 65535) {
      playerRef.sendMessage(
          Message.translation(MSG_SERVER_NOT_SAVED)
              .param(Params.DETAIL, Message.translation(MSG_SERVER_NOT_SAVED_DETAIL_PORT_INVALID))
              .param(Params.MOD_PREFIX, PREFIX));
      return false;
    }

    currentServerHost = serverHost;
    currentServerPort = serverPort;

    PortalConfigComponent.CommandEntry[] commandsArray =
        currentCommands.toArray(new PortalConfigComponent.CommandEntry[0]);

    PortalConfigComponent newConfig =
        createConfig(
            PortalConfigComponent.Type.Server,
            commandsArray,
            currentServerHost,
            currentServerPort,
            currentWorldName,
            currentWorldX,
            currentWorldY,
            currentWorldZ,
            data.interactionSoundEffectId,
            data.collisionInteraction,
            data.useInteraction);

    Store<ChunkStore> blockStore = blockRef.getStore();
    blockStore.putComponent(blockRef, PortalConfigComponent.getComponentType(), newConfig);

    updateBlockMapMarker(blockStore, data.mapMarkerName, data.mapMarkerIcon);

    playerRef.sendMessage(Message.translation(MSG_PORTAL_SAVED).param(Params.MOD_PREFIX, PREFIX));
    return true;
  }

  private boolean handleWorldSave(PageData data) {
    String worldName = data.worldName != null ? data.worldName.trim() : "";
    String worldXText = normalizeInputText(data.worldX);
    String worldYText = normalizeInputText(data.worldY);
    String worldZText = normalizeInputText(data.worldZ);

    if (worldName.isEmpty()) {
      playerRef.sendMessage(
          Message.translation(MSG_WORLD_NOT_SAVED)
              .param(Params.DETAIL, Message.translation(MSG_WORLD_NOT_SAVED_DETAIL_NAME_MISSING))
              .param(Params.MOD_PREFIX, PREFIX));
      return false;
    }

    boolean hasAnyCoordinate =
        !worldXText.isEmpty() || !worldYText.isEmpty() || !worldZText.isEmpty();
    Double worldX = hasAnyCoordinate ? parseCoordinate(worldXText) : null;
    Double worldY = hasAnyCoordinate ? parseCoordinate(worldYText) : null;
    Double worldZ = hasAnyCoordinate ? parseCoordinate(worldZText) : null;

    if (hasAnyCoordinate && (worldX == null || worldY == null || worldZ == null)) {
      playerRef.sendMessage(
          Message.translation(MSG_WORLD_NOT_SAVED)
              .param(
                  Params.DETAIL,
                  Message.translation(MSG_WORLD_NOT_SAVED_DETAIL_COORDINATES_INVALID))
              .param(Params.MOD_PREFIX, PREFIX));
      return false;
    }

    currentWorldName = worldName;
    currentWorldX = worldX;
    currentWorldY = worldY;
    currentWorldZ = worldZ;
    currentWorldXText = hasAnyCoordinate ? worldXText : "";
    currentWorldYText = hasAnyCoordinate ? worldYText : "";
    currentWorldZText = hasAnyCoordinate ? worldZText : "";

    PortalConfigComponent.CommandEntry[] commandsArray =
        currentCommands.toArray(new PortalConfigComponent.CommandEntry[0]);

    PortalConfigComponent newConfig =
        createConfig(
            data.type != null ? data.type : currentType,
            commandsArray,
            currentServerHost,
            currentServerPort,
            currentWorldName,
            currentWorldX,
            currentWorldY,
            currentWorldZ,
            data.interactionSoundEffectId,
            data.collisionInteraction,
            data.useInteraction);

    Store<ChunkStore> blockStore = blockRef.getStore();
    blockStore.putComponent(blockRef, PortalConfigComponent.getComponentType(), newConfig);

    updateBlockMapMarker(blockStore, data.mapMarkerName, data.mapMarkerIcon);

    playerRef.sendMessage(Message.translation(MSG_PORTAL_SAVED).param(Params.MOD_PREFIX, PREFIX));
    return true;
  }

  @Nullable private static Integer parseServerPort(@Nullable String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Nullable private static Double parseCoordinate(@Nullable String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      double coordinate = Double.parseDouble(value.trim());
      return Double.isFinite(coordinate) ? coordinate : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String normalizeInputText(@Nullable String value) {
    return value != null ? value.trim() : "";
  }

  private static String formatCoordinate(@Nullable Double value) {
    return value != null ? value.toString() : "";
  }

  private static PortalConfigComponent createConfig(
      @Nonnull PortalConfigComponent.Type type,
      @Nonnull PortalConfigComponent.CommandEntry[] commands,
      @Nonnull String serverHost,
      int serverPort,
      @Nonnull String worldName,
      @Nullable Double worldX,
      @Nullable Double worldY,
      @Nullable Double worldZ,
      @Nullable String interactionSoundEffectId,
      boolean collisionInteraction,
      boolean useInteraction) {
    PortalConfigComponent config =
        new PortalConfigComponent(
            type,
            serverHost,
            serverPort,
            worldName,
            worldX,
            worldY,
            worldZ,
            null,
            null,
            commands,
            interactionSoundEffectId,
            collisionInteraction,
            useInteraction);
    return config;
  }

  @ToString
  public static class PageData {
    public static final BuilderCodec<PageData> CODEC;

    public String action;
    public PortalConfigComponent.Type type;
    public String command;
    public PortalConfigComponent.CommandSender commandSender;
    public String serverHost;
    public String serverPort;
    public String worldName;
    public String worldX;
    public String worldY;
    public String worldZ;
    public String mapMarkerName;
    public String mapMarkerIcon;
    public String interactionSoundEffectId;
    public boolean collisionInteraction;
    public boolean useInteraction;
    public String index;

    static {
      CODEC =
          BuilderCodec.builder(PageData.class, PageData::new)
              .append(
                  new KeyedCodec<>("Action", Codec.STRING), (o, i) -> o.action = i, o -> o.action)
              .add()
              .append(
                  new KeyedCodec<>(
                      "@Type",
                      new EnumCodec<>(
                          PortalConfigComponent.Type.class, EnumCodec.EnumStyle.CAMEL_CASE)),
                  (o, i) -> o.type = i,
                  o -> o.type)
              .add()
              .append(
                  new KeyedCodec<>("@Command", Codec.STRING),
                  (o, i) -> o.command = i,
                  o -> o.command)
              .add()
              .append(
                  new KeyedCodec<>(
                      "@CommandSender",
                      new EnumCodec<>(
                          PortalConfigComponent.CommandSender.class,
                          EnumCodec.EnumStyle.CAMEL_CASE)),
                  (o, i) -> o.commandSender = i,
                  o -> o.commandSender)
              .add()
              .append(
                  new KeyedCodec<>("@ServerHost", Codec.STRING),
                  (o, i) -> o.serverHost = i,
                  o -> o.serverHost)
              .add()
              .append(
                  new KeyedCodec<>("@ServerPort", Codec.STRING),
                  (o, i) -> o.serverPort = i,
                  o -> o.serverPort)
              .add()
              .append(
                  new KeyedCodec<>("@WorldName", Codec.STRING),
                  (o, i) -> o.worldName = i,
                  o -> o.worldName)
              .add()
              .append(
                  new KeyedCodec<>("@WorldX", Codec.STRING), (o, i) -> o.worldX = i, o -> o.worldX)
              .add()
              .append(
                  new KeyedCodec<>("@WorldY", Codec.STRING), (o, i) -> o.worldY = i, o -> o.worldY)
              .add()
              .append(
                  new KeyedCodec<>("@WorldZ", Codec.STRING), (o, i) -> o.worldZ = i, o -> o.worldZ)
              .add()
              .append(
                  new KeyedCodec<>("@MapMarkerName", Codec.STRING),
                  (o, i) -> o.mapMarkerName = i,
                  o -> o.mapMarkerName)
              .add()
              .append(
                  new KeyedCodec<>("@MapMarkerIcon", Codec.STRING),
                  (o, i) -> o.mapMarkerIcon = i,
                  o -> o.mapMarkerIcon)
              .add()
              .append(
                  new KeyedCodec<>("@InteractionSoundEffectId", Codec.STRING),
                  (o, i) -> o.interactionSoundEffectId = i,
                  o -> o.interactionSoundEffectId)
              .add()
              .append(
                  new KeyedCodec<>("@CollisionInteraction", Codec.BOOLEAN),
                  (o, i) -> o.collisionInteraction = i,
                  o -> o.collisionInteraction)
              .add()
              .append(
                  new KeyedCodec<>("@UseInteraction", Codec.BOOLEAN),
                  (o, i) -> o.useInteraction = i,
                  o -> o.useInteraction)
              .add()
              .append(new KeyedCodec<>("Index", Codec.STRING), (o, i) -> o.index = i, o -> o.index)
              .add()
              .build();
    }
  }
}
