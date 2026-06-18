package it.markus.playerstats;

import it.markus.playerstats.command.StatCommand;
import it.markus.playerstats.config.PluginConfig;
import it.markus.playerstats.custom.CustomKeys;
import it.markus.playerstats.custom.CustomStatService;
import it.markus.playerstats.discord.DiscordListener;
import it.markus.playerstats.discord.DiscordNotifier;
import it.markus.playerstats.exclude.ExcludeManager;
import it.markus.playerstats.filter.PlayerFilter;
import it.markus.playerstats.format.StyleFormatter;
import it.markus.playerstats.group.GroupRegistry;
import it.markus.playerstats.lang.LanguageManager;
import it.markus.playerstats.listener.CombatListener;
import it.markus.playerstats.listener.GatheringListener;
import it.markus.playerstats.listener.MiningListener;
import it.markus.playerstats.listener.SessionListener;
import it.markus.playerstats.stat.StatRegistry;
import it.markus.playerstats.stat.StatService;
import it.markus.playerstats.storage.StorageFactory;
import it.markus.playerstats.storage.StorageProvider;
import it.markus.playerstats.storage.YamlStorage;
import it.markus.playerstats.update.UpdateChecker;
import it.markus.playerstats.update.UpdateNotifyListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Einstiegspunkt von PlayerStats.
 *
 * Haelt die zentralen Bausteine und reicht sie ueber Getter an Commands,
 * Listener und Resolver weiter. {@link #reload()} baut die konfig-abhaengigen
 * Teile neu auf (Backend-Wechsel erfordert allerdings einen Neustart).
 */
public final class PlayerStatsPlugin extends JavaPlugin {

    private PluginConfig config;
    private StyleFormatter style;
    private LanguageManager language;
    private ExcludeManager excludes;
    private GroupRegistry groups;
    private StatRegistry registry;
    private CustomStatService custom;
    private PlayerFilter filter;
    private StatService service;
    private UpdateChecker updater;
    private DiscordNotifier discord;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new PluginConfig(getConfig(), getLogger());
        this.style = new StyleFormatter(config);
        this.language = new LanguageManager(this);
        this.excludes = new ExcludeManager(this);
        this.groups = new GroupRegistry(this::getConfig, getLogger());
        this.registry = new StatRegistry();
        this.custom = initCustom();
        this.filter = new PlayerFilter(this::config, excludes);
        this.service = new StatService(this, filter);

        bind("playerstats", new StatCommand(this));

        this.updater = new UpdateChecker(this);
        this.discord = new DiscordNotifier(this);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(new GatheringListener(this), this);
        getServer().getPluginManager().registerEvents(new SessionListener(this), this);
        getServer().getPluginManager().registerEvents(new UpdateNotifyListener(this), this);
        getServer().getPluginManager().registerEvents(new DiscordListener(this), this);

        scheduleFlush();
        scheduleElytraTracking();
        scheduleUpdateCheck();

        discord.serverOnline();
        getLogger().info("PlayerStats aktiviert (" + registry.ids().size() + " Statistiken).");
    }

    @Override
    public void onDisable() {
        if (discord != null) {
            discord.serverOffline(); // blockierend, da Scheduler nicht mehr nutzbar
        }
        if (custom != null) {
            custom.shutdown();
        }
    }

    /** Laedt config.yml, language.yml und die Gruppen zur Laufzeit neu. */
    public void reload() {
        reloadConfig();
        this.config = new PluginConfig(getConfig(), getLogger());
        this.style = new StyleFormatter(config);
        this.language.reload();
        this.excludes.load();
        this.groups.reload();
    }

    // --- Aufbau ------------------------------------------------------------

    private CustomStatService initCustom() {
        StorageProvider provider = StorageFactory.create(config, getDataFolder(), getLogger());
        CustomStatService svc = new CustomStatService(this, provider);
        try {
            svc.init();
            return svc;
        } catch (Exception ex) {
            getLogger().severe("Storage '" + provider.name() + "' fehlgeschlagen ("
                    + ex.getMessage() + ") – Fallback auf YAML.");
            CustomStatService fallback = new CustomStatService(this, new YamlStorage(getDataFolder()));
            try {
                fallback.init();
            } catch (Exception ignored) {
                // YAML kann praktisch nicht fehlschlagen.
            }
            return fallback;
        }
    }

    private void scheduleFlush() {
        long interval = config.flushIntervalSeconds() * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, custom::flush, interval, interval);
    }

    private void scheduleElytraTracking() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!config.trackElytraTime()) {
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isGliding()) {
                    custom.add(player.getUniqueId(), CustomKeys.ELYTRA_TICKS, 20);
                }
            }
        }, 20L, 20L);
    }

    private void scheduleUpdateCheck() {
        updater.check(); // einmal beim Start
        int hours = config.updaterCheckIntervalHours();
        if (hours > 0) {
            long ticks = hours * 3_600L * 20L;
            Bukkit.getScheduler().runTaskTimer(this, updater::check, ticks, ticks);
        }
    }

    /** Die JAR-Datei dieses Plugins (fuer den Auto-Download in den Update-Ordner). */
    public File pluginJarFile() {
        return getFile();
    }

    private void bind(String name, TabExecutor handler) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        } else {
            getLogger().warning("Command '" + name + "' fehlt in der plugin.yml!");
        }
    }

    // --- Zugriff -----------------------------------------------------------

    public PluginConfig config() {
        return config;
    }

    public StyleFormatter style() {
        return style;
    }

    public LanguageManager language() {
        return language;
    }

    public ExcludeManager excludes() {
        return excludes;
    }

    public GroupRegistry groups() {
        return groups;
    }

    public StatRegistry registry() {
        return registry;
    }

    public CustomStatService custom() {
        return custom;
    }

    public StatService service() {
        return service;
    }

    public UpdateChecker updater() {
        return updater;
    }

    public DiscordNotifier discord() {
        return discord;
    }
}
