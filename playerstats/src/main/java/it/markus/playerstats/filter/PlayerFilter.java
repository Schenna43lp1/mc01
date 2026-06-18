package it.markus.playerstats.filter;

import it.markus.playerstats.config.PluginConfig;
import it.markus.playerstats.exclude.ExcludeManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Entscheidet, welche Spieler in Top-/Server-Statistiken einfliessen.
 *
 * Reihenfolge der Filter:
 *   1. Whitelist (optional: nur freigegebene Spieler)
 *   2. gebannte Spieler ausschliessen (optional)
 *   3. manuell ausgeschlossene Spieler (/playerstats exclude)
 *   4. Letzter-Login-Fenster (optional, fuer Relevanz & Performance)
 *
 * {@link #snapshotFilter()} liest die teuren Sammlungen (Whitelist) EINMAL und
 * liefert ein wiederverwendbares Praedikat – gedacht fuer die Filterung vieler
 * Index-Eintraege auf dem Hauptthread, ohne die Whitelist je Eintrag neu zu lesen.
 */
public final class PlayerFilter {

    private final Supplier<PluginConfig> config;
    private final ExcludeManager excludes;

    public PlayerFilter(Supplier<PluginConfig> config, ExcludeManager excludes) {
        this.config = config;
        this.excludes = excludes;
    }

    /**
     * Erstellt einen Schnappschuss-Filter. Die Whitelist und das Zeitfenster
     * werden hier einmal ausgewertet; das zurueckgegebene Praedikat ist danach
     * pro Spieler guenstig auswertbar. Auf dem Hauptthread aufrufen.
     */
    public Predicate<OfflinePlayer> snapshotFilter() {
        PluginConfig cfg = config.get();

        Set<UUID> whitelist = null;
        if (cfg.useWhitelist()) {
            whitelist = new HashSet<>();
            for (OfflinePlayer p : Bukkit.getWhitelistedPlayers()) {
                whitelist.add(p.getUniqueId());
            }
        }
        final Set<UUID> wl = whitelist;
        final boolean excludeBanned = cfg.excludeBanned();
        final long cutoff = cfg.maxLastJoinDays() > 0
                ? System.currentTimeMillis() - cfg.maxLastJoinDays() * 86_400_000L
                : -1L;

        return p -> {
            if (p.getName() == null) {
                return false;
            }
            if (excludeBanned && p.isBanned()) {
                return false;
            }
            if (excludes.isExcluded(p.getUniqueId())) {
                return false;
            }
            if (wl != null && !wl.contains(p.getUniqueId())) {
                return false;
            }
            if (cutoff > 0) {
                long lastSeen = p.getLastSeen();
                if (lastSeen > 0 && lastSeen < cutoff) {
                    return false;
                }
            }
            return true;
        };
    }
}
