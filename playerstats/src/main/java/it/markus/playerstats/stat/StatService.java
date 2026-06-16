package it.markus.playerstats.stat;

import it.markus.playerstats.filter.PlayerFilter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fragt Statistiken ab. Das (potenziell teure) Einlesen vieler Offline-Spieler
 * laeuft asynchron; das Ergebnis wird garantiert wieder auf dem Hauptthread
 * geliefert, sodass die Aufrufer gefahrlos Nachrichten senden koennen.
 */
public final class StatService {

    private final JavaPlugin plugin;
    private final PlayerFilter filter;

    public StatService(JavaPlugin plugin, PlayerFilter filter) {
        this.plugin = plugin;
        this.filter = filter;
    }

    /** Top-Liste nach Wert (absteigend), bereits auf {@code limit} gekuerzt. */
    public void top(StatType type, int limit, Consumer<List<StatResult>> onMain) {
        List<OfflinePlayer> players = filter.eligiblePlayers(); // Hauptthread
        runAsync(() -> {
            List<StatResult> results = new ArrayList<>();
            for (OfflinePlayer p : players) {
                long v = read(p, type);
                if (v > 0) {
                    results.add(new StatResult(p.getName(), v));
                }
            }
            results.sort(Comparator.comparingLong(StatResult::value).reversed());
            List<StatResult> trimmed = results.size() > limit
                    ? new ArrayList<>(results.subList(0, limit))
                    : results;
            deliver(onMain, trimmed);
        });
    }

    /** Summe ueber alle berechtigten Spieler. */
    public void serverTotal(StatType type, Consumer<Long> onMain) {
        List<OfflinePlayer> players = filter.eligiblePlayers();
        runAsync(() -> {
            long sum = 0;
            for (OfflinePlayer p : players) {
                sum += read(p, type);
            }
            deliver(onMain, sum);
        });
    }

    /** Einzelwert eines bestimmten Spielers. */
    public void single(OfflinePlayer player, StatType type, Consumer<Long> onMain) {
        runAsync(() -> deliver(onMain, read(player, type)));
    }

    private long read(OfflinePlayer player, StatType type) {
        try {
            return player.getStatistic(type.statistic());
        } catch (RuntimeException ex) {
            return 0L;
        }
    }

    private void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    private <T> void deliver(Consumer<T> onMain, T value) {
        Bukkit.getScheduler().runTask(plugin, () -> onMain.accept(value));
    }
}
