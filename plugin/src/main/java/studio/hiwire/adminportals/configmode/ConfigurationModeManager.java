package studio.hiwire.adminportals.configmode;

import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ConfigurationModeManager {

  private final Set<UUID> playersInConfigMode = ConcurrentHashMap.newKeySet();
  private final EventRegistration<Void, PlayerDisconnectEvent> disconnectEventRegistration;

  public ConfigurationModeManager(@NonNullDecl EventRegistry eventRegistry) {
    disconnectEventRegistration =
        eventRegistry.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
  }

  public void shutdown() {
    disconnectEventRegistration.unregister();
    playersInConfigMode.clear();
  }

  private void onPlayerDisconnect(PlayerDisconnectEvent event) {
    disable(event.getPlayerRef().getUuid());
  }

  public boolean isInConfigurationMode(@NonNullDecl PlayerRef player) {
    return playersInConfigMode.contains(player.getUuid());
  }

  public boolean isInConfigurationMode(@NonNullDecl UUID playerUuid) {
    return playersInConfigMode.contains(playerUuid);
  }

  public void enable(@NonNullDecl UUID playerUuid) {
    playersInConfigMode.add(playerUuid);
  }

  public void disable(@NonNullDecl UUID playerUuid) {
    playersInConfigMode.remove(playerUuid);
  }

  public boolean toggle(@NonNullDecl PlayerRef player) {
    UUID uuid = player.getUuid();
    if (playersInConfigMode.contains(uuid)) {
      playersInConfigMode.remove(uuid);
      return false;
    } else {
      playersInConfigMode.add(uuid);
      return true;
    }
  }

  public Set<UUID> getPlayersInConfigMode() {
    return Collections.unmodifiableSet(playersInConfigMode);
  }
}
