package it.markus.playerstats.stat;

import it.markus.playerstats.config.PluginConfig;
import it.markus.playerstats.custom.CustomStatService;
import it.markus.playerstats.group.GroupRegistry;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Alles, was ein {@link StatResolver} zum Berechnen eines Werts braucht.
 */
public record StatContext(OfflinePlayer player,
                          PluginConfig config,
                          CustomStatService custom,
                          GroupRegistry groups) {

    public UUID uuid() {
        return player.getUniqueId();
    }
}
