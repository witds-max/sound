package com.vexora.sounds;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VexoraSounds extends JavaPlugin {

    private final Set<UUID> disabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> streaks = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new SoundListener(this), this);
        getServer().getPluginManager().registerEvents(new PackListener(this), this);
        getLogger().info("VexoraSounds aktif.");
    }

    public Set<UUID> disabled() {
        return disabled;
    }

    public Map<UUID, Integer> streaks() {
        return streaks;
    }

    public static Component color(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s == null ? "" : s);
    }

    public Component msg(String path) {
        String prefix = getConfig().getString("messages.prefix", "");
        return color(prefix + getConfig().getString("messages." + path, ""));
    }

    /** Putar sound dari bagian "sounds.<path>" di config. */
    public void play(Player p, String path) {
        if (disabled.contains(p.getUniqueId())) return;
        ConfigurationSection s = getConfig().getConfigurationSection("sounds." + path);
        if (s == null || !s.getBoolean("enabled", true)) return;
        playSection(p, s);
    }

    public void playSection(Player p, ConfigurationSection s) {
        String key = s.getString("key", "");
        if (key.isEmpty()) return;
        p.playSound(p.getLocation(), key, SoundCategory.PLAYERS,
                (float) s.getDouble("volume", 1.0), (float) s.getDouble("pitch", 1.0));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&7Pakai: &f/vsound toggle &7| &f/vsound reload"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vexorasounds.admin")) {
                sender.sendMessage(msg("no-permission"));
                return true;
            }
            reloadConfig();
            sender.sendMessage(msg("reloaded"));
            return true;
        }
        if (args[0].equalsIgnoreCase("toggle")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Hanya untuk pemain.");
                return true;
            }
            if (disabled.remove(p.getUniqueId())) {
                p.sendMessage(msg("sound-on"));
            } else {
                disabled.add(p.getUniqueId());
                p.sendMessage(msg("sound-off"));
            }
            return true;
        }
        return false;
    }
                }
