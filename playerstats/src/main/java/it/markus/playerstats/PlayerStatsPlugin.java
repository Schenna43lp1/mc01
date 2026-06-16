package it.markus.playerstats;

import it.markus.playerstats.command.StatCommand;
import it.markus.playerstats.command.StatExcludeCommand;
import it.markus.playerstats.config.PluginConfig;
import it.markus.playerstats.exclude.ExcludeManager;
import it.markus.playerstats.filter.PlayerFilter;
import it.markus.playerstats.format.StyleFormatter;
import it.markus.playerstats.lang.LanguageManager;
import it.markus.playerstats.stat.StatService;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Einstiegspunkt von PlayerStats.
 *
 * Haelt die zentralen Bausteine und reicht sie ueber Getter an die Commands
 * weiter. {@link #reload()} baut die konfig-abhaengigen Teile neu auf – ganz
 * ohne Server-Neustart.
 */
public final class PlayerStatsPlugin extends JavaPlugin {

    private PluginConfig config;
    private StyleFormatter style;
    private LanguageManager language;
    private ExcludeManager excludes;
    private PlayerFilter filter;
    private StatService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new PluginConfig(getConfig(), getLogger());
        this.style = new StyleFormatter(config);
        this.language = new LanguageManager(this);
        this.excludes = new ExcludeManager(this);
        this.filter = new PlayerFilter(this::config, excludes);
        this.service = new StatService(this, filter);

        bind("playerstats", new StatCommand(this));
        bind("statexclude", new StatExcludeCommand(this));

        getLogger().info("PlayerStats aktiviert (" + it.markus.playerstats.stat.StatType.ids().size()
                + " Statistiken).");
    }

    /** Laedt config.yml und language.yml zur Laufzeit neu. */
    public void reload() {
        reloadConfig();
        this.config = new PluginConfig(getConfig(), getLogger());
        this.style = new StyleFormatter(config);
        this.language.reload();
        this.excludes.load();
    }

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

    public StatService service() {
        return service;
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
}
