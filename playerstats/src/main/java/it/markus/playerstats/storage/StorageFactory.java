package it.markus.playerstats.storage;

import it.markus.playerstats.config.PluginConfig;

import java.io.File;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Erzeugt das in der Config gewaehlte Storage-Backend.
 * Faellt bei unbekanntem Typ sicher auf YAML zurueck.
 */
public final class StorageFactory {

    private StorageFactory() {
    }

    public static StorageProvider create(PluginConfig config, File dataFolder, Logger log) {
        String type = config.storageType().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "sqlite" -> new SqliteStorage(dataFolder, config.tablePrefix(), log);
            case "mysql" -> new MySqlStorage(
                    config.mysqlHost(), config.mysqlPort(), config.mysqlDatabase(),
                    config.mysqlUser(), config.mysqlPassword(), config.tablePrefix(), log);
            case "yaml" -> new YamlStorage(dataFolder);
            default -> {
                log.warning("Unbekannter storage.type '" + type + "' – nutze YAML.");
                yield new YamlStorage(dataFolder);
            }
        };
    }
}
