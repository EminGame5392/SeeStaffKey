package ru.gdev.seestaffkey;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SeeStaffKey extends JavaPlugin implements TabExecutor {
    private FileConfiguration cfg;
    private final Map<UUID, String> activePlayers = new ConcurrentHashMap<>();
    private LuckPerms luckPerms;
    private String mode;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cfg = getConfig();
        mode = cfg.getString("settings.mode", "command").toLowerCase();
        if (getCommand("ssw") != null) {
            getCommand("ssw").setExecutor(this);
            getCommand("ssw").setTabCompleter(this);
        }
        if (getCommand("key") != null) {
            getCommand("key").setExecutor(this);
            getCommand("key").setTabCompleter(this);
        }
        if (mode.equals("lp")) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) luckPerms = provider.getProvider();
        }
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        getLogger().info(colorize("&aSeeStaffKey запущен в режиме: " + mode));
    }

    @Override
    public void onDisable() {
        for (Map.Entry<UUID, String> e : activePlayers.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && p.isOnline()) {
                handleStopCommand(p, e.getValue(), true);
            }
        }
        activePlayers.clear();
    }

    public Map<UUID, String> getActivePlayers() {
        return activePlayers;
    }

    public FileConfiguration getCfg() {
        return cfg;
    }

    public String getMode() {
        return mode;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize(cfg.getString("settings.only_players", "&cЭту команду может выполнять только игрок !")));
            return true;
        }
        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase();

        if ("ssw".equals(cmd)) {
            if (args.length == 0) return false;
            switch (args[0].toLowerCase()) {
                case "reload":
                    reloadConfig();
                    cfg = getConfig();
                    mode = cfg.getString("settings.mode", "command").toLowerCase();
                    sender.sendMessage(colorize(cfg.getString("settings.reload_message", "&aПлагин перезагружен !")));
                    return true;
                case "help":
                    sendAdminHelp(player);
                    return true;
                case "info":
                    player.sendMessage(" ");
                    player.sendMessage(colorize("&fСоздатель: &bEminGame5392"));
                    player.sendMessage(colorize("&fВерсия: &b" + getDescription().getVersion()));
                    player.sendMessage(" ");
                    return true;
                default:
                    return false;
            }
        } else if ("key".equals(cmd)) {
            if (args.length == 0) return false;
            if ("help".equalsIgnoreCase(args[0])) {
                sendKeyHelp(player);
                return true;
            }
            if ("stop".equalsIgnoreCase(args[0])) {
                if (args.length < 2) {
                    player.sendMessage(colorize(cfg.getString("settings.please_specify_key", "&bУкажите ID ключа !")));
                    return true;
                }
                handleStopCommand(player, args[1], false);
                return true;
            }
            handleKeyCommand(player, args[0]);
            return true;
        }
        return false;
    }

    private void sendAdminHelp(Player p) {
        p.sendMessage(" ");
        p.sendMessage(colorize("&eПомощь по плагину:"));
        p.sendMessage(" ");
        p.sendMessage(colorize("&b/ssw reload &f- Перезагружает плагин."));
        p.sendMessage(colorize("&b/key [id ключа] &f- Начинает работу."));
        p.sendMessage(colorize("&b/key stop [id ключа] &f- Заканчивает работу."));
        p.sendMessage(colorize("&b/key help &f- Помощь для состава."));
        p.sendMessage(" ");
    }

    private void sendKeyHelp(Player p) {
        p.sendMessage(" ");
        p.sendMessage(colorize("&eПомощь для состава:"));
        p.sendMessage(" ");
        p.sendMessage(colorize("&b/key [id ключа] &f- Начинает работу."));
        p.sendMessage(colorize("&b/key stop [id ключа] &f- Заканчивает работу."));
        p.sendMessage(" ");
    }

    private void handleKeyCommand(Player player, String keyId) {
        String keyPath = "keys." + keyId;
        if (!cfg.contains(keyPath)) {
            player.sendMessage(colorize(cfg.getString("settings.not_found_keyid", "&cНеизвестный ключ !")));
            return;
        }
        List<String> allowed = cfg.getStringList(keyPath + ".allowed_players");
        if (!allowed.contains(player.getName()) && !player.hasPermission("seestaffkey.override")) {
            player.sendMessage(colorize(cfg.getString("settings.not_allowed_player", "&cВас нету в списке состава !")));
            return;
        }
        if (activePlayers.containsKey(player.getUniqueId())) {
            player.sendMessage(colorize(cfg.getString("settings.already_active", "&cВы уже находитесь в режиме работы !")));
            return;
        }
        handleStartCommand(player, keyId);
    }

    public void handleStartCommand(Player player, String keyId) {
        activePlayers.put(player.getUniqueId(), keyId);
        String base = "keys." + keyId;
        if (mode.equals("lp") && luckPerms != null) {
            String group = cfg.getString(base + ".start_group");
            if (group != null && !group.isEmpty()) {
                User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
                InheritanceNode node = InheritanceNode.builder(group).build();
                user.data().remove(node);
                user.data().add(node);
                luckPerms.getUserManager().saveUser(user);
            }
        } else {
            String cmd = cfg.getString(base + ".start_command");
            if (cmd != null && !cmd.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
            }
        }
        String msg = cfg.getString(base + ".start_message");
        if (msg != null) player.sendMessage(colorize(msg));
        String title = cfg.getString(base + ".start_title");
        if (title != null) player.sendTitle(colorize(title), "", 10, 60, 10);
    }

    public void handleStopCommand(Player player, String keyId, boolean forced) {
        UUID uuid = player.getUniqueId();
        String active = activePlayers.get(uuid);
        if (active == null || (keyId != null && !keyId.equalsIgnoreCase(active))) {
            if (!forced) player.sendMessage(colorize(cfg.getString("settings.no_work_mode", "&cВы не находитесь в режиме работы !")));
            return;
        }
        String base = "keys." + active;
        if (mode.equals("lp") && luckPerms != null) {
            String group = cfg.getString(base + ".stop_group");
            if (group != null && !group.isEmpty()) {
                User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
                InheritanceNode node = InheritanceNode.builder(group).build();
                user.data().remove(node);
                user.data().add(node);
                luckPerms.getUserManager().saveUser(user);
            }
        } else {
            String cmd = cfg.getString(base + ".stop_command");
            if (cmd != null && !cmd.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
            }
        }
        activePlayers.remove(uuid);
        String msg = cfg.getString(base + ".stop_message");
        if (msg != null) player.sendMessage(colorize(msg));
        String title = cfg.getString(base + ".stop_title");
        if (title != null) player.sendTitle(colorize(title), "", 10, 60, 10);
    }

    private String colorize(String s) {
        return s == null ? "" : s.replace("&", "§");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        String cmd = command.getName().toLowerCase();
        List<String> suggestions = new ArrayList<>();
        if ("ssw".equals(cmd)) {
            if (args.length == 1) suggestions.addAll(Arrays.asList("reload", "help", "info"));
        } else if ("key".equals(cmd)) {
            if (args.length == 1) {
                suggestions.add("help");
                suggestions.add("stop");
                if (cfg.isConfigurationSection("keys"))
                    suggestions.addAll(cfg.getConfigurationSection("keys").getKeys(false));
            } else if (args.length == 2 && "stop".equalsIgnoreCase(args[0])) {
                if (cfg.isConfigurationSection("keys"))
                    suggestions.addAll(cfg.getConfigurationSection("keys").getKeys(false));
            }
        }
        if (args.length == 0) return suggestions;
        String current = args[args.length - 1].toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String s : suggestions) if (s.toLowerCase().startsWith(current)) filtered.add(s);
        return filtered;
    }
}
