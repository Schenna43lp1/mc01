package it.markus.serverstats;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Einstiegspunkt des Plugins ServerStats.
 *
 * Aufgabe der Main-Klasse ist bewusst klein gehalten: Sie verdrahtet die
 * Bausteine miteinander und kuemmert sich um den Lebenszyklus.
 *
 *   - {@link StatsStore}            haelt und speichert die Daten
 *   - {@link PlayerEventListener}   reagiert auf Join/Quit (inkl. Erst-Beitritt)
 *   - {@link StatsTrackingListener} zaehlt Tode/Kills/Bloecke/Distanz
 *   - {@link MilestoneService}      kuendigt Spielzeit-Meilensteine an
 *   - {@link StatsCommand}          bedient /stats und /playtime
 */
public final class ServerStats extends JavaPlugin {

    private StatsStore store;

    @Override
    public void onEnable() {
        // Legt config.yml an, falls noch nicht vorhanden (aus den resources).
        saveDefaultConfig();

        // Daten laden.
        store = new StatsStore(getDataFolder());
        store.load();

        // Listener registrieren.
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this, store), this);
        getServer().getPluginManager().registerEvents(new StatsTrackingListener(store), this);

        // Commands verdrahten (muessen in der plugin.yml deklariert sein).
        StatsCommand statsCommand = new StatsCommand(this, store);
        bind("stats", statsCommand);
        bind("playtime", statsCommand);

        // Regelmaessig speichern, damit bei einem Absturz wenig verloren geht
        // (alle 5 Minuten = 6000 Ticks).
        getServer().getScheduler().runTaskTimer(this, () -> {
            store.flushOpenSessions(System.currentTimeMillis());
            store.save();
        }, 6_000L, 6_000L);

        // Meilensteine jede Minute pruefen (1200 Ticks).
        MilestoneService milestones = new MilestoneService(this, store);
        getServer().getScheduler().runTaskTimer(this, milestones::checkAll, 1_200L, 1_200L);

        getLogger().info("ServerStats ist aktiv. Servus, Markus!");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            // Offene Sitzungen verrechnen und ein letztes Mal speichern.
            store.flushOpenSessions(System.currentTimeMillis());
            store.save();
        }
        getLogger().info("ServerStats wird heruntergefahren.");
    }

    /** Registriert Executor + TabCompleter fuer einen Command aus der plugin.yml. */
    private void bind(String name, StatsCommand handler) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        } else {
            getLogger().warning("Command '" + name + "' fehlt in der plugin.yml!");
        }
    }
}
