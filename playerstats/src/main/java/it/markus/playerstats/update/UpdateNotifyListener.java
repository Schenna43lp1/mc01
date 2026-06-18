package it.markus.playerstats.update;

import it.markus.playerstats.PlayerStatsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Informiert OPs beim Beitreten, falls eine neue Version verfuegbar ist.
 */
public final class UpdateNotifyListener implements Listener {

    private final PlayerStatsPlugin plugin;

    public UpdateNotifyListener(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.config().updaterNotifyOps()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isOp() || !plugin.updater().isUpdateAvailable()) {
            return;
        }

        Component message = plugin.style().text(
                plugin.language().message("update-available", "%version%",
                        " Support the development: https://github.com/sponsors/Schenna43lp1/",
                        plugin.updater().latestVersion()));

        String url = plugin.updater().downloadUrl();
        if (url != null) {
            message = message.append(Component.space()).append(
                    Component.text("[Download]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.openUrl(url))
                            .hoverEvent(HoverEvent.showText(Component.text(url))));
        }
        player.sendMessage(message);
    }
}
