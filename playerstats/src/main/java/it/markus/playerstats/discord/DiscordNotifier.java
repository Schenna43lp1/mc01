package it.markus.playerstats.discord;

import it.markus.playerstats.PlayerStatsPlugin;

/**
 * Formt die einzelnen Status-Ereignisse und schickt sie – je nach Config-
 * Schaltern – an den {@link DiscordWebhook}. Farben sind je Ereignis fest.
 */
public final class DiscordNotifier {

    private static final int GREEN = 0x2ECC71;
    private static final int RED = 0xE74C3C;
    private static final int BLUE = 0x3498DB;
    private static final int ORANGE = 0xE67E22;

    private final PlayerStatsPlugin plugin;
    private final DiscordWebhook webhook;

    public DiscordNotifier(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
        this.webhook = new DiscordWebhook(plugin);
    }

    public void serverOnline() {
        if (event("server-online")) {
            webhook.send(message("server-online"), null, GREEN);
        }
    }

    public void serverOffline() {
        if (event("server-offline")) {
            // blockierend: beim Stopp ist der Scheduler nicht mehr nutzbar
            webhook.sendBlocking(message("server-offline"), null, RED);
        }
    }

    public void playerJoin(String name, int online) {
        if (event("player-join")) {
            webhook.send(format(message("player-join"), name, online, null), null, GREEN);
        }
    }

    public void playerQuit(String name, int online) {
        if (event("player-quit")) {
            webhook.send(format(message("player-quit"), name, online, null), null, BLUE);
        }
    }

    public void updateAvailable(String version) {
        if (event("update-available")) {
            webhook.send(format(message("update-available"), null, -1, version), null, ORANGE);
        }
    }

    private boolean event(String key) {
        return plugin.config().discordEvent(key);
    }

    private String message(String key) {
        return plugin.config().discordMessage(key);
    }

    private String format(String template, String player, int online, String version) {
        String text = template;
        if (player != null) {
            text = text.replace("%player%", player);
        }
        if (online >= 0) {
            text = text.replace("%online%", String.valueOf(online));
        }
        if (version != null) {
            text = text.replace("%version%", version);
        }
        return text;
    }
}
