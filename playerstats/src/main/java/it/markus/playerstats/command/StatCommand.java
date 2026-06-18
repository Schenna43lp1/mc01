package it.markus.playerstats.command;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.stat.StatDefinition;
import it.markus.playerstats.stat.StatResult;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Zentraler Befehl /playerstats. Alles laeuft hierueber – Abfragen, Rangliste,
 * Vergleich, Verwaltung. Die fachliche Logik fuer compare/exclude/reset liegt
 * in eigenen Klassen, an die hier nur delegiert wird.
 */
public final class StatCommand implements TabExecutor {

    private final PlayerStatsPlugin plugin;
    private final Map<UUID, Component> shareCache = new HashMap<>();

    // delegierte Sub-Handler
    private final StatCompareCommand compareCmd;
    private final StatExcludeCommand excludeCmd;
    private final StatResetCommand resetCmd;

    public StatCommand(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
        this.compareCmd = new StatCompareCommand(plugin);
        this.excludeCmd = new StatExcludeCommand(plugin);
        this.resetCmd = new StatResetCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            handleHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "share" -> handleShare(sender);
            case "info" -> handleInfo(sender, args);
            case "help", "?" -> handleHelp(sender);
            case "version" -> handleVersion(sender);
            case "search" -> handleSearch(sender, args);
            case "rank" -> handleRank(sender, args);
            case "average", "avg" -> handleAverage(sender, args);
            case "compare" -> compareCmd.onCommand(sender, command, label, shift(args));
            case "exclude" -> excludeCmd.onCommand(sender, command, label, shift(args));
            case "reset" -> resetCmd.onCommand(sender, command, label, shift(args));
            default -> handleStat(sender, args);
        }
        return true;
    }

    // --- Statistik-Abfrage -------------------------------------------------

    private void handleStat(CommandSender sender, String[] args) {
        StatDefinition def = plugin.registry().byId(args[0]).orElse(null);
        if (def == null) {
            error(sender, "unknown-stat");
            return;
        }
        String mode = args.length >= 2
                ? args[1].toLowerCase(Locale.ROOT)
                : (sender instanceof Player ? "me" : "top");

        switch (mode) {
            case "me" -> {
                if (sender instanceof Player player) {
                    showSelf(sender, player, def);
                } else {
                    error(sender, "only-players");
                }
            }
            case "top" -> showTop(sender, def, parsePage(args, 2));
            case "server" -> showServer(sender, def);
            case "player" -> {
                if (args.length < 3) {
                    infoText(sender, "Verwendung: /playerstats " + def.id() + " player <name>");
                } else {
                    showOther(sender, args[2], def);
                }
            }
            default -> showOther(sender, args[1], def);
        }
    }

    private void showSelf(CommandSender sender, Player player, StatDefinition def) {
        plugin.service().single(player, def, value -> {
            Component header = plugin.style().text(plugin.language().message("your-stat"));
            deliver(sender, header.append(Component.newline()).append(statLine(def, value)));
        });
    }

    private void showOther(CommandSender sender, String name, StatDefinition def) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
        if (target == null) {
            error(sender, "player-not-found");
            return;
        }
        String displayName = target.getName() != null ? target.getName() : name;
        plugin.service().single(target, def, value -> {
            Component header = plugin.style().text(
                    plugin.language().message("player-stat", "%player%", displayName));
            deliver(sender, header.append(Component.newline()).append(statLine(def, value)));
        });
    }

    private void showTop(CommandSender sender, StatDefinition def, int page) {
        infoText(sender, plugin.language().message("calculating"));
        plugin.service().topList(def, results -> {
            int pageSize = plugin.config().topPageSize();
            int total = results.size();
            int pages = Math.max(1, (total + pageSize - 1) / pageSize);
            int current = Math.min(Math.max(1, page), pages);
            int from = (current - 1) * pageSize;
            int to = Math.min(from + pageSize, total);

            TextComponent.Builder b = Component.text();
            b.append(plugin.style().title(plugin.language().message("top-title",
                            "%size%", String.valueOf(total))))
                    .append(plugin.style().text(" — "))
                    .append(plugin.style().statName(plugin.language().statName(def)))
                    .append(plugin.style().text("  (Seite " + current + "/" + pages + ")"));

            if (total == 0) {
                b.append(Component.newline()).append(plugin.style().text("(keine Daten)"));
            } else {
                for (int i = from; i < to; i++) {
                    StatResult r = results.get(i);
                    String valueStr = UnitConverter.format(def.category(), r.value(), plugin.config());
                    b.append(Component.newline())
                            .append(rankComponent(i + 1))
                            .append(Component.space())
                            .append(plugin.style().playerName(r.playerName()))
                            .append(plugin.style().text(" — "))
                            .append(plugin.style().value(valueStr));
                }
                if (pages > 1) {
                    b.append(Component.newline()).append(pagination(def, current, pages));
                }
            }
            deliver(sender, b.build());
        });
    }

    private void showServer(CommandSender sender, StatDefinition def) {
        infoText(sender, plugin.language().message("calculating"));
        plugin.service().serverTotal(def, sum -> {
            String valueStr = UnitConverter.format(def.category(), sum, plugin.config());
            Component out = plugin.style().title(plugin.language().message("server-title"))
                    .append(plugin.style().text(" — "))
                    .append(plugin.style().statName(plugin.language().statName(def)))
                    .append(plugin.style().text(": "))
                    .append(plugin.style().value(valueStr));
            deliver(sender, out);
        });
    }

    // --- Neue Subcommands --------------------------------------------------

    private void handleRank(CommandSender sender, String[] args) {
        if (args.length < 2) {
            infoText(sender, "Verwendung: /playerstats rank <statistik> [spieler]");
            return;
        }
        StatDefinition def = plugin.registry().byId(args[1]).orElse(null);
        if (def == null) {
            error(sender, "unknown-stat");
            return;
        }
        String name;
        if (args.length >= 3) {
            if (!sender.hasPermission("playerstats.stats.others")) {
                error(sender, "no-permission");
                return;
            }
            name = args[2];
        } else if (sender instanceof Player player) {
            name = player.getName();
        } else {
            error(sender, "only-players");
            return;
        }

        infoText(sender, plugin.language().message("calculating"));
        plugin.service().topList(def, results -> {
            int pos = -1;
            double value = 0;
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).playerName().equalsIgnoreCase(name)) {
                    pos = i + 1;
                    value = results.get(i).value();
                    break;
                }
            }
            Component head = plugin.style().playerName(name).append(plugin.style().text(" — "))
                    .append(plugin.style().statName(plugin.language().statName(def)));
            if (pos < 0) {
                deliver(sender, head.append(Component.newline())
                        .append(plugin.style().text("nicht platziert (kein Wert)")));
            } else {
                deliver(sender, head.append(Component.newline())
                        .append(plugin.style().text("Platz "))
                        .append(rankComponent(pos))
                        .append(plugin.style().text(" von " + results.size() + " — "))
                        .append(plugin.style().value(UnitConverter.format(def.category(), value, plugin.config()))));
            }
        });
    }

    private void handleAverage(CommandSender sender, String[] args) {
        if (args.length < 2) {
            infoText(sender, "Verwendung: /playerstats average <statistik>");
            return;
        }
        StatDefinition def = plugin.registry().byId(args[1]).orElse(null);
        if (def == null) {
            error(sender, "unknown-stat");
            return;
        }
        infoText(sender, plugin.language().message("calculating"));
        plugin.service().topList(def, results -> {
            TextComponent.Builder b = Component.text();
            b.append(plugin.style().title("Durchschnitt")).append(plugin.style().text(" — "))
                    .append(plugin.style().statName(plugin.language().statName(def)));
            if (results.isEmpty()) {
                b.append(Component.newline()).append(plugin.style().text("(keine Daten)"));
            } else {
                double sum = 0;
                for (StatResult r : results) {
                    sum += r.value();
                }
                double avg = sum / results.size();
                b.append(plugin.style().text(": "))
                        .append(plugin.style().value(UnitConverter.format(def.category(), avg, plugin.config())))
                        .append(plugin.style().text(" (ueber " + results.size() + " Spieler)"));
            }
            deliver(sender, b.build());
        });
    }

    private void handleSearch(CommandSender sender, String[] args) {
        if (args.length < 2) {
            infoText(sender, "Verwendung: /playerstats search <text>");
            return;
        }
        String query = args[1].toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        for (String id : plugin.registry().ids()) {
            if (id.contains(query)) {
                hits.add(id);
            }
        }
        if (hits.isEmpty()) {
            infoText(sender, "Keine Statistik enthaelt \"" + args[1] + "\".");
            return;
        }
        Component header = plugin.style().text(hits.size() + " Treffer fuer \"" + args[1] + "\":");
        sender.sendMessage(header.append(Component.newline())
                .append(plugin.style().value(String.join(", ", hits))));
    }

    private void handleVersion(CommandSender sender) {
        String version = plugin.getPluginMeta().getVersion();
        Component out = plugin.style().title("PlayerStats")
                .append(plugin.style().text(" v" + version + " — "));
        if (plugin.updater().isUpdateAvailable()) {
            out = out.append(plugin.style().value("Update verfuegbar: v" + plugin.updater().latestVersion()));
        } else {
            out = out.append(plugin.style().text("aktuell"));
        }
        sender.sendMessage(out);
    }

    private void handleHelp(CommandSender sender) {
        TextComponent.Builder b = Component.text();
        b.append(plugin.style().title("PlayerStats — Befehle"));
        helpLine(b, "/playerstats <stat> [me|top|server|player <name>]", "Wert anzeigen");
        helpLine(b, "/playerstats <stat> top [seite]", "Rangliste (paginiert)");
        helpLine(b, "/playerstats rank <stat> [spieler]", "Platz in der Rangliste");
        helpLine(b, "/playerstats average <stat>", "Server-Durchschnitt");
        helpLine(b, "/playerstats compare <stat> <sp1> [sp2]", "Vergleich zweier Spieler");
        helpLine(b, "/playerstats list", "alle Statistiken auflisten");
        helpLine(b, "/playerstats search <text>", "Statistiken suchen");
        helpLine(b, "/playerstats info <stat>", "Details zu einer Statistik");
        helpLine(b, "/playerstats share", "letztes Ergebnis im Chat teilen");
        helpLine(b, "/playerstats version", "Version & Update-Status");
        if (sender.hasPermission("playerstats.exclude")) {
            helpLine(b, "/playerstats exclude <add|remove|list> [spieler]", "Spieler ausschliessen");
        }
        if (sender.hasPermission("playerstats.reset")) {
            helpLine(b, "/playerstats reset <spieler> [stat|all]", "Custom-Stats zuruecksetzen");
        }
        if (sender.hasPermission("playerstats.reload")) {
            helpLine(b, "/playerstats reload", "Konfiguration neu laden");
        }
        sender.sendMessage(b.build());
    }

    private void helpLine(TextComponent.Builder b, String usage, String desc) {
        b.append(Component.newline())
                .append(Component.text(usage, NamedTextColor.AQUA))
                .append(plugin.style().text("  — " + desc));
    }

    // --- Verwaltung --------------------------------------------------------

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
        Component ids = plugin.style().value(String.join(", ", plugin.registry().ids()));
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

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            infoText(sender, "Verwendung: /playerstats info <statistik>");
            return;
        }
        StatDefinition def = plugin.registry().byId(args[1]).orElse(null);
        if (def == null) {
            error(sender, "unknown-stat");
            return;
        }
        TextComponent.Builder b = Component.text();
        b.append(plugin.style().title("Info")).append(plugin.style().text(" — "))
                .append(plugin.style().statName(plugin.language().statName(def)));
        infoLine(b, "id", def.id());
        infoLine(b, "Art", kindLabel(def.kind()));
        infoLine(b, "Kategorie", categoryLabel(def.category()));
        infoLine(b, "Name", def.translationKey() != null ? "clientseitig uebersetzt" : "aus language.yml");
        if (def.customKey() != null) {
            infoLine(b, "Speicher-Schluessel", def.customKey());
        }
        sender.sendMessage(b.build());
    }

    private void infoLine(TextComponent.Builder b, String label, String value) {
        b.append(Component.newline())
                .append(plugin.style().text(label + ": "))
                .append(plugin.style().value(value));
    }

    private String kindLabel(it.markus.playerstats.stat.StatKind kind) {
        return switch (kind) {
            case VANILLA -> "Vanilla (live abgefragt)";
            case VANILLA_GROUP -> "Gruppe (Summe mehrerer Vanilla-Stats)";
            case CUSTOM -> "Custom (gespeichert)";
            case COMPUTED -> "Berechnet (nie gespeichert)";
        };
    }

    private String categoryLabel(it.markus.playerstats.stat.StatCategory category) {
        return switch (category) {
            case GENERIC -> "Anzahl";
            case TIME -> "Zeit (" + plugin.config().timeUnit().name().toLowerCase(Locale.ROOT) + ")";
            case DISTANCE -> "Distanz (" + plugin.config().distanceUnit().name().toLowerCase(Locale.ROOT) + ")";
            case DAMAGE -> "Schaden (" + plugin.config().damageUnit().name().toLowerCase(Locale.ROOT) + ")";
            case DATE -> "Zeitpunkt";
            case RATIO -> "Verhaeltnis";
            case PERCENT -> "Prozent";
        };
    }

    // --- Darstellungs-Helfer ----------------------------------------------

    private Component statLine(StatDefinition def, double value) {
        String valueStr = UnitConverter.format(def.category(), value, plugin.config());
        return plugin.style().statName(plugin.language().statName(def))
                .append(plugin.style().text(": "))
                .append(plugin.style().value(valueStr));
    }

    private void deliver(CommandSender sender, Component content) {
        if (sender instanceof Player player && player.hasPermission("playerstats.share")) {
            shareCache.put(player.getUniqueId(), content);
            if (plugin.config().showShareButton()) {
                sender.sendMessage(content.append(Component.newline()).append(shareButton()));
                return;
            }
        }
        sender.sendMessage(content);
    }

    private Component shareButton() {
        return Component.text("[Teilen]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/playerstats share"))
                .hoverEvent(HoverEvent.showText(Component.text("Statistik im Chat teilen")));
    }

    private Component rankComponent(int rank) {
        return plugin.config().showMedals() ? plugin.style().medal(rank) : plugin.style().rank(rank);
    }

    private Component pagination(StatDefinition def, int current, int pages) {
        Component prev = current > 1
                ? clickable("[< zurueck]", "/playerstats " + def.id() + " top " + (current - 1))
                : plugin.style().text("[< zurueck]");
        Component next = current < pages
                ? clickable("[weiter >]", "/playerstats " + def.id() + " top " + (current + 1))
                : plugin.style().text("[weiter >]");
        return prev.append(plugin.style().text("  ")).append(next);
    }

    private Component clickable(String text, String command) {
        return Component.text(text, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(command)));
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

    private static String[] shift(String[] args) {
        return Arrays.copyOfRange(args, 1, args.length);
    }

    private int parsePage(String[] args, int index) {
        if (args.length > index) {
            try {
                return Integer.parseInt(args[index]);
            } catch (NumberFormatException ignored) {
                // ungueltige Seite -> 1
            }
        }
        return 1;
    }

    // --- Tab-Vervollstaendigung -------------------------------------------

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length >= 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            // an Sub-Handler weiterreichen (sie kennen ihr eigenes Arg-Layout)
            switch (sub) {
                case "compare" -> { return compareCmd.onTabComplete(sender, command, alias, shift(args)); }
                case "exclude" -> { return excludeCmd.onTabComplete(sender, command, alias, shift(args)); }
                case "reset" -> { return resetCmd.onTabComplete(sender, command, alias, shift(args)); }
                default -> { /* unten weiter */ }
            }
        }

        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> options = new ArrayList<>(plugin.registry().ids());
            options.add("list");
            options.add("info");
            options.add("help");
            options.add("version");
            options.add("search");
            options.add("rank");
            options.add("average");
            options.add("compare");
            options.add("share");
            if (sender.hasPermission("playerstats.exclude")) {
                options.add("exclude");
            }
            if (sender.hasPermission("playerstats.reset")) {
                options.add("reset");
            }
            if (sender.hasPermission("playerstats.reload")) {
                options.add("reload");
            }
            addMatching(out, options, args[0]);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("info") || sub.equals("rank") || sub.equals("average") || sub.equals("avg")) {
                addMatching(out, new ArrayList<>(plugin.registry().ids()), args[1]);
            } else if (plugin.registry().byId(args[0]).isPresent()) {
                addMatching(out, List.of("me", "top", "server", "player"), args[1]);
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("rank") && sender.hasPermission("playerstats.stats.others")) {
                addOnlineNames(out, args[2]);
            } else if (plugin.registry().byId(args[0]).isPresent() && args[1].equalsIgnoreCase("player")) {
                addOnlineNames(out, args[2]);
            }
        }
        return out;
    }

    private void addOnlineNames(List<String> out, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(p.getName());
            }
        }
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
