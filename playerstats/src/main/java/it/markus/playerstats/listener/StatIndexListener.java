package it.markus.playerstats.listener;

import it.markus.playerstats.PlayerStatsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Haelt den {@link it.markus.playerstats.stat.StatIndex} aktuell: ein frischer
 * Schnappschuss beim Beitreten und beim Verlassen (Spieler ist dann noch online,
 * also liest getStatistic guenstig aus dem Speicher). Reines Beobachten.
 */
public final class StatIndexListener implements Listener {

    private final PlayerStatsPlugin plugin;

    public StatIndexListener(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.index().refresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.index().refresh(event.getPlayer());
    }
}
