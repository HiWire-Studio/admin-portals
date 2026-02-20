package studio.hiwire.adminportals.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortalConfigComponentTest {

  @Nested
  class Migrated {

    @Test
    void shouldMigrateLegacyCommandToCommandsArray() {
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              "legacy command",
              PortalConfigComponent.CommandSender.Player,
              null,
              "SFX_Custom_Sound");

      PortalConfigComponent migrated = config.migrated();

      assertNull(migrated.getCommand());
      assertNull(migrated.getCommandSender());
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
              PortalConfigComponent.Type.Command, "legacy command", null, null, null);

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
              null);

      PortalConfigComponent migrated = config.migrated();

      // Returns this — legacy fields are left for normalized() to clear
      assertSame(config, migrated);
      assertEquals(1, migrated.getCommands().length);
      assertEquals("new command", migrated.getCommands()[0].getCommand());
      assertEquals(
          PortalConfigComponent.CommandSender.Server, migrated.getCommands()[0].getCommandSender());
    }

    @Test
    void shouldReturnEmptyArrayWhenNoCommandsAndNoLegacy() {
      PortalConfigComponent config =
          new PortalConfigComponent(PortalConfigComponent.Type.Command, null, null, null, null);

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
          new PortalConfigComponent(PortalConfigComponent.Type.Command, null, null, commands, null);

      PortalConfigComponent migrated = config.migrated();

      assertSame(config, migrated);
    }
  }

  @Nested
  class Normalized {

    @Test
    void shouldUseDefaultsForAllNullFields() {
      PortalConfigComponent config = new PortalConfigComponent(null, null, null, null, null);

      PortalConfigComponent normalized = config.normalized();

      assertEquals(PortalConfigComponent.DEFAULT_TYPE, normalized.getType());
      assertNotNull(normalized.getCommands());
      assertEquals(0, normalized.getCommands().length);
      assertEquals(
          PortalConfigComponent.DEFAULT_INTERACTION_SOUND_EFFECT_ID,
          normalized.getInteractionSoundEffectId());
    }

    @Test
    void shouldPreserveEmptyInteractionSound() {
      PortalConfigComponent config =
          new PortalConfigComponent(PortalConfigComponent.Type.Command, null, null, null, "");

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
              PortalConfigComponent.Type.Command, null, null, commands, "SFX_Custom_Sound");

      PortalConfigComponent normalized = config.normalized();

      assertEquals(PortalConfigComponent.Type.Command, normalized.getType());
      assertEquals(1, normalized.getCommands().length);
      assertEquals("test command", normalized.getCommands()[0].getCommand());
      assertEquals("SFX_Custom_Sound", normalized.getInteractionSoundEffectId());
    }
  }

  @Nested
  class MigratedAndNormalized {

    @Test
    void shouldMigrateLegacyAndApplyDefaults() {
      PortalConfigComponent config = new PortalConfigComponent(null, "my command", null, null, "");

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
      PortalConfigComponent config = new PortalConfigComponent(null, null, null, null, null);

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
}
