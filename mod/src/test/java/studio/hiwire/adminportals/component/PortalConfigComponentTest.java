package studio.hiwire.adminportals.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PortalConfigComponentTest {

  @Test
  void normalizedShouldUseDefaultsForAllNullFields() {
    PortalConfigComponent config = new PortalConfigComponent(null, null, null, null);

    PortalConfigComponent normalized = config.normalized();

    assertEquals(PortalConfigComponent.DEFAULT_TYPE, normalized.getType());
    assertEquals(PortalConfigComponent.DEFAULT_COMMAND, normalized.getCommand());
    assertEquals(PortalConfigComponent.DEFAULT_COMMAND_SENDER, normalized.getCommandSender());
    assertEquals(
        PortalConfigComponent.DEFAULT_INTERACTION_SOUND_EFFECT_ID,
        normalized.getInteractionSoundEffectId());
  }

  @Test
  void normalizedShouldPreserveNonNullValues() {
    PortalConfigComponent config =
        new PortalConfigComponent(
            PortalConfigComponent.Type.Command,
            "test command",
            PortalConfigComponent.CommandSender.Player,
            "SFX_Custom_Sound");

    PortalConfigComponent normalized = config.normalized();

    assertEquals(PortalConfigComponent.Type.Command, normalized.getType());
    assertEquals("test command", normalized.getCommand());
    assertEquals(PortalConfigComponent.CommandSender.Player, normalized.getCommandSender());
    assertEquals("SFX_Custom_Sound", normalized.getInteractionSoundEffectId());
  }

  @Test
  void normalizedShouldPreserveEmptyStrings() {
    PortalConfigComponent config =
        new PortalConfigComponent(
            PortalConfigComponent.Type.Command, "", PortalConfigComponent.CommandSender.Server, "");

    PortalConfigComponent normalized = config.normalized();

    assertEquals("", normalized.getCommand());
    assertEquals("", normalized.getInteractionSoundEffectId());
  }

  @Test
  void normalizedShouldHandleMixedNullAndNonNullFields() {
    PortalConfigComponent config =
        new PortalConfigComponent(
            null, // should default
            "my command", // should preserve
            null, // should default
            ""); // should preserve empty string

    PortalConfigComponent normalized = config.normalized();

    assertEquals(PortalConfigComponent.DEFAULT_TYPE, normalized.getType());
    assertEquals("my command", normalized.getCommand());
    assertEquals(PortalConfigComponent.DEFAULT_COMMAND_SENDER, normalized.getCommandSender());
    assertEquals("", normalized.getInteractionSoundEffectId());
  }
}
