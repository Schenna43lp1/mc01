package it.markus.playerstats.command;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.stat.StatDefinition;
import it.markus.playerstats.stat.StatKind;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /statreset – setzt CUSTOM-Statistiken eines Spielers zurueck (einzeln oder alle).
 * Vanilla-Statistiken koennen nicht zurueckgesetzt werden (read-only).
 */
public final class StatResetCommand implements TabExecutor {

    private final PlayerStatsPlugin plugin;

    public StatResetCommand(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("playerstats.reset")) {
            error(sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            infoText(sender, "Verwendung: /playerstats reset <spieler> [statistik|all]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null) {
            error(sender, "player-not-found");
            return true;
        }
        String name = target.getName() != null ? target.getName() : args[0];

        if (args.length < 2 || args[1].equalsIgnoreCase("all")) {
            plugin.custom().reset(target.getUniqueId(), null);
            success(sender, "reset-all", "%player%", name);
            return true;
        }

        StatDefinition def = plugin.registry().byId(args[1]).orElse(null);
        if (def == null) {
            error(sender, "unknown-stat");
            return true;
        }
        if (def.kind() != StatKind.CUSTOM || def.customKey() == null) {
            error(sender, "not-resettable");
            return true;
        }
        plugin.custom().reset(target.getUniqueId(), def.customKey());
        success(sender, "reset-stat", "%player%", name, "%stat%", def.id());
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 2) {
            List<String> options = new ArrayList<>();
            options.add("all");
            for (StatDefinition def : plugin.registry().all()) {
                if (def.kind() == StatKind.CUSTOM) {
                    options.add(def.id());
                }
            }
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (String o : options) {
                if (o.startsWith(prefix)) {
                    out.add(o);
                }
            }
        }
        return out;
    }

    private void infoText(CommandSender sender, String text) {
        sender.sendMessage(plugin.style().text(text));
    }

    private void error(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(Component.text(plugin.language().message(key, placeholders), NamedTextColor.RED));
    }

    private void success(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(Component.text(plugin.language().message(key, placeholders), NamedTextColor.GREEN));
    }
}
