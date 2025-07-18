package ru.gdev.sstaffkey;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
   private final SeeStaffKey plugin;

   public PlayerListener(SeeStaffKey plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      String keyId = (String)this.plugin.getActivePlayers().get(player.getName());
      if (keyId != null) {
         this.plugin.handleStopCommand(player, keyId, true);
      }

   }
}
