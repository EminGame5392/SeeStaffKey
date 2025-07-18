package ru.gdev.sstaffkey;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SeeStaffKey extends JavaPlugin implements TabExecutor, Listener {
   private FileConfiguration config;
   private final ConcurrentHashMap<String, String> activePlayers = new ConcurrentHashMap();

   public void onEnable() {
      this.saveDefaultConfig();
      this.config = this.getConfig();
      this.getCommand("ssw").setExecutor(this);
      this.getCommand("key").setExecutor(this);
      this.getCommand("key").setTabCompleter(this);
      Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
      Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
      this.getLogger().info("SeeStaffKey успешно запущен!");
   }

   public void onDisable() {
      this.activePlayers.forEach((playerName, keyId) -> {
         this.handleStopCommand(Bukkit.getPlayer(playerName), keyId, true);
      });
      this.activePlayers.clear();
      this.getLogger().info("SeeStaffKey выключен!");
   }

   public ConcurrentHashMap<String, String> getActivePlayers() {
      return this.activePlayers;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§cЭту команду может выполнять только игрок !");
         return true;
      } else {
         Player player = (Player)sender;
         String mode;
         if (label.equalsIgnoreCase("ssw")) {
            if (args.length == 0) {
               return false;
            }

            mode = args[0].toLowerCase();
            byte var7 = -1;
            switch(mode.hashCode()) {
            case -934641255:
               if (mode.equals("reload")) {
                  var7 = 0;
               }
               break;
            case 3198785:
               if (mode.equals("help")) {
                  var7 = 1;
               }
               break;
            case 3237038:
               if (mode.equals("info")) {
                  var7 = 2;
               }
            }

            switch(var7) {
            case 0:
               this.reloadConfig();
               this.config = this.getConfig();
               player.sendMessage(this.config.getString("settings.reload_message").replace("&", "§"));
               break;
            case 1:
               player.sendMessage(" ");
               player.sendMessage("§eПомощь по плагину:");
               player.sendMessage(" ");
               player.sendMessage("§b/ssw reload§f - Перезагружает плагин.");
               player.sendMessage("§b/key [id ключа]§f - Начинает работу.");
               player.sendMessage("§b/key stop [id ключа]§f - Заканчивает работу.");
               player.sendMessage("§b/key help §f - Помощь для состава.");
               player.sendMessage(" ");
               break;
            case 2:
               player.sendMessage(" ");
               player.sendMessage("§fСоздатель: §bEminGame5392");
               player.sendMessage("§fВерсия: §b1.4.5§7 (Release)");
               player.sendMessage(" ");
               break;
            default:
               return false;
            }
         } else if (label.equalsIgnoreCase("key")) {
            if (args.length == 0) {
               return false;
            }

            mode = this.config.getString("settings.mode");
            String var9 = args[0].toLowerCase();
            byte var8 = -1;
            switch(var9.hashCode()) {
            case 3198785:
               if (var9.equals("help")) {
                  var8 = 0;
               }
               break;
            case 3540994:
               if (var9.equals("stop")) {
                  var8 = 1;
               }
            }

            switch(var8) {
            case 0:
               player.sendMessage(" ");
               player.sendMessage("§eПомощь для состава:");
               player.sendMessage(" ");
               player.sendMessage("§b/key [id ключа]§f - Начинает работу.");
               player.sendMessage("§b/key stop [id ключа]§f - Заканчивает работу.");
               player.sendMessage("§b/key help §f - Помощь для состава.");
               player.sendMessage(" ");
               break;
            case 1:
               if (args.length < 2) {
                  player.sendMessage("§bУкажите ID ключа !");
                  return true;
               }

               this.handleStopCommand(player, args[1], false);
               break;
            default:
               this.handleKeyCommand(player, args[0], mode);
            }
         }

         return true;
      }
   }

   private void handleKeyCommand(Player player, String keyId, String mode) {
      if (!this.config.contains("keys." + keyId)) {
         player.sendMessage(this.config.getString("settings.not_found_keyid").replace("&", "§"));
      } else if (!this.config.getStringList("keys." + keyId + ".allowed_players").contains(player.getName())) {
         player.sendMessage(this.config.getString("settings.not_allowed_player").replace("&", "§"));
      } else if (this.activePlayers.containsKey(player.getName())) {
         player.sendMessage("§cВы уже находитесь в режиме работы!");
      } else {
         String startCommand;
         if (mode.equalsIgnoreCase("lp")) {
            startCommand = this.config.getString("keys." + keyId + ".start_group");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent set " + startCommand);
         } else if (mode.equalsIgnoreCase("command")) {
            startCommand = this.config.getString("keys." + keyId + ".start_command").replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), startCommand);
         }

         player.sendMessage(this.config.getString("keys." + keyId + ".start_message").replace("&", "§"));
         player.sendTitle(this.config.getString("keys." + keyId + ".start_title").replace("&", "§"), "", 10, 70, 20);
         this.activePlayers.put(player.getName(), keyId);
      }
   }

   public void handleStopCommand(Player player, String keyId, boolean onDisconnect) {
      if (player != null && this.activePlayers.containsKey(player.getName())) {
         if (!this.config.contains("keys." + keyId)) {
            player.sendMessage(this.config.getString("settings.not_found_keyid").replace("&", "§"));
         } else {
            String mode = this.config.getString("settings.mode");
            String stopCommand;
            if (mode.equalsIgnoreCase("lp")) {
               stopCommand = this.config.getString("keys." + keyId + ".stop_group");
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent set " + stopCommand);
            } else if (mode.equalsIgnoreCase("command")) {
               stopCommand = this.config.getString("keys." + keyId + ".stop_command").replace("{player}", player.getName());
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stopCommand);
            }

            if (!onDisconnect) {
               player.sendMessage(this.config.getString("keys." + keyId + ".stop_message").replace("&", "§"));
               player.sendTitle(this.config.getString("keys." + keyId + ".stop_title").replace("&", "§"), "", 10, 70, 20);
            }

            this.activePlayers.remove(player.getName());
         }
      } else {
         if (!onDisconnect) {
            player.sendMessage(this.config.getString("settings.no_work_mode").replace("&", "§"));
         }

      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (command.getName().equalsIgnoreCase("key")) {
         if (args.length == 1) {
            completions.add("help");
            completions.add("stop");
            completions.add("[ID ключа]");
         } else if (args.length == 2 && args[0].equalsIgnoreCase("stop")) {
            completions.add("[ID ключа]");
            completions.addAll(this.activePlayers.keySet());
         }
      } else if (command.getName().equalsIgnoreCase("ssw") && args.length == 1) {
         completions.add("reload");
         completions.add("help");
         completions.add("info");
      }

      String currentInput = args[args.length - 1].toLowerCase();
      List<String> filteredCompletions = new ArrayList();
      Iterator var8 = completions.iterator();

      while(var8.hasNext()) {
         String suggestion = (String)var8.next();
         if (suggestion.toLowerCase().startsWith(currentInput)) {
            filteredCompletions.add(suggestion);
         }
      }

      return filteredCompletions;
   }
}
