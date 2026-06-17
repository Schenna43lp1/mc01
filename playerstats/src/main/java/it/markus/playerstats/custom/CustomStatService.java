package it.markus.playerstats.custom;

import it.markus.playerstats.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Haelt die CUSTOM-Statistiken im Speicher und schreibt sie gebatcht in den
 * {@link StorageProvider}.
 *
 * Ablauf: Event-Handler aendern nur den (threadsicheren) In-Memory-Cache und
 * markieren den Spieler als "dirty". Ein periodischer Async-Task ({@link #flush()})
 * persistiert die geaenderten Spieler gesammelt – nie synchron im Event-Handler.
 */
public final class CustomStatService {

    private final JavaPlugin plugin;
    private final StorageProvider storage;

    private final Map<UUID, Map<String, Long>> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    public CustomStatService(JavaPlugin plugin, StorageProvider storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    /** Storage initialisieren und vorhandene Daten in den Cache laden. */
    public void init() throws Exception {
        storage.init();
        for (Map.Entry<UUID, Map<String, Long>> e : storage.loadAll().entrySet()) {
            cache.put(e.getKey(), new ConcurrentHashMap<>(e.getValue()));
        }
        plugin.getLogger().info("Storage-Backend: " + storage.name()
                + " (" + cache.size() + " Spieler geladen)");
    }

    public long get(UUID uuid, String key) {
        Map<String, Long> map = cache.get(uuid);
        return map == null ? 0L : map.getOrDefault(key, 0L);
    }

    public void add(UUID uuid, String key, long delta) {
        addAndGet(uuid, key, delta);
    }

    /** Erhoeht einen Wert und liefert das Ergebnis (fuer Streak-Logik). */
    public long addAndGet(UUID uuid, String key, long delta) {
        long result = playerMap(uuid).merge(key, delta, Long::sum);
        dirty.add(uuid);
        return result;
    }

    public void set(UUID uuid, String key, long value) {
        playerMap(uuid).put(key, value);
        dirty.add(uuid);
    }

    /** Setzt nur, wenn der neue Wert groesser ist (Highscore/Streak-Rekord). */
    public void max(UUID uuid, String key, long value) {
        playerMap(uuid).merge(key, value, Math::max);
        dirty.add(uuid);
    }

    /** Persistiert alle als geaendert markierten Spieler. Blockierend -> async aufrufen. */
    public void flush() {
        if (dirty.isEmpty()) {
            return;
        }
        Set<UUID> snapshot = Set.copyOf(dirty);
        Map<UUID, Map<String, Long>> batch = new HashMap<>();
        for (UUID uuid : snapshot) {
            Map<String, Long> map = cache.get(uuid);
            if (map != null) {
                batch.put(uuid, new HashMap<>(map));
            }
        }
        try {
            storage.saveBatch(batch);
            dirty.removeAll(snapshot);
        } catch (Exception ex) {
            // Dirty-Markierung bleibt bestehen -> naechster Flush versucht es erneut.
            plugin.getLogger().warning("Konnte CUSTOM-Statistiken nicht speichern: " + ex.getMessage());
        }
    }

    /**
     * Setzt CUSTOM-Statistiken eines Spielers zurueck. {@code key == null}
     * loescht alle, sonst nur den einen Schluessel. Cache sofort, Storage async.
     */
    public void reset(UUID uuid, String key) {
        if (key == null) {
            cache.remove(uuid);
            dirty.remove(uuid);
        } else {
            Map<String, Long> map = cache.get(uuid);
            if (map != null) {
                map.remove(key);
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                storage.delete(uuid, key);
            } catch (Exception ex) {
                plugin.getLogger().warning("Reset im Storage fehlgeschlagen: " + ex.getMessage());
            }
        });
    }

    /** Beim Server-Stopp: letzter Flush und Storage schliessen. */
    public void shutdown() {
        flush();
        storage.close();
    }

    private Map<String, Long> playerMap(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
    }
}
