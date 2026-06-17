package it.markus.playerstats.listener;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.custom.CustomKeys;
import it.markus.playerstats.custom.CustomStatService;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

/**
 * Zaehlt Kampf-CUSTOM-Stats: Killstreaks (aktuell/laengster), abgeschossene und
 * treffende Pfeile (fuer die berechnete Trefferquote) und – optional – Headshots.
 */
public final class CombatListener implements Listener {

    private final PlayerStatsPlugin plugin;
    private final CustomStatService custom;

    public CombatListener(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
        this.custom = plugin.custom();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            long streak = custom.addAndGet(killer.getUniqueId(), CustomKeys.KILLSTREAK_CURRENT, 1);
            custom.max(killer.getUniqueId(), CustomKeys.KILLSTREAK_LONGEST, streak);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        custom.set(event.getEntity().getUniqueId(), CustomKeys.KILLSTREAK_CURRENT, 0);
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (plugin.config().trackArrowAccuracy() && event.getEntity() instanceof Player player) {
            custom.add(player.getUniqueId(), CustomKeys.ARROWS_SHOT, 1);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!plugin.config().trackArrowAccuracy()) {
            return;
        }
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof AbstractArrow)) {
            return;
        }
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player player)) {
            return;
        }
        if (!(event.getHitEntity() instanceof LivingEntity target)) {
            return; // nur Treffer an Lebewesen zaehlen
        }
        UUID uuid = player.getUniqueId();
        custom.add(uuid, CustomKeys.ARROWS_HIT, 1);

        if (plugin.config().trackHeadshots()) {
            double projectileY = projectile.getLocation().getY();
            if (projectileY >= target.getEyeLocation().getY() - 0.2) {
                custom.add(uuid, CustomKeys.HEADSHOTS, 1);
            }
        }
    }
}
