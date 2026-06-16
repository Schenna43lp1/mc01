package it.markus.serverstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Prueft regelmaessig, ob online befindliche Spieler einen Spielzeit-Meilenstein
 * erreicht haben, und kuendigt diesen einmalig an.
 *
 * Welche Meilensteine bereits angekuendigt wurden, merkt sich der
 * {@link PlayerStats#getMilestonesReached()}-Zaehler – so wird jeder Meilenstein
 * pro Spieler nur ein einziges Mal ausgeloest, auch ueber Neustarts hinweg.
 */
public final class MilestoneService {

    private final ServerStats plugin;
    private final StatsStore store;

    public MilestoneService(ServerStats plugin, StatsStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    /** Prueft alle aktuell online befindlichen Spieler. */
    public void checkAll() {
        if (!plugin.getConfig().getBoolean("milestones.enabled", true)) {
            return;
        }
        long now = System.currentTimeMillis();
        List<Integer> hours = plugin.getConfig().getIntegerList("milestones.hours");
        if (hours.isEmpty()) {
            return;
        }
        String template = plugin.getConfig().getString("milestones.broadcast", "");
        boolean changed = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStats p = store.get(player.getUniqueId());
            if (p == null) {
                continue;
            }
            double totalHours = store.getTotalPlaytimeMs(player.getUniqueId(), now) / 3_600_000.0;
            int reached = p.getMilestonesReached();

            // Meilensteine sind aufsteigend sortiert; ggf. mehrere auf einmal.
            while (reached < hours.size() && totalHours >= hours.get(reached)) {
                int milestoneHours = hours.get(reached);
                Bukkit.getServer().broadcast(Msg.render(template,
                        "%player%", player.getName(),
                        "%hours%", String.valueOf(milestoneHours)));
                reached++;
                changed = true;
            }
            p.setMilestonesReached(reached);
        }

        if (changed) {
            store.save();
        }
    }
}
