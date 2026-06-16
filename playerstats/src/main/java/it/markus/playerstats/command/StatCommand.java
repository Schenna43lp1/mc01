package it.markus.playerstats.command;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.stat.StatResult;
import it.markus.playerstats.stat.StatType;
import it.markus.playerstats.unit.UnitConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * /playerstats – zeigt eigene/fremde Statistiken, Top-Liste und Server-Gesamt,
 * verwaltet Reload und das Teilen im Chat.
 */
public final class StatCommand implements TabExecutor {

    private final PlayerStatsPlugin plugin;
    /** Letztes Ergebnis je Spieler – fuer den /playerstats share-Button. */
    private final Map<UUID, Component> shareCache = new HashMap<>();

    public StatCommand(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            infoText(sender, "Verwendung: /playerstats <statistik> [me|top|server|player <name>] | list | reload");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> { handleReload(sender); return true; }
            case "list" -> { handleList(sender); return true; }
            case "share" -> { handleShare(sender); return true; }
            default -> { /* faellt durch zur Statistik-Auswertung */ }
        }

        StatType type = StatType.byId(args[0]).orElse(null);
        if (type == null) {
            error(sender, "unknown-stat");
            return true;
        }

        String mode = args.length >= 2
                ? args[1].toLowerCase(Locale.ROOT)
                : (sender instanceof Player ? "me" : "top");

        switch (mode) {
            case "me" -> {
                if (sender instanceof Player player) {
                    showSelf(sender, player, type);
                } else {
                    error(sender, "only-players");
                }
            }
            case "top" -> showTop(sender, type);
            case "server" -> showServer(sender, type);
            case "player" -> {
                if (args.length < 3) {
                    infoText(sender, "Verwendung: /playerstats " + type.id() + " player <name>");
                } else {
                    showOther(sender, args[2], type);
                }
            }
            // jedes andere zweite Argument wird als Spielername interpretiert
            default -> showOther(sender, args[1], type);
        }
        return true;
    }

    // --- Subcommands -------------------------------------------------------

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("playerstats.reload")) {
            error(sender, "no-permission");
            return;
        }
        plugin.reload();
        success(sender, "reload-success");
    }

    private void handleList(CommandSender sender) {
        Component header = plugin.style().text(plugin.language().message("list-header"));
        Component ids = plugin.style().value(String.join(", ", StatType.ids()));
        sender.sendMessage(header.append(Component.newline()).append(ids));
    }

    private void handleShare(CommandSender sender) {
        if (!sender.hasPermission("playerstats.share")) {
            error(sender, "no-permission");
            return;
        }
        if (!(sender instanceof Player player)) {
            error(sender, "only-players");
            return;
        }
        Component cached = shareCache.remove(player.getUniqueId());
        if (cached == null) {
            infoText(sender, plugin.language().message("share-empty"));
            return;
        }
        Component header = plugin.style().text(plugin.language().message("share-by", "%player%", player.getName()));
        Bukkit.broadcast(header.append(Component.newline()).append(cached));
    }

    // --- Anzeige -----------------------------------------------------------

    private void showSelf(CommandSender sender, Player player, StatType type) {
        plugin.service().single(player, type, value -> {
            Component header = plugin.style().text(plugin.language().message("your-stat"));
            deliver(sender, header.append(Component.newline()).append(statLine(type, value)));
        });
    }

    private void showOther(CommandSender sender, String name, StatType type) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
        if (target == null) {
            error(sender, "player-not-found");
            return;
        }
        String displayName = target.getName() != null ? target.getName() : name;
        plugin.service().single(target, type, value -> {
            Component header = plugin.style().text(
                    plugin.language().message("player-stat", "%player%", displayName));
            deliver(sender, header.append(Component.newline()).append(statLine(type, value)));
        });
    }

    private void showTop(CommandSender sender, StatType type) {
        infoText(sender, plugin.language().message("calculating"));
        int limit = plugin.config().topListSize();
        plugin.service().top(type, limit, results -> {
            TextComponent.Builder b = Component.text();
            b.append(plugin.style().title(plugin.language().message("top-title",
                            "%size%", String.valueOf(results.size()))))
                    .append(plugin.style().text(" — "))
                    .append(plugin.style().statName(plugin.language().statName(type)));

            if (results.isEmpty()) {
                b.append(Component.newline()).append(plugin.style().text("(keine Daten)"));
            } else {
                int rank = 1;
                for (StatResult r : results) {
                    String valueStr = UnitConverter.format(type.category(), r.value(), plugin.config());
                    b.append(Component.newline())
                            .append(plugin.style().rank(rank))
                            .append(Component.space())
                            .append(plugin.style().playerName(r.playerName()))
                            .append(plugin.style().text(" — "))
                            .append(plugin.style().value(valueStr));
                    rank++;
                }
            }
            deliver(sender, b.build());
        });
    }

    private void showServer(CommandSender sender, StatType type) {
        infoText(sender, plugin.language().message("calculating"));
        plugin.service().serverTotal(type, sum -> {
            String valueStr = UnitConverter.format(type.category(), sum, plugin.config());
            Component out = plugin.style().title(plugin.language().message("server-title"))
                    .append(plugin.style().text(" — "))
                    .append(plugin.style().statName(plugin.language().statName(type)))
                    .append(plugin.style().text(": "))
                    .append(plugin.style().value(valueStr));
            deliver(sender, out);
        });
    }

    private Component statLine(StatType type, long value) {
        String valueStr = UnitConverter.format(type.category(), value, plugin.config());
        return plugin.style().statName(plugin.language().statName(type))
                .append(plugin.style().text(": "))
                .append(plugin.style().value(valueStr));
    }

    /** Sendet das Ergebnis und haengt – falls erlaubt – den Teilen-Button an. */
    private void deliver(CommandSender sender, Component content) {
        if (sender instanceof Player player && player.hasPermission("playerstats.share")) {
            shareCache.put(player.getUniqueId(), content);
            sender.sendMessage(content.append(Component.newline()).append(shareButton()));
        } else {
            sender.sendMessage(content);
        }
    }

    private Component shareButton() {
        return Component.text("[Teilen]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/playerstats share"))
                .hoverEvent(HoverEvent.showText(Component.text("Statistik im Chat teilen")));
    }

    // --- kleine Helfer -----------------------------------------------------

    private void infoText(CommandSender sender, String text) {
        sender.sendMessage(plugin.style().text(text));
    }

    private void error(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(Component.text(plugin.language().message(key, placeholders), NamedTextColor.RED));
    }

    private void success(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(Component.text(plugin.language().message(key, placeholders), NamedTextColor.GREEN));
    }

    // --- Tab-Vervollstaendigung -------------------------------------------

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> options = new ArrayList<>(StatType.ids());
            options.add("list");
            options.add("share");
            if (sender.hasPermission("playerstats.reload")) {
                options.add("reload");
            }
            addMatching(out, options, args[0]);
        } else if (args.length == 2 && StatType.byId(args[0]).isPresent()) {
            addMatching(out, List.of("me", "top", "server", "player"), args[1]);
        } else if (args.length == 3 && args[1].equalsIgnoreCase("player")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }

    private void addMatching(List<String> out, List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(o);
            }
        }
    }
}
