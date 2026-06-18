package it.markus.playerstats.discord;

import it.markus.playerstats.PlayerStatsPlugin;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Sendet Nachrichten an einen Discord-Webhook (als Embed oder einfacher Text).
 *
 * Der eigentliche HTTP-POST laeuft asynchron ({@link #send}); fuer den
 * Server-Stopp gibt es eine blockierende Variante ({@link #sendBlocking}),
 * weil der Scheduler dann nicht mehr verfuegbar ist.
 *
 * Das JSON wird bewusst von Hand gebaut (mit Escaping), um keine zusaetzliche
 * Bibliothek zu benoetigen.
 */
public final class DiscordWebhook {

    private final PlayerStatsPlugin plugin;

    public DiscordWebhook(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(String title, String description, int color) {
        if (!enabled()) {
            return;
        }
        String payload = buildPayload(title, description, color);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> post(payload));
    }

    /** Blockierende Variante fuer onDisable (kurzer Timeout). */
    public void sendBlocking(String title, String description, int color) {
        if (!enabled()) {
            return;
        }
        post(buildPayload(title, description, color));
    }

    private boolean enabled() {
        String url = plugin.config().discordWebhookUrl();
        if (!plugin.config().discordEnabled() || url == null || url.isBlank()) {
            return false;
        }
        if (!url.startsWith("https://")) {
            plugin.getLogger().warning("discord.webhook-url muss mit https:// beginnen – ignoriert.");
            return false;
        }
        return true;
    }

    private String buildPayload(String title, String description, int color) {
        var cfg = plugin.config();
        StringBuilder b = new StringBuilder("{");
        b.append("\"username\":\"").append(esc(cfg.discordUsername())).append("\"");
        if (!cfg.discordAvatarUrl().isBlank()) {
            b.append(",\"avatar_url\":\"").append(esc(cfg.discordAvatarUrl())).append("\"");
        }
        if (cfg.discordUseEmbeds()) {
            b.append(",\"embeds\":[{")
                    .append("\"title\":\"").append(esc(title)).append("\"");
            if (description != null && !description.isBlank()) {
                b.append(",\"description\":\"").append(esc(description)).append("\"");
            }
            b.append(",\"color\":").append(color).append("}]");
        } else {
            String text = (description == null || description.isBlank()) ? title : title + " — " + description;
            b.append(",\"content\":\"").append(esc(text)).append("\"");
        }
        b.append("}");
        return b.toString();
    }

    private void post(String payload) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(plugin.config().discordWebhookUrl()))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "PlayerStats-Discord")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                plugin.getLogger().warning("Discord-Webhook antwortete mit HTTP " + response.statusCode());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Discord-Webhook fehlgeschlagen: " + ex.getMessage());
        }
    }

    /** Minimal-Escaping fuer JSON-Strings. */
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.toString();
    }
}
