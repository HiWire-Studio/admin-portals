package studio.hiwire.adminportals.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
              customSound);

      PortalConfigurationPage page = new PortalConfigurationPage(playerRef, blockRef, config);

      String actualSoundId = getPrivateField(page, "currentInteractionSoundEffectId");
      assertEquals(customSound, actualSoundId);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T getPrivateField(Object obj, String fieldName) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return (T) field.get(obj);
  }
}
