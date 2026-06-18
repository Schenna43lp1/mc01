package it.markus.playerstats.discord;

import it.markus.playerstats.PlayerStatsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Leitet Join/Quit an den {@link DiscordNotifier} weiter.
 */
public final class DiscordListener implements Listener {

    private final PlayerStatsPlugin plugin;

    public DiscordListener(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.discord().playerJoin(event.getPlayer().getName(), Bukkit.getOnlinePlayers().size());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // der gehende Spieler zaehlt hier noch mit -> -1
        plugin.discord().playerQuit(event.getPlayer().getName(),
                Math.max(0, Bukkit.getOnlinePlayers().size() - 1));
    }
}
