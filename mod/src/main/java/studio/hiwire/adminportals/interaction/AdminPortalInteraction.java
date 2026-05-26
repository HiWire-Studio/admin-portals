package studio.hiwire.adminportals.interaction;

import static studio.hiwire.adminportals.AdminPortalsPlugin.PREFIX;
import static studio.hiwire.adminportals.TranslationKeys.CHAT_MESSAGES;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Arrays;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.joml.Vector3d;
import org.joml.Vector3i;
import studio.hiwire.adminportals.AdminPortalsPlugin;
import studio.hiwire.adminportals.Permissions;
import studio.hiwire.adminportals.TranslationKeys.Params;
import studio.hiwire.adminportals.component.PortalConfigComponent;
import studio.hiwire.adminportals.page.PortalConfigurationPage;
import studio.hiwire.adminportals.placeholder.PlaceholderContext;

public class AdminPortalInteraction extends SimpleBlockInteraction {

  public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  public static final BuilderCodec<AdminPortalInteraction> CODEC;

  // Message IDs
  private static final String MSG_PORTAL_NOT_CONFIGURED =
      CHAT_MESSAGES + ".Interaction.Portal.NotConfigured";
  private static final String MSG_SERVER_INVALID_CONFIG =
      CHAT_MESSAGES + ".Interaction.Portal.Server.InvalidConfig";
  private static final String MSG_SERVER_TRANSFER_FAILED =
      CHAT_MESSAGES + ".Interaction.Portal.Server.TransferFailed";
  private static final String MSG_WORLD_INVALID_CONFIG =
      CHAT_MESSAGES + ".Interaction.Portal.World.InvalidConfig";
  private static final String MSG_WORLD_INCOMPLETE_COORDINATES =
      CHAT_MESSAGES + ".Interaction.Portal.World.IncompleteCoordinates";
  private static final String MSG_WORLD_NOT_FOUND =
      CHAT_MESSAGES + ".Interaction.Portal.World.NotFound";
  private static final String MSG_WORLD_NOT_IN_WORLD =
      CHAT_MESSAGES + ".Interaction.Portal.World.NotInWorld";
  private static final String MSG_WORLD_TELEPORT_FAILED =
      CHAT_MESSAGES + ".Interaction.Portal.World.TeleportFailed";
  private static final String MSG_NO_PERMISSION_VIEW =
      CHAT_MESSAGES + ".Configuration.Portal.View.NoPermission";
  private static final String MSG_BLOCK_ENTITY_MISSING =
      CHAT_MESSAGES + ".Interaction.Portal.BlockEntityMissing";

  @Override
  protected void interactWithBlock(
      @NonNullDecl World world,
      @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
      @NonNullDecl InteractionType interactionType,
      @NonNullDecl InteractionContext interactionContext,
      @NullableDecl ItemStack itemStack,
      @NonNullDecl Vector3i pos,
      @NonNullDecl CooldownHandler cooldownHandler) {
    final var actorRef = interactionContext.getEntity();
    final var playerRef = commandBuffer.getComponent(actorRef, PlayerRef.getComponentType());
    final var isPlayer = playerRef != null;

    if (!isPlayer) {
      return;
    }

    final var playerInConfigurationMode =
        AdminPortalsPlugin.get().getConfigurationModeManager().isInConfigurationMode(playerRef);

    // Necessary for now as Use interactions don't apply cooldowns
    if (interactionType == InteractionType.Use
        && checkHasAndApplyCooldown(interactionContext.getChain(), cooldownHandler)
        && !playerInConfigurationMode) {
      return;
    }

    // Get the chunk containing this block
    WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
    if (chunk == null) {
      return;
    }

    // Get the block entity reference for this specific block
    Ref<ChunkStore> blockEntityRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
    if (blockEntityRef == null) {
      LOGGER.at(Level.WARNING).log(
          "Block entity missing for portal at position %s - this is a bug, please report it with"
              + " the server error if it is nearby",
          pos);
      playerRef.sendMessage(
          Message.translation(MSG_BLOCK_ENTITY_MISSING).param(Params.MOD_PREFIX, PREFIX));
      return;
    }

    // Get the config from the block entity (stored in chunk's store)
    final var chunkStore = chunk.getReference().getStore();
    final var portalConfig =
        chunkStore.getComponent(blockEntityRef, PortalConfigComponent.getComponentType());

    // Open configuration page on interact if the player is in configuration mode
    if (interactionType == InteractionType.Use && playerInConfigurationMode) {
      // Check if player has permission to view the configuration UI
      if (!PermissionsModule.get()
          .hasPermission(playerRef.getUuid(), Permissions.PORTAL_CONFIG_VIEW)) {
        playerRef.sendMessage(
            Message.translation(MSG_NO_PERMISSION_VIEW)
                .param(Params.MOD_PREFIX, PREFIX)
                .param(Params.PERMISSION, Permissions.PORTAL_CONFIG_VIEW));
        return;
      }

      final var page = new PortalConfigurationPage(playerRef, blockEntityRef, portalConfig);
      final var player = commandBuffer.getComponent(actorRef, Player.getComponentType());
      if (player == null) {
        LOGGER.at(Level.WARNING).log("Player not found for interaction");
        return;
      }

      player.getPageManager().openCustomPage(actorRef, actorRef.getStore(), page);
      return;
    }

    if (portalConfig == null) {
      playerRef.sendMessage(
          Message.translation(MSG_PORTAL_NOT_CONFIGURED).param(Params.MOD_PREFIX, PREFIX));
      return;
    }

    // Migrate legacy fields and normalize to ensure all fields have valid values
    final var config = portalConfig.migrated().normalized();

    if (interactionType == InteractionType.Collision && !config.getCollisionInteraction()) {
      return;
    }

    if (interactionType == InteractionType.Use && !config.getUseInteraction()) {
      return;
    }

    LOGGER.at(Level.FINE).log(
        "Portal config found: type=%s, commands=%s",
        config.getType(), Arrays.toString(config.getCommands()));

    PlaceholderContext placeholderContext =
        new PlaceholderContext(
            world,
            commandBuffer,
            interactionType,
            interactionContext,
            itemStack,
            pos,
            cooldownHandler,
            playerRef,
            config);

    if (config.getType() == PortalConfigComponent.Type.Command) {
      handleCommandAction(config, playerRef, placeholderContext);
      playTeleportSound(actorRef, commandBuffer, config.getInteractionSoundEffectId());
    } else if (config.getType() == PortalConfigComponent.Type.Server) {
      if (handleServerAction(config, playerRef)) {
        playTeleportSound(actorRef, commandBuffer, config.getInteractionSoundEffectId());
      }
    } else if (config.getType() == PortalConfigComponent.Type.World) {
      if (handleWorldAction(config, playerRef, actorRef, commandBuffer)) {
        playTeleportSound(actorRef, commandBuffer, config.getInteractionSoundEffectId());
      }
    } else {
      LOGGER.at(Level.WARNING).log("Unsupported portal type: %s", config.getType());
    }
  }

  private boolean checkHasAndApplyCooldown(
      InteractionChain chain, @NonNullDecl CooldownHandler cooldownHandler) {
    if (chain == null) {
      return false;
    }

    // Check root-level cooldown if configured
    // For Use interactions, the root-level cooldown is not automatically checked
    // (unlike Collision which goes through executeChain0), so we handle it manually.
    final var rootInteraction = chain.getRootInteraction();
    final var cooldownConfig = rootInteraction.getCooldown();

    if (cooldownConfig != null) {
      String cooldownId =
          cooldownConfig.cooldownId != null ? cooldownConfig.cooldownId : rootInteraction.getId();
      float cooldownTime = cooldownConfig.cooldown;
      float[] chargeTimes =
          cooldownConfig.chargeTimes != null
              ? cooldownConfig.chargeTimes
              : new float[] {cooldownTime};
      boolean interruptRecharge = cooldownConfig.interruptRecharge;

      final var forceCooldownCreation = true;
      var cooldown =
          cooldownHandler.getCooldown(
              cooldownId, cooldownTime, chargeTimes, forceCooldownCreation, interruptRecharge);
      // cooldown cannot be null
      return cooldown.hasCooldown(true);
    }

    return false;
  }

  @Override
  protected void simulateInteractWithBlock(
      @NonNullDecl InteractionType interactionType,
      @NonNullDecl InteractionContext interactionContext,
      @NullableDecl ItemStack itemStack,
      @NonNullDecl World world,
      @NonNullDecl Vector3i vector3i) {}

  private void handleCommandAction(
      PortalConfigComponent config, PlayerRef playerRef, PlaceholderContext context) {
    for (PortalConfigComponent.CommandEntry entry : config.getCommands()) {
      if (entry.getCommand() == null || entry.getCommand().isBlank()) {
        continue;
      }

      String processedCommand =
          AdminPortalsPlugin.get().getPlaceholderManager().process(entry.getCommand(), context);

      switch (entry.getCommandSender()) {
        case Server -> CommandManager.get().handleCommand(ConsoleSender.INSTANCE, processedCommand);
        case Player -> CommandManager.get().handleCommand(playerRef, processedCommand);
      }
    }
  }

  private boolean handleServerAction(PortalConfigComponent config, PlayerRef playerRef) {
    String host = config.getServerHost() != null ? config.getServerHost().trim() : "";
    Integer port = config.getServerPort();

    if (host.isBlank() || port == null || port < 1 || port > 65535) {
      playerRef.sendMessage(
          Message.translation(MSG_SERVER_INVALID_CONFIG)
              .param(Params.MOD_PREFIX, PREFIX)
              .param(Params.HOST, host)
              .param(Params.PORT, port != null ? port : 0));
      return false;
    }

    try {
      playerRef.referToServer(host, port);
      return true;
    } catch (RuntimeException e) {
      LOGGER.at(Level.WARNING).withCause(e).log(
          "Failed to transfer player %s to server %s:%s", playerRef.getUuid(), host, port);
      playerRef.sendMessage(
          Message.translation(MSG_SERVER_TRANSFER_FAILED)
              .param(Params.MOD_PREFIX, PREFIX)
              .param(Params.HOST, host)
              .param(Params.PORT, port)
              .param(
                  Params.ERROR,
                  e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
      return false;
    }
  }

  private boolean handleWorldAction(
      PortalConfigComponent config,
      PlayerRef playerRef,
      Ref<EntityStore> actorRef,
      CommandBuffer<EntityStore> commandBuffer) {
    if (!validateWorldConfig(config, playerRef)) {
      return false;
    }

    Player playerComponent = commandBuffer.getComponent(actorRef, Player.getComponentType());
    if (!validateWorldPlayerReady(playerComponent, playerRef)) {
      return false;
    }

    if (commandBuffer.getComponent(actorRef, Teleport.getComponentType()) != null
        || commandBuffer.getComponent(actorRef, PendingTeleport.getComponentType()) != null) {
      playerRef.sendMessage(
          Message.translation(MSG_WORLD_TELEPORT_FAILED)
              .param(Params.MOD_PREFIX, PREFIX)
              .param(Params.ERROR, "Teleport already pending"));
      return false;
    }

    String worldName = normalizedWorldName(config);
    World targetWorld = Universe.get().getWorld(worldName);
    if (targetWorld == null) {
      playerRef.sendMessage(
          Message.translation(MSG_WORLD_NOT_FOUND)
              .param(Params.MOD_PREFIX, PREFIX)
              .param(Params.WORLD_NAME, worldName));
      return false;
    }

    try {
      Teleport teleport;
      if (hasAnyCoordinate(config)) {
        teleport =
            Teleport.createForPlayer(
                targetWorld,
                new Vector3d(config.getWorldX(), config.getWorldY(), config.getWorldZ()),
                Rotation3f.IDENTITY);
      } else {
        var spawnProvider = targetWorld.getWorldConfig().getSpawnProvider();
        Transform spawnPoint =
            spawnProvider != null
                ? spawnProvider.getSpawnPoint(targetWorld, playerRef.getUuid())
                : new Transform();
        teleport = Teleport.createForPlayer(targetWorld, spawnPoint);
      }

      commandBuffer.addComponent(actorRef, Teleport.getComponentType(), teleport);
      return true;
    } catch (RuntimeException e) {
      LOGGER.at(Level.WARNING).withCause(e).log(
          "Failed to teleport player %s to world %s", playerRef.getUuid(), worldName);
      playerRef.sendMessage(
          Message.translation(MSG_WORLD_TELEPORT_FAILED)
              .param(Params.MOD_PREFIX, PREFIX)
              .param(
                  Params.ERROR,
                  e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
      return false;
    }
  }

  static boolean validateWorldConfig(PortalConfigComponent config, PlayerRef playerRef) {
    String worldName = normalizedWorldName(config);
    if (worldName.isBlank()) {
      playerRef.sendMessage(
          Message.translation(MSG_WORLD_INVALID_CONFIG).param(Params.MOD_PREFIX, PREFIX));
      return false;
    }

    if (hasAnyCoordinate(config) && !hasAllCoordinates(config)) {
      playerRef.sendMessage(
          Message.translation(MSG_WORLD_INCOMPLETE_COORDINATES)
              .param(Params.MOD_PREFIX, PREFIX)
              .param(Params.WORLD_NAME, worldName)
              .param(Params.X, formatCoordinate(config.getWorldX()))
              .param(Params.Y, formatCoordinate(config.getWorldY()))
              .param(Params.Z, formatCoordinate(config.getWorldZ())));
      return false;
    }

    return true;
  }

  static boolean validateWorldPlayerReady(
      @NullableDecl Player playerComponent, PlayerRef playerRef) {
    if (playerComponent == null) {
      playerRef.sendMessage(
          Message.translation(MSG_WORLD_NOT_IN_WORLD).param(Params.MOD_PREFIX, PREFIX));
      return false;
    }

    if (playerComponent.isWaitingForClientReady()) {
      playerRef.sendMessage(
          Message.translation(MSG_WORLD_TELEPORT_FAILED)
              .param(Params.MOD_PREFIX, PREFIX)
              .param(Params.ERROR, "Player client is not ready"));
      return false;
    }

    return true;
  }

  static boolean hasAnyCoordinate(PortalConfigComponent config) {
    return config.getWorldX() != null || config.getWorldY() != null || config.getWorldZ() != null;
  }

  private static boolean hasAllCoordinates(PortalConfigComponent config) {
    return config.getWorldX() != null && config.getWorldY() != null && config.getWorldZ() != null;
  }

  private static String normalizedWorldName(PortalConfigComponent config) {
    return config.getWorldName() != null ? config.getWorldName().trim() : "";
  }

  private static String formatCoordinate(Double coordinate) {
    return coordinate != null ? coordinate.toString() : "";
  }

  private void playTeleportSound(
      Ref<EntityStore> actorRef,
      CommandBuffer<EntityStore> commandBuffer,
      String interactionSoundEffectId) {
    int soundEventIndex = SoundEvent.getAssetMap().getIndex(interactionSoundEffectId);
    if (soundEventIndex >= 0) {
      SoundUtil.playSoundEvent2d(
          actorRef, soundEventIndex, SoundCategory.SFX, commandBuffer.getStore());
    }
  }

  static {
    CODEC =
        BuilderCodec.builder(
                AdminPortalInteraction.class,
                AdminPortalInteraction::new,
                SimpleBlockInteraction.CODEC)
            .build();
  }
}
