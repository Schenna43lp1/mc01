package it.markus.playerstats.stat;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.filter.PlayerFilter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * In-Memory-Index der aufgeloesten Statistik-Werte je Spieler.
 *
 * Warum: top/rank/average/server iterierten frueher live ueber ALLE
 * Offline-Spieler und riefen dabei {@code OfflinePlayer.getStatistic(...)}
 * im Async-Thread auf – das blockiert bei vielen Spielern und ist off-thread
 * nicht sauber. Stattdessen wird hier ein Cache gepflegt:
 *
 *   - aktualisiert bei Join/Quit und periodisch (nur ONLINE-Spieler, billig),
 *   - beim Start gebatcht aus den Offline-Spielern „aufgewaermt" (wenige pro Tick),
 *   - IMMER auf dem Hauptthread befuellt – also keine Bukkit-API off-thread.
 *
 * Abfragen lesen nur noch aus dem Cache (in-memory), die Filterung nutzt einen
 * einmaligen {@link PlayerFilter#snapshotFilter()}.
 */
public final class StatIndex {

    private record Entry(String name, Map<String, Double> values) {
    }

    private final PlayerStatsPlugin plugin;
    private final PlayerFilter filter;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    public StatIndex(PlayerStatsPlugin plugin, PlayerFilter filter) {
        this.plugin = plugin;
        this.filter = filter;
    }

    /** Aktualisiert den Eintrag eines Spielers. NUR auf dem Hauptthread aufrufen. */
    public void refresh(OfflinePlayer player) {
        if (player.getName() == null) {
            return;
        }
        StatContext ctx = new StatContext(player, plugin.config(), plugin.custom(), plugin.groups());
        Map<String, Double> values = new HashMap<>();
        for (StatDefinition def : plugin.registry().all()) {
            double v;
            try {
                v = def.resolver().resolve(ctx);
            } catch (RuntimeException ex) {
                v = 0.0;
            }
            values.put(def.id(), v);
        }
        entries.put(player.getUniqueId(), new Entry(player.getName(), values));
    }

    /** Periodische, guenstige Aktualisierung (Online-Spieler lesen aus dem Speicher). */
    public void refreshOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            refresh(p);
        }
    }

    /**
     * Befuellt den Index schrittweise aus allen Offline-Spielern – wenige pro
     * Tick auf dem Hauptthread, um keinen Stall zu verursachen. 0 = deaktiviert.
     */
    public void startWarmUp() {
        int batch = plugin.config().indexWarmupPerTick();
        if (batch <= 0) {
            return;
        }
        Deque<OfflinePlayer> queue = new ArrayDeque<>(Arrays.asList(Bukkit.getOfflinePlayers()));
        new BukkitRunnable() {
            @Override
            public void run() {
                int n = 0;
                while (n < batch && !queue.isEmpty()) {
                    refresh(queue.poll());
                    n++;
                }
                if (queue.isEmpty()) {
                    cancel();
                    plugin.getLogger().info("Stat-Index aufgewaermt (" + entries.size() + " Spieler).");
                }
            }
        }.runTaskTimer(plugin, 40L, 1L);
    }

    /** Wert eines Spielers aus dem Cache (0, falls unbekannt). */
    public double value(UUID uuid, String statId) {
        Entry e = entries.get(uuid);
        if (e == null) {
            return 0.0;
        }
        Double v = e.values().get(statId);
        return v == null ? 0.0 : v;
    }

    /** Absteigend sortierte Rangliste aus dem Cache (gefiltert, min-value beachtet). */
    public List<StatResult> top(StatDefinition def) {
        Predicate<OfflinePlayer> eligible = filter.snapshotFilter();
        long minValue = plugin.config().minValue();
        List<StatResult> out = new ArrayList<>();
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            Double v = e.getValue().values().get(def.id());
            if (v == null || v <= 0 || v < minValue) {
                continue;
            }
            if (!eligible.test(Bukkit.getOfflinePlayer(e.getKey()))) {
                continue;
            }
            out.add(new StatResult(e.getValue().name(), v));
        }
        out.sort(Comparator.comparingDouble(StatResult::value).reversed());
        return out;
    }

    /** Summe ueber alle berechtigten Spieler (aus dem Cache). */
    public double total(StatDefinition def) {
        Predicate<OfflinePlayer> eligible = filter.snapshotFilter();
        double sum = 0;
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            Double v = e.getValue().values().get(def.id());
            if (v == null) {
                continue;
            }
            if (!eligible.test(Bukkit.getOfflinePlayer(e.getKey()))) {
                continue;
            }
            sum += v;
        }
        return sum;
    }

    public int size() {
        return entries.size();
    }
}
