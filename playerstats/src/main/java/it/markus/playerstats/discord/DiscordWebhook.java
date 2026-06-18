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

    private static final long ERROR_LOG_COOLDOWN_MS = 60_000L;
    private static final long DEFAULT_BACKOFF_MS = 5_000L;

    private final PlayerStatsPlugin plugin;

    // Rate-Limit-Backoff (bei 429) und gedrosseltes Fehler-Logging.
    private volatile long blockedUntil;
    private volatile long lastErrorLog;

    public DiscordWebhook(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(String title, String description, int color) {
        if (!enabled() || isBlocked()) {
            return;
        }
        String payload = buildPayload(title, description, color);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> post(payload));
    }

    /** Blockierende Variante fuer onDisable (kurzer Timeout). */
    public void sendBlocking(String title, String description, int color) {
        if (!enabled() || isBlocked()) {
            return;
        }
        post(buildPayload(title, description, color));
    }

    private boolean isBlocked() {
        return System.currentTimeMillis() < blockedUntil;
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
            int code = response.statusCode();
            if (code == 429) {
                long retryMs = response.headers().firstValue("Retry-After")
                        .map(DiscordWebhook::parseRetryAfter)
                        .orElse(DEFAULT_BACKOFF_MS);
                blockedUntil = System.currentTimeMillis() + retryMs;
                logThrottled("Discord-Webhook rate-limited (429) – pausiere " + (retryMs / 1000) + "s.");
            } else if (code / 100 != 2) {
                logThrottled("Discord-Webhook antwortete mit HTTP " + code);
            }
        } catch (Exception ex) {
            // Bewusst ohne URL/Payload, um keine Geheimnisse zu loggen.
            logThrottled("Discord-Webhook fehlgeschlagen: " + ex.getMessage());
        }
    }

    private static long parseRetryAfter(String value) {
        try {
            return (long) (Double.parseDouble(value.trim()) * 1000.0);
        } catch (NumberFormatException ex) {
            return DEFAULT_BACKOFF_MS;
        }
    }

    /** Loggt hoechstens einmal pro Cooldown, damit Fehler nicht spammen. */
    private void logThrottled(String message) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLog >= ERROR_LOG_COOLDOWN_MS) {
            lastErrorLog = now;
            plugin.getLogger().warning(message);
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
