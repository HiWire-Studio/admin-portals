package studio.hiwire.adminportals.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaceholderManagerTest {

  private static final String playerUsername = "testPlayer";
  private static final UUID playerUuid = UUID.fromString("12345678-1234-1234-1234-123456789abc");
  private static final String worldName = "testWorld";
  private static final int posX = 10;
  private static final int posY = 20;
  private static final int posZ = 30;

  private PlaceholderManager placeholderManager;
  private PlaceholderContext context;

  @BeforeEach
  void setup() {
    PlayerRef playerRef = mock(PlayerRef.class);
    when(playerRef.getUsername()).thenReturn(playerUsername);
    when(playerRef.getUuid()).thenReturn(playerUuid);

    World world = mock(World.class);
    when(world.getName()).thenReturn(worldName);

    Vector3i position = new Vector3i(posX, posY, posZ);

    context = mock(PlaceholderContext.class);
    when(context.playerRef()).thenReturn(playerRef);
    when(context.world()).thenReturn(world);
    when(context.pos()).thenReturn(position);

    placeholderManager = new PlaceholderManager();
  }

  @Test
  void processShouldReplacePlaceholders() {
    String input = "say {PlayerUsername} used portal at {PosX} {PosY} {PosZ} in world {WorldName}";

    String result = placeholderManager.process(input, context);

    String expected =
        "say "
            + playerUsername
            + " used portal at "
            + posX
            + " "
            + posY
            + " "
            + posZ
            + " in world "
            + worldName;
    assertEquals(expected, result);
  }

  @Test
  void processShouldKeepUnknownPlaceholders() {
    String result = placeholderManager.process("unknown: {UnknownPlaceholder}", context);

    assertEquals("unknown: {UnknownPlaceholder}", result);
  }

  @Test
  void processShouldHandleEmptyInput() {
    String result = placeholderManager.process("", context);

    assertEquals("", result);
  }

  @Test
  void processShouldHandleInputWithoutPlaceholders() {
    String result = placeholderManager.process("no placeholders here", context);

    assertEquals("no placeholders here", result);
  }

  @Test
  void processShouldReplaceMultiplePlaceholders() {
    String result = placeholderManager.process("{PlayerUsername} at {PosX},{PosY},{PosZ}", context);

    String expected = playerUsername + " at " + posX + "," + posY + "," + posZ;
    assertEquals(expected, result);
  }

  @Test
  void registerShouldThrowOnBlankName() {
    assertThrows(
        IllegalArgumentException.class, () -> placeholderManager.register("", ctx -> "value"));

    assertThrows(
        IllegalArgumentException.class, () -> placeholderManager.register("   ", ctx -> "value"));
  }

  @Test
  void registerShouldThrowOnDuplicateName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> placeholderManager.register("PlayerUsername", ctx -> "duplicate"));
  }

  @Test
  void unregisterShouldRemovePlaceholder() {
    assertTrue(placeholderManager.isRegistered("PlayerUsername"));

    boolean removed = placeholderManager.unregister("PlayerUsername");

    assertTrue(removed);
    assertFalse(placeholderManager.isRegistered("PlayerUsername"));
  }

  @Test
  void unregisterShouldReturnFalseForNonExistent() {
    boolean removed = placeholderManager.unregister("nonexistent");

    assertFalse(removed);
  }

  @Test
  void registerShouldEnableCustomPlaceholders() {
    placeholderManager.register("Custom", ctx -> "customValue");

    String result = placeholderManager.process("test {Custom}", context);

    assertEquals("test customValue", result);
  }
}
