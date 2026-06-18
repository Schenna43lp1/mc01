package it.markus.playerstats.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * SQLite-Backend: lokale Datei plugins/PlayerStats/playerstats.db.
 *
 * Jede neue Verbindung aktiviert den WAL-Modus (Write-Ahead-Log) – bessere
 * Nebenlaeufigkeit (Leser blockieren den Schreiber nicht) und robusteres
 * Verhalten bei Absturz. Pool-Groesse 1, da SQLite nur einen Schreiber zulaesst.
 */
public final class SqliteStorage extends JdbcStorage {

    private final String url;

    public SqliteStorage(File dataFolder, String tablePrefix, Logger log) {
        super(tablePrefix, log, 1);
        this.url = "jdbc:sqlite:" + new File(dataFolder, "playerstats.db").getAbsolutePath();
    }

    @Override
    protected Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA synchronous=NORMAL");
            s.execute("PRAGMA busy_timeout=5000");
        }
        return c;
    }

    @Override
    protected String driverClass() {
        return "org.sqlite.JDBC";
    }

    @Override
    protected String upsertSql() {
        return "INSERT INTO " + table + " (uuid, stat_key, value) VALUES (?, ?, ?) "
                + "ON CONFLICT(uuid, stat_key) DO UPDATE SET value = excluded.value";
    }

    @Override
    public String name() {
        return "SQLite";
    }
}
