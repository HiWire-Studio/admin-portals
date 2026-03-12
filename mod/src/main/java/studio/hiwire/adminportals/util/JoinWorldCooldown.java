package studio.hiwire.adminportals.util;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.registry.Registration;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import studio.hiwire.adminportals.AdminPortalsPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JoinWorldCooldown {

  private final Map<UUID,Long> loginTimes = new ConcurrentHashMap<>();
  private final long cooldownMs;

  private final Set<EventRegistration<?,?>> registrations = ConcurrentHashMap.newKeySet();

  public void unregisterListeners() {
    registrations.forEach(Registration::unregister);
    registrations.clear();
  }

  public JoinWorldCooldown(AdminPortalsPlugin plugin, long cooldownMs) {
    registrations.add(plugin.getEventRegistry().registerGlobal(EventPriority.FIRST, PlayerConnectEvent.class, this::onPlayerConnect));
    registrations.add(plugin.getEventRegistry().registerGlobal(EventPriority.FIRST, PlayerReadyEvent.class, this::onPlayerReady));
    registrations.add(plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onDisconnect));
    this.cooldownMs = cooldownMs;
  }

  // Pre-init cache with always true cooldown
  public void onPlayerConnect(PlayerConnectEvent event) {
    loginTimes.put(event.getPlayerRef().getUuid(), Long.MAX_VALUE);
  }

  // Shorten down cooldown when the player actually logs in the world
  public void onPlayerReady(PlayerReadyEvent event) {
    loginTimes.put(event.getPlayer().getUuid(), System.currentTimeMillis());
  }

  public void onDisconnect(PlayerDisconnectEvent event) {
    loginTimes.remove(event.getPlayerRef().getUuid());
  }

  /**
   * @return true if the player is on cooldown
   */
  public boolean hasLoginCooldown(UUID userUuid) {
    Long loginTime = loginTimes.get(userUuid);
    if(loginTime == null) {
      return false;
    } else {
      return cooldownMs > System.currentTimeMillis() - loginTime;
    }
  }

}
