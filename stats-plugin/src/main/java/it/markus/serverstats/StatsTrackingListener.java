package it.markus.serverstats;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Zaehlt die Gameplay-Statistiken ueber Events:
 * Tode, Mob-Kills, abgebaute Bloecke und zurueckgelegte Distanz.
 *
 * Hinweis: PlayerMoveEvent feuert haeufig – fuer kleine/mittlere Server ist das
 * unproblematisch. Auf sehr grossen Servern wuerde man die Distanz stattdessen
 * aus den Vanilla-Statistiken lesen.
 */
public final class StatsTrackingListener implements Listener {

    private final StatsStore store;

    public StatsTrackingListener(StatsStore store) {
        this.store = store;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        store.recordDeath(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Nur echte Mobs zaehlen – Spieler-Tode sind oben abgedeckt.
        if (event.getEntity() instanceof Player) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            store.recordMobKill(killer.getUniqueId());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        store.recordBlockMined(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        if (dx == 0 && dz == 0) {
            // Nur Kopfdrehung/Hoehe geaendert -> keine horizontale Strecke.
            return;
        }
        long cm = Math.round(Math.sqrt(dx * dx + dz * dz) * 100.0);
        if (cm > 0) {
            store.recordDistanceCm(event.getPlayer().getUniqueId(), cm);
        }
    }
}
