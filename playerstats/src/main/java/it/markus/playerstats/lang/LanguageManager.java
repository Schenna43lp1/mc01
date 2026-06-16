package it.markus.playerstats.lang;

import it.markus.playerstats.stat.StatType;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Verwaltet language.yml: anpassbare Statistik-Namen und die UI-Texte.
 *
 * Statistik-Namen:
 *   - Eintrag leer  -> {@link Component#translatable} mit dem Minecraft-Key,
 *     d. h. der CLIENT uebersetzt den Namen in seine eigene Sprache.
 *   - Eintrag gesetzt -> fester Text fuer alle Clients.
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
    public Component statName(StatType type) {
        String override = lang.getString("stats." + type.id(), "");
        if (override == null || override.isBlank()) {
            return Component.translatable(type.translationKey());
        }
        return Component.text(override);
    }

    /** Ein UI-Text aus dem messages-Abschnitt. */
    public String message(String key) {
        return lang.getString("messages." + key, key);
    }

    /** Ein UI-Text mit Platzhaltern (paarweise: "%player%", name, ...). */
    public String message(String key, String... placeholderPairs) {
        String text = message(key);
        for (int i = 0; i + 1 < placeholderPairs.length; i += 2) {
            text = text.replace(placeholderPairs[i], placeholderPairs[i + 1]);
        }
        return text;
    }
}
