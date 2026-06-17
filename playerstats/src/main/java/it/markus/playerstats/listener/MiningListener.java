package it.markus.playerstats.listener;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.custom.CustomKeys;
import it.markus.playerstats.custom.CustomStatService;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zaehlt Mining-CUSTOM-Stats: meiste Bloecke pro Session (Highscore), seltene
 * Bloecke und geerntete (ausgewachsene) Pflanzen.
 *
 * Die abgebauten Gesamt-/Gruppenbloecke sind VANILLA (Statistic.MINE_BLOCK) und
 * werden hier NICHT dupliziert.
 */
public final class MiningListener implements Listener {

    private final PlayerStatsPlugin plugin;
    private final CustomStatService custom;

    /** Bloecke der aktuellen Sitzung (nicht persistiert, Reset bei Quit). */
    private final Map<UUID, Long> sessionBlocks = new ConcurrentHashMap<>();

    public MiningListener(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
        this.custom = plugin.custom();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Material type = event.getBlock().getType();

        long session = sessionBlocks.merge(uuid, 1L, Long::sum);
        custom.max(uuid, CustomKeys.SESSION_BLOCKS_BEST, session);

        if (plugin.groups().rareBlocks().contains(type)) {
            custom.add(uuid, CustomKeys.RARE_BLOCKS, 1);
        }

        if (plugin.groups().crops().contains(type)) {
            BlockData data = event.getBlock().getBlockData();
            if (data instanceof Ageable ageable && ageable.getAge() == ageable.getMaximumAge()) {
                custom.add(uuid, CustomKeys.PLANTS_HARVESTED, 1);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessionBlocks.remove(event.getPlayer().getUniqueId());
    }
}
