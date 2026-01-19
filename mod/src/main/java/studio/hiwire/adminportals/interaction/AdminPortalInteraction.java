package studio.hiwire.adminportals.interaction;

import static studio.hiwire.adminportals.AdminPortalsPlugin.PREFIX;
import static studio.hiwire.adminportals.TranslationKeys.CHAT_MESSAGES;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
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
  private static final String MSG_NO_PERMISSION_VIEW =
      CHAT_MESSAGES + ".Configuration.Portal.View.NoPermission";

  @Override
  protected void interactWithBlock(
      @NonNullDecl World world,
      @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
      @NonNullDecl InteractionType interactionType,
      @NonNullDecl InteractionContext interactionContext,
      @NullableDecl ItemStack itemStack,
      @NonNullDecl Vector3i pos,
      @NonNullDecl CooldownHandler cooldownHandler) {

    // Necessary for now as Use interactions don't apply cooldowns
    if (interactionType == InteractionType.Use
        && checkHasAndApplyCooldown(interactionContext.getChain(), cooldownHandler)) {
      interactionContext.getState().state = InteractionState.Failed;
      return;
    }

    final var actorRef = interactionContext.getEntity();
    final var playerRef = commandBuffer.getComponent(actorRef, PlayerRef.getComponentType());
    final var isPlayer = playerRef != null;

    if (!isPlayer) {
      interactionContext.getState().state = InteractionState.Failed;
      return;
    }

    // Get the chunk containing this block
    WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
    if (chunk == null) {
      interactionContext.getState().state = InteractionState.Failed;
      return;
    }

    // Get the block entity reference for this specific block
    Ref<ChunkStore> blockEntityRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
    if (blockEntityRef == null) {
      playerRef.sendMessage(
          Message.translation(MSG_PORTAL_NOT_CONFIGURED).param(Params.MOD_PREFIX, PREFIX));
      interactionContext.getState().state = InteractionState.Failed;
      return;
    }

    // Get the config from the block entity (stored in chunk's store)
    final var chunkStore = chunk.getReference().getStore();
    final var portalConfig =
        chunkStore.getComponent(blockEntityRef, PortalConfigComponent.getComponentType());

    // Open configuration page on interact if the player is in configuration mode
    final var playerInConfigurationMode =
        AdminPortalsPlugin.get().getConfigurationModeManager().isInConfigurationMode(playerRef);
    if (interactionType == InteractionType.Use && playerInConfigurationMode) {
      interactionContext.getState().state = InteractionState.Skip;

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
        interactionContext.getState().state = InteractionState.Failed;
        return;
      }

      player.getPageManager().openCustomPage(actorRef, actorRef.getStore(), page);
      return;
    }

    if (portalConfig == null) {
      interactionContext.getState().state = InteractionState.Failed;
      playerRef.sendMessage(
          Message.translation(MSG_PORTAL_NOT_CONFIGURED).param(Params.MOD_PREFIX, PREFIX));
      return;
    }

    LOGGER.at(Level.FINE).log(
        "Portal config found: type=%s, command=%s, sender=%s",
        portalConfig.getType(), portalConfig.getCommand(), portalConfig.getCommandSender());

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
            portalConfig);

    if (portalConfig.getType() == PortalConfigComponent.Type.Command) {
      handleCommandAction(portalConfig, playerRef, placeholderContext);
      interactionContext.getState().state = InteractionState.Finished;
    } else {
      LOGGER.at(Level.WARNING).log("Unsupported portal type: %s", portalConfig.getType());
      interactionContext.getState().state = InteractionState.Failed;
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

  @NonNullDecl
  @Override
  public WaitForDataFrom getWaitForDataFrom() {
    // Wait for server interaction context state so
    // client plays the correct interact sound
    return WaitForDataFrom.Server;
  }

  private void handleCommandAction(
      PortalConfigComponent config, PlayerRef playerRef, PlaceholderContext context) {
    String processedCommand =
        AdminPortalsPlugin.get().getPlaceholderManager().process(config.getCommand(), context);

    switch (config.getCommandSender()) {
      case Server -> CommandManager.get().handleCommand(ConsoleSender.INSTANCE, processedCommand);
      case Player -> CommandManager.get().handleCommand(playerRef, processedCommand);
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
