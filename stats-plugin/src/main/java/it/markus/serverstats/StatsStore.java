package it.markus.serverstats;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet die Spielerstatistiken: laden, speichern und aktualisieren.
 *
 * Gespeichert wird in plugins/ServerStats/playerdata.yml. Im Speicher liegen
 * die Werte in einer Map (UUID -> {@link PlayerStats}), damit Zugriffe schnell
 * sind. Die laufende Sitzung (seit wann ein Spieler gerade online ist) wird
 * getrennt in {@code sessionStart} gehalten und beim Verlassen verrechnet.
 */
public final class StatsStore {

    /** Ein Eintrag der Rangliste. */
    public record LeaderboardEntry(String name, long playtimeMs) {
    }

    private final File file;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private final Map<UUID, Long> sessionStart = new HashMap<>();

    public StatsStore(File dataFolder) {
        this.file = new File(dataFolder, "playerdata.yml");
    }

    /** Liest alle gespeicherten Spieler aus der Datei in den Speicher. */
    public void load() {
        stats.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            ConfigurationSection s = players.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                stats.put(uuid, new PlayerStats(
                        s.getString("name", key),
                        s.getLong("first-join"),
                        s.getLong("last-seen"),
                        s.getInt("join-count"),
                        s.getLong("playtime-ms"),
                        s.getLong("deaths"),
                        s.getLong("mob-kills"),
                        s.getLong("blocks-mined"),
                        s.getLong("distance-cm"),
                        s.getInt("milestones-reached")
                ));
            } catch (IllegalArgumentException ignored) {
                // Ungueltiger UUID-Schluessel -> Eintrag ueberspringen.
            }
        }
    }

    /** Schreibt den aktuellen Stand zurueck auf die Festplatte. */
    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> e : stats.entrySet()) {
            String base = "players." + e.getKey();
            PlayerStats p = e.getValue();
            yaml.set(base + ".name", p.getName());
            yaml.set(base + ".first-join", p.getFirstJoin());
            yaml.set(base + ".last-seen", p.getLastSeen());
            yaml.set(base + ".join-count", p.getJoinCount());
            yaml.set(base + ".playtime-ms", p.getPlaytimeMs());
            yaml.set(base + ".deaths", p.getDeaths());
            yaml.set(base + ".mob-kills", p.getMobKills());
            yaml.set(base + ".blocks-mined", p.getBlocksMined());
            yaml.set(base + ".distance-cm", p.getDistanceCm());
            yaml.set(base + ".milestones-reached", p.getMilestonesReached());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            throw new RuntimeException("Konnte playerdata.yml nicht speichern", ex);
        }
    }

    /**
     * Beim Beitritt: Eintrag anlegen/aktualisieren und Sitzungsbeginn merken.
     *
     * @return true, wenn der Spieler zum ersten Mal ueberhaupt beitritt.
     */
    public boolean handleJoin(Player player, long now) {
        UUID uuid = player.getUniqueId();
        PlayerStats p = stats.get(uuid);
        boolean firstEver = (p == null);
        if (firstEver) {
            p = PlayerStats.fresh(player.getName(), now);
            stats.put(uuid, p);
        }
        p.setName(player.getName());
        p.setLastSeen(now);
        p.incrementJoinCount();
        sessionStart.put(uuid, now);
        return firstEver;
    }

    /** Beim Verlassen: laufende Sitzung zur Gesamtspielzeit addieren. */
    public void handleQuit(Player player, long now) {
        UUID uuid = player.getUniqueId();
        PlayerStats p = stats.get(uuid);
        if (p == null) {
            return;
        }
        Long start = sessionStart.remove(uuid);
        if (start != null) {
            p.addPlaytimeMs(now - start);
        }
        p.setLastSeen(now);
    }

    /**
     * Verrechnet alle noch offenen Sitzungen (z. B. beim Server-Stopp), damit
     * keine Spielzeit verloren geht. Setzt die Sitzungen anschliessend neu auf
     * 'jetzt', falls der Server doch weiterlaeuft.
     */
    public void flushOpenSessions(long now) {
        for (Map.Entry<UUID, Long> e : sessionStart.entrySet()) {
            PlayerStats p = stats.get(e.getKey());
            if (p != null) {
                p.addPlaytimeMs(now - e.getValue());
                p.setLastSeen(now);
            }
            e.setValue(now);
        }
    }

    public PlayerStats get(UUID uuid) {
        return stats.get(uuid);
    }

    /** Gesamtspielzeit inklusive der gerade laufenden Sitzung (falls online). */
    public long getTotalPlaytimeMs(UUID uuid, long now) {
        PlayerStats p = stats.get(uuid);
        if (p == null) {
            return 0L;
        }
        long total = p.getPlaytimeMs();
        Long start = sessionStart.get(uuid);
        if (start != null) {
            total += now - start;
        }
        return total;
    }

    // --- Gameplay-Zaehler (no-op, falls der Spieler nicht erfasst ist) ---

    public void recordDeath(UUID uuid) {
        PlayerStats p = stats.get(uuid);
        if (p != null) {
            p.incrementDeaths();
        }
    }

    public void recordMobKill(UUID uuid) {
        PlayerStats p = stats.get(uuid);
        if (p != null) {
            p.incrementMobKills();
        }
    }

    public void recordBlockMined(UUID uuid) {
        PlayerStats p = stats.get(uuid);
        if (p != null) {
            p.incrementBlocksMined();
        }
    }

    public void recordDistanceCm(UUID uuid, long cm) {
        PlayerStats p = stats.get(uuid);
        if (p != null) {
            p.addDistanceCm(cm);
        }
    }

    /**
     * Setzt die Statistiken eines Spielers zurueck. Ist der Spieler gerade
     * online, wird ein frischer Eintrag fuer die laufende Sitzung angelegt.
     *
     * @return true, wenn vorher Daten vorhanden waren.
     */
    public boolean reset(UUID uuid, String name, long now) {
        boolean existed = stats.remove(uuid) != null;
        if (sessionStart.containsKey(uuid)) {
            PlayerStats fresh = PlayerStats.fresh(name, now);
            fresh.incrementJoinCount();
            stats.put(uuid, fresh);
            sessionStart.put(uuid, now);
        }
        save();
        return existed;
    }

    /** Die Top-Spieler nach Gesamtspielzeit (absteigend). */
    public List<LeaderboardEntry> topByPlaytime(int limit, long now) {
        return stats.entrySet().stream()
                .map(e -> new LeaderboardEntry(
                        e.getValue().getName(),
                        getTotalPlaytimeMs(e.getKey(), now)))
                .sorted(Comparator.comparingLong(LeaderboardEntry::playtimeMs).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }
}
