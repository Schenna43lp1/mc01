package it.markus.playerstats.lang;

import it.markus.playerstats.stat.StatDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Verwaltet language.yml: anpassbare Statistik-Namen und die UI-Texte.
 *
 * Statistik-Namen:
 *   - Eintrag gesetzt -> fester Text fuer alle Clients.
 *   - sonst, falls ein Translation-Key existiert -> {@link Component#translatable},
 *     d. h. der CLIENT uebersetzt den Namen in seine eigene Sprache.
 *   - sonst -> die id als Fallback.
 */
public final class LanguageManager {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration lang;

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "language.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("language.yml", false);
        }
        this.lang = YamlConfiguration.loadConfiguration(file);
    }

    /** Der (unkolorierte) Anzeigename einer Statistik. */
    public Component statName(StatDefinition def) {
        String override = lang.getString("stats." + def.id(), "");
        if (override != null && !override.isBlank()) {
            return Component.text(override);
        }
        if (def.translationKey() != null) {
            return Component.translatable(def.translationKey());
        }
        return Component.text(def.id());
    }

    public String message(String key) {
        return lang.getString("messages." + key, key);
    }

    public String message(String key, String... placeholderPairs) {
        String text = message(key);
        for (int i = 0; i + 1 < placeholderPairs.length; i += 2) {
            text = text.replace(placeholderPairs[i], placeholderPairs[i + 1]);
        }
        return text;
    }
}
