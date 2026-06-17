package it.markus.playerstats.listener;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.custom.CustomKeys;
import it.markus.playerstats.custom.CustomStatService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Pflegt den Login-Streak: aufeinanderfolgende Kalendertage mit Login.
 * Bricht ab, wenn ein Tag uebersprungen wurde; haelt zusaetzlich den Rekord.
 */
public final class SessionListener implements Listener {

    private final CustomStatService custom;

    public SessionListener(PlayerStatsPlugin plugin) {
        this.custom = plugin.custom();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        long today = LocalDate.now(ZoneId.systemDefault()).toEpochDay();
        long lastDay = custom.get(uuid, CustomKeys.LOGIN_LAST_DAY);

        if (lastDay == today) {
            return; // heute schon gezaehlt
        }

        long streak = (lastDay == today - 1)
                ? custom.get(uuid, CustomKeys.LOGIN_STREAK_CURRENT) + 1
                : 1;

        custom.set(uuid, CustomKeys.LOGIN_STREAK_CURRENT, streak);
        custom.max(uuid, CustomKeys.LOGIN_STREAK_LONGEST, streak);
        custom.set(uuid, CustomKeys.LOGIN_LAST_DAY, today);
    }
}
