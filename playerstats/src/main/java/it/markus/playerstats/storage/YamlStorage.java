package it.markus.playerstats.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Flat-YAML-Backend: plugins/PlayerStats/custom-stats.yml.
 * Struktur: players.&lt;uuid&gt;.&lt;stat-key&gt; = wert
 */
public final class YamlStorage implements StorageProvider {

    private final File file;

    public YamlStorage(File dataFolder) {
        this.file = new File(dataFolder, "custom-stats.yml");
    }

    @Override
    public void init() {
        // Datei entsteht beim ersten Speichern – nichts zu tun.
    }

    @Override
    public Map<UUID, Map<String, Long>> loadAll() {
        Map<UUID, Map<String, Long>> result = new HashMap<>();
        if (!file.exists()) {
            return result;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return result;
        }
        for (String key : players.getKeys(false)) {
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                Map<String, Long> values = new LinkedHashMap<>();
                for (String stat : section.getKeys(false)) {
                    values.put(stat, section.getLong(stat));
                }
                result.put(uuid, values);
            } catch (IllegalArgumentException ignored) {
                // ungueltige UUID -> ueberspringen
            }
        }
        return result;
    }

    @Override
    public void saveBatch(Map<UUID, Map<String, Long>> dirty) throws Exception {
        // Bestehende Datei laden und nur die geaenderten Spieler ueberschreiben,
        // damit nicht-betroffene Eintraege erhalten bleiben.
        YamlConfiguration yaml = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, Long>> e : dirty.entrySet()) {
            String base = "players." + e.getKey();
            yaml.set(base, null); // alten Block des Spielers entfernen
            for (Map.Entry<String, Long> stat : e.getValue().entrySet()) {
                yaml.set(base + "." + stat.getKey(), stat.getValue());
            }
        }
        yaml.save(file);
    }

    @Override
    public void delete(UUID uuid, String key) throws Exception {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (key == null) {
            yaml.set("players." + uuid, null);
        } else {
            yaml.set("players." + uuid + "." + key, null);
        }
        yaml.save(file);
    }

    @Override
    public void close() {
    }

    @Override
    public String name() {
        return "YAML";
    }
}
