package it.markus.playerstats.command;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.stat.StatCategory;
import it.markus.playerstats.stat.StatDefinition;
import it.markus.playerstats.unit.UnitConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
 * /statcompare – vergleicht eine Statistik zweier Spieler nebeneinander.
 *
 *   /statcompare <stat> <spieler>           -> du gegen Spieler
 *   /statcompare <stat> <spieler1> <spieler2>
 */
public final class StatCompareCommand implements TabExecutor {

    private final PlayerStatsPlugin plugin;

    public StatCompareCommand(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            infoText(sender, "Verwendung: /statcompare <statistik> <spieler> [spieler2]");
            return true;
        }
        StatDefinition def = plugin.registry().byId(args[0]).orElse(null);
        if (def == null) {
            error(sender, "unknown-stat");
            return true;
        }

        OfflinePlayer first;
        OfflinePlayer second;
        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                error(sender, "only-players");
                return true;
            }
            first = player;
            second = Bukkit.getOfflinePlayerIfCached(args[1]);
        } else {
            first = Bukkit.getOfflinePlayerIfCached(args[1]);
            second = Bukkit.getOfflinePlayerIfCached(args[2]);
        }
        if (first == null || second == null) {
            error(sender, "player-not-found");
            return true;
        }

        String name1 = first.getName() != null ? first.getName() : "?";
        String name2 = second.getName() != null ? second.getName() : "?";

        // Beide Werte nacheinander asynchron lesen, dann gemeinsam anzeigen.
        plugin.service().single(first, def, v1 ->
                plugin.service().single(second, def, v2 ->
                        render(sender, def, name1, v1, name2, v2)));
        return true;
    }

    private void render(CommandSender sender, StatDefinition def,
                        String name1, double v1, String name2, double v2) {
        TextComponent.Builder b = Component.text();
        b.append(plugin.style().title(plugin.language().message("compare-title")))
                .append(plugin.style().text(" — "))
                .append(plugin.style().statName(plugin.language().statName(def)));

        b.append(Component.newline()).append(line(name1, v1, def));
        b.append(Component.newline()).append(line(name2, v2, def));

        if (def.category() != StatCategory.DATE) {
            double diff = v1 - v2;
            String leader = diff == 0
                    ? plugin.language().message("compare-tie")
                    : plugin.language().message("compare-leads", "%player%", diff > 0 ? name1 : name2);
            b.append(Component.newline())
                    .append(plugin.style().text(plugin.language().message("compare-diff") + ": "))
                    .append(plugin.style().value(UnitConverter.format(def.category(), Math.abs(diff), plugin.config())))
                    .append(plugin.style().text("  (" + leader + ")"));
        }
        sender.sendMessage(b.build());
    }

    private Component line(String name, double value, StatDefinition def) {
        return plugin.style().playerName(name)
                .append(plugin.style().text(": "))
                .append(plugin.style().value(UnitConverter.format(def.category(), value, plugin.config())));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String id : plugin.registry().ids()) {
                if (id.startsWith(prefix)) {
                    out.add(id);
                }
            }
        } else if (args.length == 2 || args.length == 3) {
            String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
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
}
