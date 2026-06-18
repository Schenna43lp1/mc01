package it.markus.firstplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Einstiegspunkt des Plugins.
 *
 * JavaPlugin ist die Basisklasse jedes Bukkit/Paper-Plugins. Der Server
 * instanziiert sie automatisch und ruft onEnable() beim Start und
 * onDisable() beim Stoppen auf.
 *
 * Listener wird hier gleich mit-implementiert, damit das Plugin für den
 * Einstieg in einer Datei bleibt. Später lagerst du Listener und Commands
 * in eigene Klassen aus.
 */
public final class FirstPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Dieses Plugin als Event-Listener beim Server registrieren.
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("FirstPlugin ist aktiv. Servus, Markus!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FirstPlugin wird heruntergefahren.");
    }

    /**
     * Wird aufgerufen, sobald ein Spieler dem Server beitritt.
     * Die @EventHandler-Annotation macht aus der Methode einen Listener.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(
                Component.text("Willkommen auf dem Server!", NamedTextColor.GREEN)
        );
    }

    /**
     * Behandelt den /ping-Command. Registriert wird er in der plugin.yml.
     *
     * @return true, wenn der Command korrekt verarbeitet wurde;
     *         false zeigt dem Spieler automatisch die 'usage' aus der plugin.yml.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("ping")) {
            sender.sendMessage(
                    Component.text("Pong!", NamedTextColor.AQUA)
            );
            return true;
        }
        return false;
    }
}
