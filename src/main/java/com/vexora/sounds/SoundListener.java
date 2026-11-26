package com.vexora.sounds;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class SoundListener implements Listener {

    private final VexoraSounds plugin;

    public SoundListener(VexoraSounds plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player attacker && e.getEntity() instanceof LivingEntity) {
            plugin.play(attacker, "hit");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead instanceof Player p) {
            plugin.streaks().remove(p.getUniqueId());
        }
        Player killer = dead.getKiller();
        if (killer == null || killer.equals(dead)) return;

        plugin.play(killer, "kill");

        int streak = plugin.streaks().merge(killer.getUniqueId(), 1, Integer::sum);
        if (!plugin.getConfig().getBoolean("killstreak.enabled", true)) return;
        if (plugin.disabled().contains(killer.getUniqueId())) return;

        ConfigurationSection tiers = plugin.getConfig().getConfigurationSection("killstreak.tiers");
        if (tiers == null) return;
        int best = -1;
        for (String k : tiers.getKeys(false)) {
            try {
                int n = Integer.parseInt(k);
                if (n <= streak && n > best) best = n;
            } catch (NumberFormatException ignored) {
            }
        }
        if (best > 0) {
            ConfigurationSection tier = tiers.getConfigurationSection(String.valueOf(best));
            if (tier != null) plugin.playSection(killer, tier);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.streaks().remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHotbar(PlayerItemHeldEvent e) {
        plugin.play(e.getPlayer(), "hotbar");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            plugin.play(p, "craft");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onOffhand(PlayerSwapHandItemsEvent e) {
        plugin.play(e.getPlayer(), "offhand");
    }
}
