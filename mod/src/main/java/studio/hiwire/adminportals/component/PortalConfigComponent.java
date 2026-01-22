package studio.hiwire.adminportals.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import studio.hiwire.adminportals.AdminPortalsPlugin;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PortalConfigComponent implements Component<ChunkStore> {

  public static final BuilderCodec<PortalConfigComponent> CODEC;

  public static final Type DEFAULT_TYPE = Type.Command;
  public static final String DEFAULT_COMMAND = "";
  public static final CommandSender DEFAULT_COMMAND_SENDER = CommandSender.Server;
  public static final String DEFAULT_INTERACTION_SOUND_EFFECT_ID =
      "SFX_Portal_Neutral_Teleport_Local";

  private Type type;
  private String command;
  private CommandSender commandSender;
  private String interactionSoundEffectId;

  /**
   * Returns a new component with all null fields replaced by their default values. Empty strings
   * are preserved (e.g., empty interactionSoundEffectId means no sound).
   */
  public PortalConfigComponent normalized() {
    return new PortalConfigComponent(
        type != null ? type : DEFAULT_TYPE,
        command != null ? command : DEFAULT_COMMAND,
        commandSender != null ? commandSender : DEFAULT_COMMAND_SENDER,
        interactionSoundEffectId != null
            ? interactionSoundEffectId
            : DEFAULT_INTERACTION_SOUND_EFFECT_ID);
  }

  @NullableDecl
  @Override
  public Component<ChunkStore> clone() {
    return new PortalConfigComponent(
        this.type, this.command, this.commandSender, this.interactionSoundEffectId);
  }

  public static ComponentType<ChunkStore, PortalConfigComponent> getComponentType() {
    return AdminPortalsPlugin.get().getAdminPortalConfigComponentType();
  }

  public enum Type {
    Command
  }

  @RequiredArgsConstructor
  @Getter
  public enum CommandSender {
    Server("Server"),
    Player("Player");

    private final String uiDisplayName;
  }

  static {
    CODEC =
        BuilderCodec.builder(PortalConfigComponent.class, PortalConfigComponent::new)
            .append(
                new KeyedCodec<>(
                    "Type", new EnumCodec<>(Type.class, EnumCodec.EnumStyle.CAMEL_CASE)),
                (o, i) -> o.type = i,
                o -> o.type)
            .add()
            .append(
                new KeyedCodec<>("Command", Codec.STRING), (o, i) -> o.command = i, o -> o.command)
            .add()
            .append(
                new KeyedCodec<>(
                    "CommandSender",
                    new EnumCodec<>(CommandSender.class, EnumCodec.EnumStyle.CAMEL_CASE)),
                (o, i) -> o.commandSender = i,
                o -> o.commandSender)
            .add()
            .append(
                new KeyedCodec<>("InteractionSoundEffectId", Codec.STRING),
                (o, i) -> o.interactionSoundEffectId = i,
                o -> o.interactionSoundEffectId)
            .add()
            .build();
  }
}
