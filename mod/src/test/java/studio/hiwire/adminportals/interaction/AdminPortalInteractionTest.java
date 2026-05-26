package studio.hiwire.adminportals.interaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.hiwire.adminportals.component.PortalConfigComponent;

class AdminPortalInteractionTest {

  private PlayerRef playerRef;

  @BeforeEach
  void setup() {
    playerRef = mock(PlayerRef.class);
    when(playerRef.getUuid()).thenReturn(UUID.randomUUID());
  }

  @Test
  void handleServerActionShouldReferToConfiguredServer() throws Exception {
    PortalConfigComponent config =
        new PortalConfigComponent(
            PortalConfigComponent.Type.Server,
            " play.example.com ",
            5520,
            null,
            null,
            PortalConfigComponent.DEFAULT_COMMANDS,
            "",
            true,
            true);

    boolean result = invokeHandleServerAction(config);

    assertTrue(result);
    verify(playerRef).referToServer("play.example.com", 5520);
    verify(playerRef, never()).sendMessage(any(Message.class));
  }

  @Test
  void handleServerActionShouldRejectMissingHost() throws Exception {
    PortalConfigComponent config =
        new PortalConfigComponent(
            PortalConfigComponent.Type.Server,
            " ",
            5520,
            null,
            null,
            PortalConfigComponent.DEFAULT_COMMANDS,
            "",
            true,
            true);

    boolean result = invokeHandleServerAction(config);

    assertFalse(result);
    verify(playerRef, never()).referToServer(anyString(), anyInt());
    verify(playerRef).sendMessage(any(Message.class));
  }

  @Test
  void handleServerActionShouldRejectInvalidPort() throws Exception {
    PortalConfigComponent config =
        new PortalConfigComponent(
            PortalConfigComponent.Type.Server,
            "play.example.com",
            70000,
            null,
            null,
            PortalConfigComponent.DEFAULT_COMMANDS,
            "",
            true,
            true);

    boolean result = invokeHandleServerAction(config);

    assertFalse(result);
    verify(playerRef, never()).referToServer(anyString(), anyInt());
    verify(playerRef).sendMessage(any(Message.class));
  }

  @Test
  void handleServerActionShouldReportTransferFailure() throws Exception {
    PortalConfigComponent config =
        new PortalConfigComponent(
            PortalConfigComponent.Type.Server,
            "play.example.com",
            5520,
            null,
            null,
            PortalConfigComponent.DEFAULT_COMMANDS,
            "",
            true,
            true);
    doThrow(new IllegalArgumentException("bad server"))
        .when(playerRef)
        .referToServer("play.example.com", 5520);

    boolean result = invokeHandleServerAction(config);

    assertFalse(result);
    verify(playerRef).referToServer("play.example.com", 5520);
    verify(playerRef).sendMessage(any(Message.class));
  }

  @Test
  void validateWorldConfigShouldRejectMissingWorldName() {
    PortalConfigComponent config =
        new PortalConfigComponent(
            PortalConfigComponent.Type.World,
            "play.example.com",
            5520,
            " ",
            null,
            null,
            null,
            null,
            null,
            PortalConfigComponent.DEFAULT_COMMANDS,
            "",
            true,
            true);

    boolean result = AdminPortalInteraction.validateWorldConfig(config, playerRef);

    assertFalse(result);
    verify(playerRef).sendMessage(any(Message.class));
  }

  @Test
  void validateWorldConfigShouldRejectIncompleteCoordinates() {
    PortalConfigComponent config =
        new PortalConfigComponent(
            PortalConfigComponent.Type.World,
            "play.example.com",
            5520,
            "adventure",
            1.0,
            null,
            3.0,
            null,
            null,
            PortalConfigComponent.DEFAULT_COMMANDS,
            "",
            true,
            true);

    boolean result = AdminPortalInteraction.validateWorldConfig(config, playerRef);

    assertFalse(result);
    verify(playerRef).sendMessage(any(Message.class));
  }

  @Test
  void validateWorldConfigShouldAllowSpawnTeleportConfig() {
    PortalConfigComponent config =
        new PortalConfigComponent(
            PortalConfigComponent.Type.World,
            "play.example.com",
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

    boolean result = AdminPortalInteraction.validateWorldConfig(config, playerRef);

    assertTrue(result);
    verify(playerRef, never()).sendMessage(any(Message.class));
  }

  @Test
  void validateWorldConfigShouldAllowCompleteCoordinates() {
    PortalConfigComponent config =
        new PortalConfigComponent(
            PortalConfigComponent.Type.World,
            "play.example.com",
            5520,
            "adventure",
            1.0,
            2.0,
            3.0,
            null,
            null,
            PortalConfigComponent.DEFAULT_COMMANDS,
            "",
            true,
            true);

    boolean result = AdminPortalInteraction.validateWorldConfig(config, playerRef);

    assertTrue(result);
    assertTrue(AdminPortalInteraction.hasAnyCoordinate(config));
    verify(playerRef, never()).sendMessage(any(Message.class));
  }

  @Test
  void validateWorldPlayerReadyShouldRejectPlayerWaitingForClientReady() {
    Player playerComponent = mock(Player.class);
    when(playerComponent.isWaitingForClientReady()).thenReturn(true);

    boolean result = AdminPortalInteraction.validateWorldPlayerReady(playerComponent, playerRef);

    assertFalse(result);
    verify(playerRef).sendMessage(any(Message.class));
  }

  private boolean invokeHandleServerAction(PortalConfigComponent config) throws Exception {
    Method method =
        AdminPortalInteraction.class.getDeclaredMethod(
            "handleServerAction", PortalConfigComponent.class, PlayerRef.class);
    method.setAccessible(true);
    return (boolean) method.invoke(new AdminPortalInteraction(), config, playerRef);
  }
}
