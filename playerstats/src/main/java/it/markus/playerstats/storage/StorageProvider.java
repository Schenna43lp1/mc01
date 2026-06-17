package it.markus.playerstats.storage;

import java.util.Map;
import java.util.UUID;

/**
 * Backend-Abstraktion fuer die CUSTOM-Statistiken.
 *
 * Datenmodell bewusst einfach: pro Spieler eine Map {@code stat-key -> Wert}.
 * Damit bleiben alle Backends (YAML/SQLite/MySQL) gleich strukturiert.
 */
public interface StorageProvider {

    /** Datei/Tabellen/Verbindung vorbereiten. */
    void init() throws Exception;

    /** Alle gespeicherten Daten laden (wird beim Start in den Cache gezogen). */
    Map<UUID, Map<String, Long>> loadAll() throws Exception;

    /** Eine Teilmenge (geaenderte Spieler) persistieren. */
    void saveBatch(Map<UUID, Map<String, Long>> dirty) throws Exception;

    /**
     * Loescht Daten eines Spielers. Bei {@code key == null} alle CUSTOM-Stats
     * des Spielers, sonst nur den einen Schluessel.
     */
    void delete(UUID uuid, String key) throws Exception;

    /** Verbindungen/Dateien schliessen. */
    void close();

    /** Anzeigename fuers Log. */
    String name();
}
