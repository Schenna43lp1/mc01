package it.markus.playerstats.exclude;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistente Liste manuell ausgeschlossener Spieler (excluded.yml).
 * Gepflegt ueber /statexclude. UUID -> zuletzt bekannter Name.
 */
public final class ExcludeManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, String> excluded = new LinkedHashMap<>();

    public ExcludeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "excluded.yml");
        load();
    }

    public void load() {
        excluded.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("excluded");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                excluded.put(UUID.fromString(key), section.getString(key, key));
            } catch (IllegalArgumentException ignored) {
                // ungueltige UUID -> ueberspringen
            }
        }
    }

    public boolean isExcluded(UUID uuid) {
        return excluded.containsKey(uuid);
    }

    public boolean add(UUID uuid, String name) {
        if (excluded.containsKey(uuid)) {
            return false;
        }
        excluded.put(uuid, name);
        save();
        return true;
    }

    public boolean remove(UUID uuid) {
        if (excluded.remove(uuid) == null) {
            return false;
        }
        save();
        return true;
    }

    /** Unveraenderliche Sicht auf UUID -> Name. */
    public Map<UUID, String> entries() {
        return Map.copyOf(excluded);
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : excluded.entrySet()) {
            yaml.set("excluded." + e.getKey(), e.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Konnte excluded.yml nicht speichern: " + ex.getMessage());
        }
    }
}
