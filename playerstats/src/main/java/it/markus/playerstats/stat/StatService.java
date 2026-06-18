package it.markus.playerstats.stat;

import it.markus.playerstats.PlayerStatsPlugin;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Fragt Statistiken ab. top/server lesen aus dem {@link StatIndex}-Cache
 * (in-memory, Hauptthread, threadsicher) statt live ueber alle Offline-Spieler
 * zu iterieren. Einzelabfragen werden direkt aufgeloest.
 *
 * Die callback-basierte Signatur bleibt erhalten, damit die Aufrufer
 * unveraendert funktionieren; die Zustellung erfolgt synchron auf dem
 * Hauptthread (es gibt keine Off-Thread-Bukkit-Aufrufe mehr).
 */
public final class StatService {

    private final PlayerStatsPlugin plugin;

    public StatService(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    public void topList(StatDefinition def, Consumer<List<StatResult>> onMain) {
        onMain.accept(plugin.index().top(def));
    }

    public void serverTotal(StatDefinition def, Consumer<Double> onMain) {
        onMain.accept(plugin.index().total(def));
    }

    public void single(OfflinePlayer player, StatDefinition def, Consumer<Double> onMain) {
        onMain.accept(resolve(def, player));
    }

    private double resolve(StatDefinition def, OfflinePlayer player) {
        StatContext ctx = new StatContext(player, plugin.config(), plugin.custom(), plugin.groups());
        try {
            return def.resolver().resolve(ctx);
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }
}
