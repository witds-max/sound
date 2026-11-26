package com.vexora.sounds;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Mengirim resource pack pada fase konfigurasi (saat layar loading logo Mojang),
 * sebelum pemain masuk ke dunia. Handler ini berjalan async, jadi aman menunggu.
 */
public final class PackListener implements Listener {

    private final VexoraSounds plugin;

    public PackListener(VexoraSounds plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConfigure(AsyncPlayerConnectionConfigureEvent e) {
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("resource-pack.enabled", true)) return;

        String url = cfg.getString("resource-pack.url", "").trim();
        if (url.isEmpty()) return;

        String sha1 = cfg.getString("resource-pack.sha1", "").trim().toLowerCase();
        boolean required = cfg.getBoolean("resource-pack.required", true);
        int timeout = cfg.getInt("resource-pack.timeout-seconds", 120);
        Component prompt = VexoraSounds.color(cfg.getString("resource-pack.prompt", ""));

        // UUID dibuat dari url + sha1 supaya client mengunduh ulang saat pack diganti
        UUID packId = UUID.nameUUIDFromBytes((url + sha1).getBytes(StandardCharsets.UTF_8));

        ResourcePackInfo info;
        try {
            info = ResourcePackInfo.resourcePackInfo(packId, URI.create(url), sha1);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("URL resource pack tidak valid: " + url);
            return;
        }

        CompletableFuture<Boolean> done = new CompletableFuture<>();
        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(required)
                .prompt(prompt)
                .callback((uuid, status, audience) -> {
                    if (!status.intermediate()) {
                        done.complete(status == ResourcePackStatus.SUCCESSFULLY_LOADED);
                    }
                })
                .build();

        PlayerConfigurationConnection connection = e.getConnection();
        connection.getAudience().sendResourcePacks(request);

        // Tahan pemain di layar loading sampai pack selesai (atau gagal / timeout).
        // Kalau pack wajib dan pemain menolak/gagal, client sendiri yang memutus koneksi.
        try {
            done.get(timeout, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }
}
