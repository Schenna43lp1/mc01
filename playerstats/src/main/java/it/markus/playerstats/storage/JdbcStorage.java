package it.markus.playerstats.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Gemeinsame JDBC-Basis fuer SQLite und MySQL.
 *
 * Tabelle: &lt;prefix&gt;custom_stats(uuid, stat_key, value), PK(uuid, stat_key).
 * Es wird pro Operation eine Verbindung geoeffnet/geschlossen – fuer die
 * periodischen Batch-Schreibvorgaenge voellig ausreichend (kein Pool noetig).
 */
public abstract class JdbcStorage implements StorageProvider {

    protected final Logger log;
    protected final String table;

    protected JdbcStorage(String tablePrefix, Logger log) {
        this.log = log;
        this.table = tablePrefix + "custom_stats";
    }

    /** Oeffnet eine frische Verbindung zum jeweiligen Backend. */
    protected abstract Connection connect() throws SQLException;

    /** Vollqualifizierter Treiber-Klassenname (fuer Class.forName). */
    protected abstract String driverClass();

    /** Backend-spezifisches Upsert-Statement. */
    protected abstract String upsertSql();

    /** Backend-spezifischer Spaltentyp fuer die Werte. */
    protected String createTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "uuid VARCHAR(36) NOT NULL, "
                + "stat_key VARCHAR(64) NOT NULL, "
                + "value BIGINT NOT NULL, "
                + "PRIMARY KEY (uuid, stat_key))";
    }

    @Override
    public void init() throws Exception {
        try {
            Class.forName(driverClass());
        } catch (ClassNotFoundException ignored) {
            // Treiber registriert sich i. d. R. selbst via SPI.
        }
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute(createTableSql());
        }
    }

    @Override
    public Map<UUID, Map<String, Long>> loadAll() throws Exception {
        Map<UUID, Map<String, Long>> result = new LinkedHashMap<>();
        String sql = "SELECT uuid, stat_key, value FROM " + table;
        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    result.computeIfAbsent(uuid, k -> new LinkedHashMap<>())
                            .put(rs.getString("stat_key"), rs.getLong("value"));
                } catch (IllegalArgumentException ignored) {
                    // ungueltige UUID -> ueberspringen
                }
            }
        }
        return result;
    }

    @Override
    public void saveBatch(Map<UUID, Map<String, Long>> dirty) throws Exception {
        if (dirty.isEmpty()) {
            return;
        }
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(upsertSql())) {
            c.setAutoCommit(false);
            for (Map.Entry<UUID, Map<String, Long>> e : dirty.entrySet()) {
                String uuid = e.getKey().toString();
                for (Map.Entry<String, Long> stat : e.getValue().entrySet()) {
                    ps.setString(1, uuid);
                    ps.setString(2, stat.getKey());
                    ps.setLong(3, stat.getValue());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            c.commit();
        }
    }

    @Override
    public void delete(UUID uuid, String key) throws Exception {
        String sql = key == null
                ? "DELETE FROM " + table + " WHERE uuid = ?"
                : "DELETE FROM " + table + " WHERE uuid = ? AND stat_key = ?";
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            if (key != null) {
                ps.setString(2, key);
            }
            ps.executeUpdate();
        }
    }

    @Override
    public void close() {
    }
}
