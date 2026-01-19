package studio.hiwire.adminportals.placeholder;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import studio.hiwire.adminportals.component.PortalConfigComponent;

public record PlaceholderContext(
    @NonNullDecl World world,
    @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
    @NonNullDecl InteractionType interactionType,
    @NonNullDecl InteractionContext interactionContext,
    @NullableDecl ItemStack itemStack,
    @NonNullDecl Vector3i pos,
    @NonNullDecl CooldownHandler cooldownHandler,
    @NonNullDecl PlayerRef playerRef,
    @NonNullDecl PortalConfigComponent portalConfig) {}
