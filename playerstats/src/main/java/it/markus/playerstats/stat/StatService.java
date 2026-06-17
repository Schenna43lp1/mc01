package it.markus.playerstats.stat;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.filter.PlayerFilter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fragt Statistiken ab. Das (potenziell teure) Einlesen vieler Offline-Spieler
 * laeuft asynchron; das Ergebnis wird garantiert wieder auf dem Hauptthread
 * geliefert, sodass die Aufrufer gefahrlos Nachrichten senden koennen.
 *
 * Die Werte kommen ausschliesslich aus den {@link StatResolver}n der jeweiligen
 * {@link StatDefinition} – die Trennung VANILLA/GRUPPE/CUSTOM/COMPUTED ist hier
 * also transparent.
 */
public final class StatService {

    private final PlayerStatsPlugin plugin;
    private final PlayerFilter filter;

    public StatService(PlayerStatsPlugin plugin, PlayerFilter filter) {
        this.plugin = plugin;
        this.filter = filter;
    }

    /**
     * Vollstaendige, absteigend sortierte Rangliste (gefiltert nach min-value).
     * Die Paginierung uebernimmt der Aufrufer.
     */
    public void topList(StatDefinition def, Consumer<List<StatResult>> onMain) {
        List<OfflinePlayer> players = filter.eligiblePlayers(); // Hauptthread
        long minValue = plugin.config().minValue();
        runAsync(() -> {
            List<StatResult> results = new ArrayList<>();
            for (OfflinePlayer p : players) {
                double v = resolve(def, p);
                if (v > 0 && v >= minValue) {
                    results.add(new StatResult(p.getName(), v));
                }
            }
            results.sort(Comparator.comparingDouble(StatResult::value).reversed());
            deliver(onMain, results);
        });
    }

    public void serverTotal(StatDefinition def, Consumer<Double> onMain) {
        List<OfflinePlayer> players = filter.eligiblePlayers();
        runAsync(() -> {
            double sum = 0;
            for (OfflinePlayer p : players) {
                sum += resolve(def, p);
            }
            deliver(onMain, sum);
        });
    }

    public void single(OfflinePlayer player, StatDefinition def, Consumer<Double> onMain) {
        runAsync(() -> deliver(onMain, resolve(def, player)));
    }

    private double resolve(StatDefinition def, OfflinePlayer player) {
        StatContext ctx = new StatContext(player, plugin.config(), plugin.custom(), plugin.groups());
        try {
            return def.resolver().resolve(ctx);
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }

    private void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    private <T> void deliver(Consumer<T> onMain, T value) {
        Bukkit.getScheduler().runTask(plugin, () -> onMain.accept(value));
    }
}
