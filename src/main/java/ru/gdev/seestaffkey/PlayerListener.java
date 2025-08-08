package ru.gdev.seestaffkey;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class PlayerListener implements Listener {
   private final SeeStaffKey plugin;

   public PlayerListener(SeeStaffKey plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      String keyId = plugin.getActivePlayers().get(uuid);
      if (keyId != null) {
         plugin.handleStopCommand(player, keyId, true);
      }
   }
}
