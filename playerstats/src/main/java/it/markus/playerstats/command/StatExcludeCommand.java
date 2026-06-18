package it.markus.playerstats.command;

import it.markus.playerstats.PlayerStatsPlugin;
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
import java.util.Map;
import java.util.UUID;

/**
 * /statexclude – nimmt Spieler manuell aus Top-/Server-Statistiken heraus
 * (oder wieder hinein). Persistiert ueber den {@code ExcludeManager}.
 */
public final class StatExcludeCommand implements TabExecutor {

    private final PlayerStatsPlugin plugin;

    public StatExcludeCommand(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("playerstats.exclude")) {
            error(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            infoText(sender, "Verwendung: /playerstats exclude <add|remove|list> [spieler]");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            default -> infoText(sender, "Verwendung: /playerstats exclude <add|remove|list> [spieler]");
        }
        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            infoText(sender, "Verwendung: /playerstats exclude add <spieler>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            error(sender, "player-not-found");
            return;
        }
        String name = target.getName() != null ? target.getName() : args[1];
        if (plugin.excludes().add(target.getUniqueId(), name)) {
            success(sender, "excluded-added", "%player%", name);
        } else {
            infoText(sender, name + " ist bereits ausgeschlossen.");
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            infoText(sender, "Verwendung: /playerstats exclude remove <spieler>");
            return;
        }
        UUID uuid = resolveUuid(args[1]);
        if (uuid == null) {
            error(sender, "player-not-found");
            return;
        }
        if (plugin.excludes().remove(uuid)) {
            success(sender, "excluded-removed", "%player%", args[1]);
        } else {
            infoText(sender, args[1] + " war nicht ausgeschlossen.");
        }
    }

    private void handleList(CommandSender sender) {
        Map<UUID, String> entries = plugin.excludes().entries();
        if (entries.isEmpty()) {
            infoText(sender, plugin.language().message("excluded-empty"));
            return;
        }
        Component header = plugin.style().text(plugin.language().message("excluded-header"));
        Component names = plugin.style().value(String.join(", ", entries.values()));
        sender.sendMessage(header.append(Component.newline()).append(names));
    }

    /** UUID aus Cache ODER (fuer entfernte/uncachte Spieler) aus der Ausschlussliste. */
    private UUID resolveUuid(String name) {
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) {
            return cached.getUniqueId();
        }
        for (Map.Entry<UUID, String> e : plugin.excludes().entries().entrySet()) {
            if (e.getValue().equalsIgnoreCase(name)) {
                return e.getKey();
            }
        }
        return null;
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

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String o : List.of("add", "remove", "list")) {
                if (o.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(o);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (String name : plugin.excludes().entries().values()) {
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(name);
                }
            }
        }
        return out;
    }
}
