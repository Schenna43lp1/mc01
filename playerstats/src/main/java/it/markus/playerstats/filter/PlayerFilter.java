package it.markus.playerstats.filter;

import it.markus.playerstats.config.PluginConfig;
import it.markus.playerstats.exclude.ExcludeManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Entscheidet, welche Spieler in Top-/Server-Statistiken einfliessen.
 *
 * Reihenfolge der Filter:
 *   1. Whitelist (optional: nur freigegebene Spieler)
 *   2. gebannte Spieler ausschliessen (optional)
 *   3. manuell ausgeschlossene Spieler (/statexclude)
 *   4. Letzter-Login-Fenster (optional, fuer Relevanz & Performance)
 *
 * Wichtig: {@link #eligiblePlayers()} ruft Bukkit-Sammlungen ab und muss daher
 * auf dem Hauptthread laufen. Das (teure) Einlesen der eigentlichen Statistiken
 * passiert danach asynchron im {@code StatService}.
 */
public final class PlayerFilter {

    private final Supplier<PluginConfig> config;
    private final ExcludeManager excludes;

    public PlayerFilter(Supplier<PluginConfig> config, ExcludeManager excludes) {
        this.config = config;
        this.excludes = excludes;
    }

    public List<OfflinePlayer> eligiblePlayers() {
        PluginConfig cfg = config.get();

        Set<UUID> whitelist = null;
        if (cfg.useWhitelist()) {
            whitelist = new HashSet<>();
            for (OfflinePlayer p : Bukkit.getWhitelistedPlayers()) {
                whitelist.add(p.getUniqueId());
            }
        }

        long cutoff = cfg.maxLastJoinDays() > 0
                ? System.currentTimeMillis() - cfg.maxLastJoinDays() * 86_400_000L
                : -1L;

        List<OfflinePlayer> result = new ArrayList<>();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getName() == null) {
                continue;
            }
            if (cfg.excludeBanned() && p.isBanned()) {
                continue;
            }
            if (excludes.isExcluded(p.getUniqueId())) {
                continue;
            }
            if (whitelist != null && !whitelist.contains(p.getUniqueId())) {
                continue;
            }
            if (cutoff > 0) {
                long lastSeen = p.getLastSeen();
                if (lastSeen > 0 && lastSeen < cutoff) {
                    continue;
                }
            }
            result.add(p);
        }
        return result;
    }
}
