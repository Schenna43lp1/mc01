package it.markus.serverstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Reagiert auf Beitritt und Verlassen: aktualisiert die Statistiken und
 * ersetzt – je nach Config – die Standard-Broadcast-Nachricht. Brandneue
 * Spieler bekommen optional eine eigene Willkommens-Nachricht.
 */
public final class PlayerEventListener implements Listener {

    private final ServerStats plugin;
    private final StatsStore store;

    public PlayerEventListener(ServerStats plugin, StatsStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();

        boolean firstEver = store.handleJoin(player, now);

        String online = String.valueOf(Bukkit.getOnlinePlayers().size());
        int joinCount = store.get(player.getUniqueId()).getJoinCount();

        // Erst-Beitritt bekommt – falls aktiviert – eine eigene Broadcast-Nachricht.
        if (firstEver && plugin.getConfig().getBoolean("first-join.enabled", true)) {
            String raw = plugin.getConfig().getString("first-join.broadcast", "");
            event.joinMessage(Msg.render(raw, "%player%", player.getName(), "%online%", online));
        } else if (plugin.getConfig().getBoolean("broadcast.join-enabled", true)) {
            String raw = plugin.getConfig().getString("broadcast.join-message", "");
            event.joinMessage(Msg.render(raw, "%player%", player.getName(), "%online%", online));
        }

        // Private Begruessung an den Spieler selbst.
        String privateRaw = plugin.getConfig().getString("welcome-private", "");
        if (privateRaw != null && !privateRaw.isBlank()) {
            player.sendMessage(Msg.render(privateRaw,
                    "%player%", player.getName(),
                    "%online%", online,
                    "%joincount%", String.valueOf(joinCount)));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        store.handleQuit(player, System.currentTimeMillis());

        if (plugin.getConfig().getBoolean("broadcast.quit-enabled", true)) {
            String raw = plugin.getConfig().getString("broadcast.quit-message", "");
            event.quitMessage(Msg.render(raw,
                    "%player%", player.getName(),
                    "%online%", String.valueOf(Bukkit.getOnlinePlayers().size() - 1)));
        }
    }
}
