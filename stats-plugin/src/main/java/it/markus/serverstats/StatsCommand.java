package it.markus.serverstats;

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

/**
 * Behandelt /stats und /playtime (samt Tab-Vervollstaendigung).
 *
 *   /stats                 -> eigene Statistiken
 *   /stats <spieler>       -> Statistiken eines anderen   (serverstats.stats.others)
 *   /stats top             -> Rangliste nach Spielzeit     (serverstats.top)
 *   /stats reload          -> Config neu laden             (serverstats.admin)
 *   /stats reset <spieler> -> Statistiken zuruecksetzen    (serverstats.admin)
 *
 *   /playtime [spieler]    -> nur die Spielzeit
 */
public final class StatsCommand implements TabExecutor {

    private final ServerStats plugin;
    private final StatsStore store;

    public StatsCommand(ServerStats plugin, StatsStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("playtime")) {
            return handlePlaytime(sender, args);
        }

        if (args.length == 0) {
            return showStats(sender, sender);
        }

        return switch (args[0].toLowerCase()) {
            case "top" -> handleTop(sender);
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            default -> showStatsByName(sender, args[0]);
        };
    }

    // --- Subcommands -------------------------------------------------------

    private boolean handleTop(CommandSender sender) {
        if (!sender.hasPermission("serverstats.top")) {
            return deny(sender);
        }
        int size = plugin.getConfig().getInt("leaderboard.size", 10);
        List<StatsStore.LeaderboardEntry> top = store.topByPlaytime(size, System.currentTimeMillis());

        if (top.isEmpty()) {
            sender.sendMessage(Component.text("Noch keine Daten vorhanden.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("── Top " + top.size() + " nach Spielzeit ──", NamedTextColor.GOLD));
        int rank = 1;
        for (StatsStore.LeaderboardEntry e : top) {
            sender.sendMessage(Component.text("#" + rank + " ", NamedTextColor.GOLD)
                    .append(Component.text(e.name(), NamedTextColor.WHITE))
                    .append(Component.text(" — " + Format.duration(e.playtimeMs()), NamedTextColor.AQUA)));
            rank++;
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("serverstats.admin")) {
            return deny(sender);
        }
        plugin.reloadConfig();
        sender.sendMessage(Component.text("ServerStats-Config neu geladen.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("serverstats.admin")) {
            return deny(sender);
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Verwendung: /stats reset <spieler>", NamedTextColor.RED));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String name = target.getName() != null ? target.getName() : args[1];
        boolean existed = store.reset(target.getUniqueId(), name, System.currentTimeMillis());

        sender.sendMessage(existed
                ? Component.text("Statistiken von " + name + " zurueckgesetzt.", NamedTextColor.GREEN)
                : Component.text("Keine Daten fuer " + name + ".", NamedTextColor.YELLOW));
        return true;
    }

    // --- Anzeige -----------------------------------------------------------

    private boolean handlePlaytime(CommandSender sender, String[] args) {
        OfflinePlayer target = resolveTarget(sender, args.length == 0 ? null : args[0]);
        if (target == null) {
            return true; // Fehlermeldung wurde bereits gesendet
        }
        PlayerStats p = store.get(target.getUniqueId());
        if (p == null) {
            sender.sendMessage(Component.text("Keine Daten fuer " + target.getName() + ".", NamedTextColor.YELLOW));
            return true;
        }
        String playtime = Format.duration(store.getTotalPlaytimeMs(target.getUniqueId(), System.currentTimeMillis()));
        sender.sendMessage(Component.text("Spielzeit von " + p.getName() + ": ", NamedTextColor.GRAY)
                .append(Component.text(playtime, NamedTextColor.AQUA)));
        return true;
    }

    private boolean showStats(CommandSender sender, CommandSender targetSource) {
        if (!(targetSource instanceof Player player)) {
            sender.sendMessage(Component.text(
                    "Bitte einen Spielernamen angeben: /stats <spieler>", NamedTextColor.RED));
            return true;
        }
        return printStats(sender, player);
    }

    private boolean showStatsByName(CommandSender sender, String name) {
        OfflinePlayer target = resolveTarget(sender, name);
        if (target == null) {
            return true;
        }
        return printStats(sender, target);
    }

    private boolean printStats(CommandSender sender, OfflinePlayer target) {
        PlayerStats p = store.get(target.getUniqueId());
        if (p == null) {
            sender.sendMessage(Component.text(
                    "Keine Daten fuer " + target.getName() + ".", NamedTextColor.YELLOW));
            return true;
        }

        long now = System.currentTimeMillis();
        boolean online = target.isOnline();

        sender.sendMessage(Component.text("── Statistiken: " + p.getName() + " ──", NamedTextColor.GOLD));
        sender.sendMessage(line("Status", online ? "online" : "offline",
                online ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(line("Beigetreten", p.getJoinCount() + "x", NamedTextColor.AQUA));
        sender.sendMessage(line("Spielzeit",
                Format.duration(store.getTotalPlaytimeMs(target.getUniqueId(), now)), NamedTextColor.AQUA));
        sender.sendMessage(line("Tode", String.valueOf(p.getDeaths()), NamedTextColor.AQUA));
        sender.sendMessage(line("Mob-Kills", String.valueOf(p.getMobKills()), NamedTextColor.AQUA));
        sender.sendMessage(line("Abgebaute Bloecke", String.valueOf(p.getBlocksMined()), NamedTextColor.AQUA));
        sender.sendMessage(line("Strecke", Format.distance(p.getDistanceCm()), NamedTextColor.AQUA));
        sender.sendMessage(line("Erster Besuch", Format.date(p.getFirstJoin()), NamedTextColor.AQUA));
        sender.sendMessage(line("Zuletzt gesehen",
                online ? "jetzt" : Format.date(p.getLastSeen()), NamedTextColor.AQUA));
        return true;
    }

    // --- Helfer ------------------------------------------------------------

    /**
     * Ermittelt den Ziel-Spieler. Ohne Namen -> der Sender selbst. Mit Namen
     * ist die Permission serverstats.stats.others noetig. Gibt null zurueck und
     * verschickt eine Fehlermeldung, falls etwas nicht passt.
     */
    private OfflinePlayer resolveTarget(CommandSender sender, String name) {
        if (name == null) {
            if (sender instanceof Player player) {
                return player;
            }
            sender.sendMessage(Component.text("Bitte einen Spielernamen angeben.", NamedTextColor.RED));
            return null;
        }
        if (!sender.hasPermission("serverstats.stats.others")) {
            sender.sendMessage(Component.text(
                    "Du darfst nur deine eigenen Statistiken ansehen.", NamedTextColor.RED));
            return null;
        }
        return Bukkit.getOfflinePlayer(name);
    }

    private boolean deny(CommandSender sender) {
        sender.sendMessage(Component.text("Dazu fehlt dir die Berechtigung.", NamedTextColor.RED));
        return true;
    }

    private Component line(String label, String value, NamedTextColor valueColor) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, valueColor));
    }

    // --- Tab-Vervollstaendigung -------------------------------------------

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("playtime")) {
            if (args.length == 1 && sender.hasPermission("serverstats.stats.others")) {
                addOnlineNames(out, args[0]);
            }
            return out;
        }

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("serverstats.top")) {
                options.add("top");
            }
            if (sender.hasPermission("serverstats.admin")) {
                options.add("reload");
                options.add("reset");
            }
            String prefix = args[0].toLowerCase();
            for (String o : options) {
                if (o.startsWith(prefix)) {
                    out.add(o);
                }
            }
            if (sender.hasPermission("serverstats.stats.others")) {
                addOnlineNames(out, args[0]);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")
                && sender.hasPermission("serverstats.admin")) {
            addOnlineNames(out, args[1]);
        }
        return out;
    }

    private void addOnlineNames(List<String> out, String prefix) {
        String lower = prefix.toLowerCase();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase().startsWith(lower)) {
                out.add(online.getName());
            }
        }
    }
}
