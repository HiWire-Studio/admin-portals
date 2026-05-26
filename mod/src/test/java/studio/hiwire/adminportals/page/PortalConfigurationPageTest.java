package studio.hiwire.adminportals.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.meta.state.BlockMapMarker;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
              "",
              0,
              null, // null sound effect ID
              null,
              null);

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
              "",
              0,
              "", // explicitly empty sound effect ID
              null,
              null);

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
              "",
              0,
              customSound,
              null,
              null);

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
              "",
              0,
              "SFX_Custom_Sound",
              null,
              null);

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
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command, null, null, commands, "", 0, null, null, null);

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
              "",
              0,
              null,
              null,
              null);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      List<PortalConfigComponent.CommandEntry> currentCommands =
          getPrivateField(page, "currentCommands");
      assertNotNull(currentCommands);
      assertEquals(0, currentCommands.size());
    }
  }

  @Test
  void constructorShouldLoadCollisionInteractionFromConfig() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command, null, null, null, "", 0, null, false, null);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      boolean collision = getPrivateField(page, "currentCollisionInteraction");
      assertFalse(collision);
    }
  }

  @Test
  void constructorShouldDefaultCollisionInteractionWhenNull() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command, null, null, null, "", 0, null, null, null);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      boolean collision = getPrivateField(page, "currentCollisionInteraction");
      assertTrue(collision);
    }
  }

  @Test
  void constructorShouldLoadUseInteractionFromConfig() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command, null, null, null, "", 0, null, null, false);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      boolean use = getPrivateField(page, "currentUseInteraction");
      assertFalse(use);
    }
  }

  @Test
  void constructorShouldDefaultUseInteractionWhenNull() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command, null, null, null, "", 0, null, null, null);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      boolean use = getPrivateField(page, "currentUseInteraction");
      assertTrue(use);
    }
  }

  @Test
  void constructorShouldLoadServerHostAndPortFromConfig() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Server,
              null,
              null,
              null,
              "play.example.com",
              5520,
              null,
              null,
              null);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      String host = getPrivateField(page, "currentServerHost");
      int port = getPrivateField(page, "currentServerPort");
      assertEquals("play.example.com", host);
      assertEquals(5520, port);
    }
  }

  @Test
  void constructorShouldDefaultServerHostAndPortWhenNull() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              null,
              (PortalConfigComponent.CommandSender) null,
              null,
              null,
              null,
              null,
              null,
              null);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      String host = getPrivateField(page, "currentServerHost");
      int port = getPrivateField(page, "currentServerPort");
      assertEquals("", host);
      assertEquals(PortalConfigComponent.DEFAULT_SERVER_PORT, port);
    }
  }

  @Test
  void constructorShouldDefaultWorldFieldsWhenUnset() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigurationPage page =
          new PortalConfigurationPage(playerRef, blockRef, new PortalConfigComponent());

      String worldName = getPrivateField(page, "currentWorldName");
      Double worldX = getPrivateField(page, "currentWorldX");
      Double worldY = getPrivateField(page, "currentWorldY");
      Double worldZ = getPrivateField(page, "currentWorldZ");
      String worldXText = getPrivateField(page, "currentWorldXText");
      String worldYText = getPrivateField(page, "currentWorldYText");
      String worldZText = getPrivateField(page, "currentWorldZText");

      assertEquals("", worldName);
      assertNull(worldX);
      assertNull(worldY);
      assertNull(worldZ);
      assertEquals("", worldXText);
      assertEquals("", worldYText);
      assertEquals("", worldZText);
    }
  }

  @Test
  void constructorShouldLoadWorldFieldsFromConfig() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.World,
              "",
              5520,
              "adventure",
              10.5d,
              64.0d,
              -20.25d,
              null,
              null,
              null,
              null,
              null,
              null);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      assertEquals("adventure", getPrivateField(page, "currentWorldName"));
      assertEquals(10.5d, getPrivateField(page, "currentWorldX"));
      assertEquals(64.0d, getPrivateField(page, "currentWorldY"));
      assertEquals(-20.25d, getPrivateField(page, "currentWorldZ"));
      assertEquals("10.5", getPrivateField(page, "currentWorldXText"));
      assertEquals("64.0", getPrivateField(page, "currentWorldYText"));
      assertEquals("-20.25", getPrivateField(page, "currentWorldZText"));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void handleServerSaveShouldPersistServerConfig() throws Exception {
    ComponentType<ChunkStore, PortalConfigComponent> portalComponentType =
        (ComponentType<ChunkStore, PortalConfigComponent>) mock(ComponentType.class);
    ComponentType<ChunkStore, BlockModule.BlockStateInfo> blockStateInfoComponentType =
        (ComponentType<ChunkStore, BlockModule.BlockStateInfo>) mock(ComponentType.class);

    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class);
        MockedStatic<PortalConfigComponent> mockedPortalConfig =
            mockStatic(PortalConfigComponent.class, CALLS_REAL_METHODS);
        MockedStatic<BlockModule.BlockStateInfo> mockedBlockStateInfo =
            mockStatic(BlockModule.BlockStateInfo.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);
      mockedPortalConfig
          .when(PortalConfigComponent::getComponentType)
          .thenReturn(portalComponentType);
      mockedBlockStateInfo
          .when(BlockModule.BlockStateInfo::getComponentType)
          .thenReturn(blockStateInfoComponentType);

      PortalConfigComponent.CommandEntry[] commands =
          new PortalConfigComponent.CommandEntry[] {
            new PortalConfigComponent.CommandEntry(
                "kept command", PortalConfigComponent.CommandSender.Player)
          };
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              null,
              null,
              commands,
              "",
              0,
              "SFX_Custom_Sound",
              true,
              true);
      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      PortalConfigurationPage.PageData data = new PortalConfigurationPage.PageData();
      data.serverHost = " play.example.com ";
      data.serverPort = "5520";
      data.interactionSoundEffectId = "";
      data.collisionInteraction = false;
      data.useInteraction = true;

      boolean saved = invokeHandleServerSave(page, data);

      assertTrue(saved);
      ArgumentCaptor<PortalConfigComponent> configCaptor =
          ArgumentCaptor.forClass(PortalConfigComponent.class);
      verify(store).putComponent(eq(blockRef), eq(portalComponentType), configCaptor.capture());

      PortalConfigComponent savedConfig = configCaptor.getValue();
      assertEquals(PortalConfigComponent.Type.Server, savedConfig.getType());
      assertEquals("play.example.com", savedConfig.getServerHost());
      assertEquals(5520, savedConfig.getServerPort());
      assertEquals("", savedConfig.getInteractionSoundEffectId());
      assertFalse(savedConfig.getCollisionInteraction());
      assertTrue(savedConfig.getUseInteraction());
      assertEquals(1, savedConfig.getCommands().length);
      assertEquals("kept command", savedConfig.getCommands()[0].getCommand());
      assertEquals(
          PortalConfigComponent.CommandSender.Player,
          savedConfig.getCommands()[0].getCommandSender());
    }
  }

  @Test
  void handleServerSaveShouldRejectInvalidPort() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigurationPage page =
          new PortalConfigurationPage(playerRef, blockRef, new PortalConfigComponent());

      PortalConfigurationPage.PageData data = new PortalConfigurationPage.PageData();
      data.serverHost = "play.example.com";
      data.serverPort = "70000";

      boolean saved = invokeHandleServerSave(page, data);

      assertFalse(saved);
      verify(store, never()).putComponent(any(), any(), any());
      verify(playerRef).sendMessage(any(Message.class));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void finishSaveEventShouldClearLoadingWhenSaveFails() {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      TestPortalConfigurationPage page =
          new TestPortalConfigurationPage(playerRef, blockRef, new PortalConfigComponent());
      Ref<EntityStore> entityRef = mock(Ref.class);
      Store<EntityStore> entityStore = mock(Store.class);

      page.finishSaveEvent(entityRef, entityStore, null, false);

      assertEquals(1, page.sendUpdateCount);
    }
  }

  @Test
  void handleWorldSaveShouldRejectPartialCoordinates() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigurationPage page =
          new PortalConfigurationPage(playerRef, blockRef, new PortalConfigComponent());

      PortalConfigurationPage.PageData data = new PortalConfigurationPage.PageData();
      data.worldName = "adventure";
      data.worldX = "10.5";
      data.worldY = "";
      data.worldZ = "20.25";

      boolean saved = invokeHandleWorldSave(page, data);

      assertFalse(saved);
      verify(store, never()).putComponent(any(), any(), any());
      verify(playerRef).sendMessage(any(Message.class));
    }
  }

  @Test
  void handleWorldSaveShouldRejectMissingWorldName() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigurationPage page =
          new PortalConfigurationPage(playerRef, blockRef, new PortalConfigComponent());

      PortalConfigurationPage.PageData data = new PortalConfigurationPage.PageData();
      data.worldName = " ";
      data.worldX = "";
      data.worldY = "";
      data.worldZ = "";

      boolean saved = invokeHandleWorldSave(page, data);

      assertFalse(saved);
      verify(store, never()).putComponent(any(), any(), any());
      verify(playerRef).sendMessage(any(Message.class));
    }
  }

  @Test
  void handleWorldSaveShouldRejectNonFiniteCoordinates() throws Exception {
    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);

      PortalConfigurationPage page =
          new PortalConfigurationPage(playerRef, blockRef, new PortalConfigComponent());

      PortalConfigurationPage.PageData data = new PortalConfigurationPage.PageData();
      data.worldName = "adventure";
      data.worldX = "NaN";
      data.worldY = "64";
      data.worldZ = "20";

      boolean saved = invokeHandleWorldSave(page, data);

      assertFalse(saved);
      verify(store, never()).putComponent(any(), any(), any());
      verify(playerRef).sendMessage(any(Message.class));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void handleWorldSaveShouldPersistWorldConfig() throws Exception {
    ComponentType<ChunkStore, PortalConfigComponent> portalComponentType =
        (ComponentType<ChunkStore, PortalConfigComponent>) mock(ComponentType.class);
    ComponentType<ChunkStore, BlockModule.BlockStateInfo> blockStateInfoComponentType =
        (ComponentType<ChunkStore, BlockModule.BlockStateInfo>) mock(ComponentType.class);

    try (MockedStatic<BlockMapMarker> mockedMarker = mockStatic(BlockMapMarker.class);
        MockedStatic<PortalConfigComponent> mockedPortalConfig =
            mockStatic(PortalConfigComponent.class, CALLS_REAL_METHODS);
        MockedStatic<BlockModule.BlockStateInfo> mockedBlockStateInfo =
            mockStatic(BlockModule.BlockStateInfo.class)) {
      mockedMarker.when(BlockMapMarker::getComponentType).thenReturn(markerComponentType);
      mockedPortalConfig
          .when(PortalConfigComponent::getComponentType)
          .thenReturn(portalComponentType);
      mockedBlockStateInfo
          .when(BlockModule.BlockStateInfo::getComponentType)
          .thenReturn(blockStateInfoComponentType);

      PortalConfigComponent.CommandEntry[] commands =
          new PortalConfigComponent.CommandEntry[] {
            new PortalConfigComponent.CommandEntry(
                "kept command", PortalConfigComponent.CommandSender.Player)
          };
      PortalConfigComponent config =
          new PortalConfigComponent(
              PortalConfigComponent.Type.Command,
              null,
              null,
              commands,
              "play.example.com",
              5520,
              "SFX_Custom_Sound",
              true,
              true);
      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      PortalConfigurationPage.PageData data = new PortalConfigurationPage.PageData();
      data.type = PortalConfigComponent.Type.World;
      data.worldName = " adventure ";
      data.worldX = "10.5";
      data.worldY = "64";
      data.worldZ = "-20.25";
      data.interactionSoundEffectId = "";
      data.collisionInteraction = false;
      data.useInteraction = true;

      boolean saved = invokeHandleWorldSave(page, data);

      assertTrue(saved);
      ArgumentCaptor<PortalConfigComponent> configCaptor =
          ArgumentCaptor.forClass(PortalConfigComponent.class);
      verify(store).putComponent(eq(blockRef), eq(portalComponentType), configCaptor.capture());

      PortalConfigComponent savedConfig = configCaptor.getValue();
      assertEquals(PortalConfigComponent.Type.World, savedConfig.getType());
      assertEquals("play.example.com", savedConfig.getServerHost());
      assertEquals(5520, savedConfig.getServerPort());
      assertEquals("adventure", savedConfig.getWorldName());
      assertEquals(10.5d, savedConfig.getWorldX());
      assertEquals(64.0d, savedConfig.getWorldY());
      assertEquals(-20.25d, savedConfig.getWorldZ());
      assertEquals("", savedConfig.getInteractionSoundEffectId());
      assertFalse(savedConfig.getCollisionInteraction());
      assertTrue(savedConfig.getUseInteraction());
      assertEquals(1, savedConfig.getCommands().length);
      assertEquals("kept command", savedConfig.getCommands()[0].getCommand());
    }
  }

  private boolean invokeHandleServerSave(
      PortalConfigurationPage page, PortalConfigurationPage.PageData data) throws Exception {
    Method method =
        PortalConfigurationPage.class.getDeclaredMethod("handleServerSave", data.getClass());
    method.setAccessible(true);
    return (boolean) method.invoke(page, data);
  }

  private boolean invokeHandleWorldSave(
      PortalConfigurationPage page, PortalConfigurationPage.PageData data) throws Exception {
    Method method =
        PortalConfigurationPage.class.getDeclaredMethod("handleWorldSave", data.getClass());
    method.setAccessible(true);
    return (boolean) method.invoke(page, data);
  }

  @SuppressWarnings("unchecked")
  private <T> T getPrivateField(Object obj, String fieldName) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return (T) field.get(obj);
  }

  private static final class TestPortalConfigurationPage extends PortalConfigurationPage {
    private int sendUpdateCount;

    private TestPortalConfigurationPage(
        PlayerRef playerRef, Ref<ChunkStore> blockRef, PortalConfigComponent config) {
      super(playerRef, blockRef, config);
    }

    @Override
    protected void sendUpdate() {
      sendUpdateCount++;
    }
  }
}
