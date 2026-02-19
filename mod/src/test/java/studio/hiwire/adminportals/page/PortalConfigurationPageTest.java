package studio.hiwire.adminportals.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.meta.state.BlockMapMarker;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import studio.hiwire.adminportals.component.PortalConfigComponent;

class PortalConfigurationPageTest {

  private PlayerRef playerRef;
  private Ref<ChunkStore> blockRef;
  private Store<ChunkStore> store;
  private ComponentType<ChunkStore, BlockMapMarker> markerComponentType;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setup() {
    playerRef = mock(PlayerRef.class);
    blockRef = mock(Ref.class);
    store = mock(Store.class);
    markerComponentType = mock(ComponentType.class);

    when(blockRef.getStore()).thenReturn(store);
    // Return null for the map marker component - no existing marker
    when(store.getComponent(eq(blockRef), any())).thenReturn(null);
  }

  @Test
  void constructorShouldUseDefaultSoundWhenConfigSoundIsNull() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              "test command",
              PortalConfigComponent.CommandSender.Server,
              null,
              null // null sound effect ID
              );

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      String actualSoundId = getPrivateField(page, "currentInteractionSoundEffectId");
      assertEquals(PortalConfigComponent.DEFAULT_INTERACTION_SOUND_EFFECT_ID, actualSoundId);
    }
  }

  @Test
  void constructorShouldPreserveEmptyStringWhenConfigSoundIsEmpty() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              "test command",
              PortalConfigComponent.CommandSender.Server,
              null,
              "" // explicitly empty sound effect ID
              );

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      String actualSoundId = getPrivateField(page, "currentInteractionSoundEffectId");
      assertEquals("", actualSoundId);
    }
  }

  @Test
  void constructorShouldPreserveCustomSoundWhenConfigSoundIsSet() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      String customSound = "SFX_Custom_Sound";
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              "test command",
              PortalConfigComponent.CommandSender.Server,
              null,
              customSound);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      String actualSoundId = getPrivateField(page, "currentInteractionSoundEffectId");
      assertEquals(customSound, actualSoundId);
    }
  }

  @Test
  void constructorShouldMigrateLegacyCommandToCurrentCommands() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              "legacy command",
              PortalConfigComponent.CommandSender.Player,
              null, // no commands array - should migrate
              "SFX_Custom_Sound");

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      List<PortalConfigComponent.CommandEntry> commands = getPrivateField(page, "currentCommands");
      assertNotNull(commands);
      assertEquals(1, commands.size());
      assertEquals("legacy command", commands.get(0).getCommand());
      assertEquals(PortalConfigComponent.CommandSender.Player, commands.get(0).getCommandSender());
    }
  }

  @Test
  void constructorShouldLoadMultipleCommands() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent.CommandEntry[] commands =
          new PortalConfigComponent.CommandEntry[] {
            new PortalConfigComponent.CommandEntry(
                "cmd1", PortalConfigComponent.CommandSender.Server),
            new PortalConfigComponent.CommandEntry(
                "cmd2", PortalConfigComponent.CommandSender.Player)
          };
      PortalConfigComponent config =
          new PortalConfigComponent(PortalConfigComponent.Type.Command, null, null, commands, null);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      List<PortalConfigComponent.CommandEntry> currentCommands =
          getPrivateField(page, "currentCommands");
      assertNotNull(currentCommands);
      assertEquals(2, currentCommands.size());
      assertEquals("cmd1", currentCommands.get(0).getCommand());
      assertEquals(
          PortalConfigComponent.CommandSender.Server, currentCommands.get(0).getCommandSender());
      assertEquals("cmd2", currentCommands.get(1).getCommand());
      assertEquals(
          PortalConfigComponent.CommandSender.Player, currentCommands.get(1).getCommandSender());
    }
  }

  @Test
  void constructorShouldHandleEmptyCommandsArray() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              null,
              null,
              new PortalConfigComponent.CommandEntry[0],
              null);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      List<PortalConfigComponent.CommandEntry> currentCommands =
          getPrivateField(page, "currentCommands");
      assertNotNull(currentCommands);
      assertEquals(0, currentCommands.size());
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T getPrivateField(Object obj, String fieldName) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return (T) field.get(obj);
  }
}
