package studio.hiwire.adminportals.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hypixel.hytale.codec.ExtraInfo;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortalConfigComponentTest {

  @Nested
  class Migrated {

    @Test
    void shouldMigrateLegacyCommandToCommandsArray() {
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Server,
              "play.example.net",
              5520,
              "legacy command",
              PortalConfigComponent.CommandSender.Player,
              null,
              "SFX_Custom_Sound",
              null,
              null);

      PortalConfigComponent migrated = config.migrated();

      assertNull(migrated.getCommand());
      assertNull(migrated.getCommandSender());
      assertEquals(PortalConfigComponent.Type.Server, migrated.getType());
      assertEquals("play.example.net", migrated.getServerHost());
      assertEquals(5520, migrated.getServerPort());
      assertEquals(PortalConfigComponent.DEFAULT_WORLD_NAME, migrated.getWorldName());
      assertNull(migrated.getWorldX());
      assertNull(migrated.getWorldY());
      assertNull(migrated.getWorldZ());
      assertNotNull(migrated.getCommands());
      assertEquals(1, migrated.getCommands().length);
      assertEquals("legacy command", migrated.getCommands()[0].getCommand());
      assertEquals(
          PortalConfigComponent.CommandSender.Player, migrated.getCommands()[0].getCommandSender());
      assertEquals("SFX_Custom_Sound", migrated.getInteractionSoundEffectId());
    }

    @Test
    void shouldDefaultSenderWhenNull() {
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command, "legacy command", null, null, null, null, null);

      PortalConfigComponent migrated = config.migrated();

      assertEquals(1, migrated.getCommands().length);
      assertEquals("legacy command", migrated.getCommands()[0].getCommand());
      assertEquals(
          PortalConfigComponent.DEFAULT_COMMAND_SENDER,
          migrated.getCommands()[0].getCommandSender());
    }

    @Test
    void shouldPreferCommandsArrayOverLegacyCommand() {
      PortalConfigComponent.CommandEntry[] commands =
          new PortalConfigComponent.CommandEntry[] {
            new PortalConfigComponent.CommandEntry(
                "new command", PortalConfigComponent.CommandSender.Server)
          };
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              "legacy command",
              PortalConfigComponent.CommandSender.Player,
              commands,
              null,
              null,
              null);

      PortalConfigComponent migrated = config.migrated();

      // Returns this; legacy fields are left for normalized() to clear.
      assertSame(config, migrated);
      assertEquals(1, migrated.getCommands().length);
      assertEquals("new command", migrated.getCommands()[0].getCommand());
      assertEquals(
          PortalConfigComponent.CommandSender.Server, migrated.getCommands()[0].getCommandSender());
    }

    @Test
    void shouldReturnEmptyArrayWhenNoCommandsAndNoLegacy() {
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command, null, null, null, null, null, null);

      PortalConfigComponent migrated = config.migrated();

      assertNotNull(migrated.getCommands());
      assertEquals(0, migrated.getCommands().length);
    }

    @Test
    void shouldReturnEmptyArrayWhenEmptyLegacyCommand() {
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              "",
              PortalConfigComponent.CommandSender.Server,
              null,
              null,
              null,
              null);

      PortalConfigComponent migrated = config.migrated();

      assertNull(migrated.getCommand());
      assertNull(migrated.getCommandSender());
      assertNotNull(migrated.getCommands());
      assertEquals(0, migrated.getCommands().length);
    }

    @Test
    void shouldNotMutateOriginal() {
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              "legacy command",
              PortalConfigComponent.CommandSender.Player,
              null,
              null,
              null,
              null);

      PortalConfigComponent migrated = config.migrated();

      // Original is untouched
      assertEquals("legacy command", config.getCommand());
      assertEquals(PortalConfigComponent.CommandSender.Player, config.getCommandSender());
      assertNull(config.getCommands());
      // Migrated is a different instance
      assertNotSame(config, migrated);
    }

    @Test
    void shouldReturnSameInstanceWhenCommandsAlreadyPopulated() {
      PortalConfigComponent.CommandEntry[] commands =
          new PortalConfigComponent.CommandEntry[] {
            new PortalConfigComponent.CommandEntry(
                "cmd", PortalConfigComponent.CommandSender.Server)
          };
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command, null, null, commands, null, null, null);

      PortalConfigComponent migrated = config.migrated();

      assertSame(config, migrated);
    }
  }

  @Nested
  class Codec {

    @Test
    void shouldRoundTripWorldWithBlankCoordinates() {
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.World,
              "play.example.net",
              5520,
              "adventure",
              null,
              null,
              null,
              null,
              null,
              PortalConfigComponent.DEFAULT_COMMANDS,
              "",
              true,
              true);

      BsonDocument encoded = PortalConfigComponent.CODEC.encode(config, new ExtraInfo());
      PortalConfigComponent decoded =
          PortalConfigComponent.CODEC.decode(encoded, new ExtraInfo()).migrated().normalized();

      assertFalse(encoded.containsKey("WorldX"));
      assertFalse(encoded.containsKey("WorldY"));
      assertFalse(encoded.containsKey("WorldZ"));
      assertEquals(PortalConfigComponent.Type.World, decoded.getType());
      assertEquals("adventure", decoded.getWorldName());
      assertNull(decoded.getWorldX());
      assertNull(decoded.getWorldY());
      assertNull(decoded.getWorldZ());
    }

    @Test
    void shouldDecodeLegacyDocumentMissingServerAndWorldFields() {
      BsonDocument document = new BsonDocument();
      document.put("Type", new BsonString("Command"));
      document.put("Command", new BsonString("legacy command"));
      document.put("CommandSender", new BsonString("Server"));

      PortalConfigComponent decoded =
          PortalConfigComponent.CODEC.decode(document, new ExtraInfo()).migrated().normalized();

      assertEquals(PortalConfigComponent.Type.Command, decoded.getType());
      assertEquals(PortalConfigComponent.DEFAULT_SERVER_HOST, decoded.getServerHost());
      assertEquals(PortalConfigComponent.DEFAULT_SERVER_PORT, decoded.getServerPort());
      assertEquals(PortalConfigComponent.DEFAULT_WORLD_NAME, decoded.getWorldName());
      assertNull(decoded.getWorldX());
      assertNull(decoded.getWorldY());
      assertNull(decoded.getWorldZ());
      assertEquals(1, decoded.getCommands().length);
      assertEquals("legacy command", decoded.getCommands()[0].getCommand());
      assertEquals(
          PortalConfigComponent.CommandSender.Server, decoded.getCommands()[0].getCommandSender());
    }
  }

  @Nested
  class Normalized {

    @Test
    void shouldUseDefaultsForAllNullFields() {
      PortalConfigComponent config =
          new PortalConfigComponent(null, null, null, null, null, null, null);

      PortalConfigComponent normalized = config.normalized();

      assertEquals(PortalConfigComponent.DEFAULT_TYPE, normalized.getType());
      assertEquals(PortalConfigComponent.DEFAULT_SERVER_HOST, normalized.getServerHost());
      assertEquals(PortalConfigComponent.DEFAULT_SERVER_PORT, normalized.getServerPort());
      assertEquals(PortalConfigComponent.DEFAULT_WORLD_NAME, normalized.getWorldName());
      assertNull(normalized.getWorldX());
      assertNull(normalized.getWorldY());
      assertNull(normalized.getWorldZ());
      assertNotNull(normalized.getCommands());
      assertEquals(0, normalized.getCommands().length);
      assertEquals(
          PortalConfigComponent.DEFAULT_INTERACTION_SOUND_EFFECT_ID,
          normalized.getInteractionSoundEffectId());
      assertEquals(
          PortalConfigComponent.DEFAULT_COLLISION_INTERACTION,
          normalized.getCollisionInteraction());
      assertEquals(PortalConfigComponent.DEFAULT_USE_INTERACTION, normalized.getUseInteraction());
    }

    @Test
    void shouldPreserveEmptyInteractionSound() {
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command, null, null, null, "", null, null);

      PortalConfigComponent normalized = config.normalized();

      assertEquals("", normalized.getInteractionSoundEffectId());
    }

    @Test
    void shouldPreserveExistingValues() {
      PortalConfigComponent.CommandEntry[] commands =
          new PortalConfigComponent.CommandEntry[] {
            new PortalConfigComponent.CommandEntry(
                "test command", PortalConfigComponent.CommandSender.Player)
          };
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              "play.example.net",
              5520,
              null,
              null,
              commands,
              "SFX_Custom_Sound",
              null,
              null);

      PortalConfigComponent normalized = config.normalized();

      assertEquals(PortalConfigComponent.Type.Command, normalized.getType());
      assertEquals("play.example.net", normalized.getServerHost());
      assertEquals(5520, normalized.getServerPort());
      assertEquals(PortalConfigComponent.DEFAULT_WORLD_NAME, normalized.getWorldName());
      assertEquals(1, normalized.getCommands().length);
      assertEquals("test command", normalized.getCommands()[0].getCommand());
      assertEquals("SFX_Custom_Sound", normalized.getInteractionSoundEffectId());
    }

    @Test
    void shouldPreserveWorldValues() {
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.World,
              "play.example.net",
              5520,
              "adventure",
              1.5,
              64.0,
              -12.25,
              null,
              null,
              PortalConfigComponent.DEFAULT_COMMANDS,
              "SFX_Custom_Sound",
              true,
              false);

      PortalConfigComponent normalized = config.normalized();

      assertEquals(PortalConfigComponent.Type.World, normalized.getType());
      assertEquals("adventure", normalized.getWorldName());
      assertEquals(1.5, normalized.getWorldX());
      assertEquals(64.0, normalized.getWorldY());
      assertEquals(-12.25, normalized.getWorldZ());
    }

    @Test
    void shouldDefaultNullServerFields() {
      PortalConfigComponent config =
          new PortalConfigComponent(null, null, null, "", null, null, null, null, null);

      PortalConfigComponent normalized = config.normalized();

      assertEquals(PortalConfigComponent.DEFAULT_SERVER_HOST, normalized.getServerHost());
      assertEquals(PortalConfigComponent.DEFAULT_SERVER_PORT, normalized.getServerPort());
    }

    @Test
    void shouldDefaultNullCollisionInteraction() {
      PortalConfigComponent config =
          new PortalConfigComponent(null, null, null, null, null, null, null);

      PortalConfigComponent normalized = config.normalized();

      assertEquals(
          PortalConfigComponent.DEFAULT_COLLISION_INTERACTION,
          normalized.getCollisionInteraction());
    }

    @Test
    void shouldPreserveExplicitTrueCollisionInteraction() {
      PortalConfigComponent config =
          new PortalConfigComponent(null, null, null, null, null, true, null);

      PortalConfigComponent normalized = config.normalized();

      assertTrue(normalized.getCollisionInteraction());
    }

    @Test
    void shouldPreserveExplicitFalseCollisionInteraction() {
      PortalConfigComponent config =
          new PortalConfigComponent(null, null, null, null, null, false, null);

      PortalConfigComponent normalized = config.normalized();

      assertFalse(normalized.getCollisionInteraction());
    }

    @Test
    void shouldDefaultNullUseInteraction() {
      PortalConfigComponent config =
          new PortalConfigComponent(null, null, null, null, null, null, null);

      PortalConfigComponent normalized = config.normalized();

      assertEquals(PortalConfigComponent.DEFAULT_USE_INTERACTION, normalized.getUseInteraction());
    }

    @Test
    void shouldPreserveExplicitTrueUseInteraction() {
      PortalConfigComponent config =
          new PortalConfigComponent(null, null, null, null, null, null, true);

      PortalConfigComponent normalized = config.normalized();

      assertTrue(normalized.getUseInteraction());
    }

    @Test
    void shouldPreserveExplicitFalseUseInteraction() {
      PortalConfigComponent config =
          new PortalConfigComponent(null, null, null, null, null, null, false);

      PortalConfigComponent normalized = config.normalized();

      assertFalse(normalized.getUseInteraction());
    }
  }

  @Nested
  class MigratedAndNormalized {

    @Test
    void shouldMigrateLegacyAndApplyDefaults() {
      PortalConfigComponent config =
          new PortalConfigComponent(null, "my command", null, null, "", null, null);

      PortalConfigComponent result = config.migrated().normalized();

      assertEquals(PortalConfigComponent.DEFAULT_TYPE, result.getType());
      assertNull(result.getCommand());
      assertNull(result.getCommandSender());
      assertEquals(1, result.getCommands().length);
      assertEquals("my command", result.getCommands()[0].getCommand());
      assertEquals(
          PortalConfigComponent.DEFAULT_COMMAND_SENDER, result.getCommands()[0].getCommandSender());
      assertEquals("", result.getInteractionSoundEffectId());
    }

    @Test
    void shouldHandleAllNullFields() {
      PortalConfigComponent config =
          new PortalConfigComponent(null, null, null, null, null, null, null);

      PortalConfigComponent result = config.migrated().normalized();

      assertEquals(PortalConfigComponent.DEFAULT_TYPE, result.getType());
      assertNull(result.getCommand());
      assertNull(result.getCommandSender());
      assertNotNull(result.getCommands());
      assertEquals(0, result.getCommands().length);
      assertEquals(
          PortalConfigComponent.DEFAULT_INTERACTION_SOUND_EFFECT_ID,
          result.getInteractionSoundEffectId());
    }
  }

  @Nested
  class Clone {

    @Test
    void shouldPreserveWorldValues() {
      PortalConfigComponent.CommandEntry[] commands =
          new PortalConfigComponent.CommandEntry[] {
            new PortalConfigComponent.CommandEntry(
                "test command", PortalConfigComponent.CommandSender.Player)
          };
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.World,
              "play.example.net",
              5520,
              "adventure",
              1.5,
              64.0,
              -12.25,
              null,
              null,
              commands,
              "SFX_Custom_Sound",
              true,
              false);

      PortalConfigComponent cloned = (PortalConfigComponent) config.clone();

      assertNotSame(config, cloned);
      assertEquals(PortalConfigComponent.Type.World, cloned.getType());
      assertEquals("adventure", cloned.getWorldName());
      assertEquals(1.5, cloned.getWorldX());
      assertEquals(64.0, cloned.getWorldY());
      assertEquals(-12.25, cloned.getWorldZ());
      assertEquals(1, cloned.getCommands().length);
      assertNotSame(config.getCommands()[0], cloned.getCommands()[0]);
      assertEquals("test command", cloned.getCommands()[0].getCommand());
    }
  }
}
