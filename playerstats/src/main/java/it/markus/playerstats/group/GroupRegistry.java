package it.markus.playerstats.group;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Liest die konfigurierbaren Material-/Statistik-Gruppen aus der config.yml.
 *
 * So lassen sich Ore-/Log-/Crop-Listen oder die Distanz-Statistiken anpassen,
 * ohne Code zu aendern (auch fuer neue Minecraft-Versionen). reload() liest neu.
 */
public final class GroupRegistry {

    private final Supplier<FileConfiguration> configSupplier;
    private final Logger log;

    private final Map<String, List<Material>> blockGroups = new LinkedHashMap<>();
    private final List<Statistic> distanceStats = new ArrayList<>();
    private final Set<Material> crops = EnumSet.noneOf(Material.class);
    private final Set<Material> rareBlocks = EnumSet.noneOf(Material.class);
    private List<Material> allBlocks = List.of();

    public GroupRegistry(Supplier<FileConfiguration> configSupplier, Logger log) {
        this.configSupplier = configSupplier;
        this.log = log;
        reload();
    }

    public void reload() {
        FileConfiguration c = configSupplier.get();
        blockGroups.clear();
        distanceStats.clear();
        crops.clear();
        rareBlocks.clear();

        // Block-Gruppen unter groups.blocks.<name>: [MATERIAL, ...]
        ConfigurationSection blocks = c.getConfigurationSection("groups.blocks");
        if (blocks != null) {
            for (String key : blocks.getKeys(false)) {
                blockGroups.put(key.toLowerCase(Locale.ROOT), materials(blocks.getStringList(key)));
            }
        }

        for (String name : c.getStringList("groups.distance")) {
            try {
                distanceStats.add(Statistic.valueOf(name.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                warn("Unbekannte Statistik in groups.distance: " + name);
            }
        }

        crops.addAll(materials(c.getStringList("groups.crops")));
        rareBlocks.addAll(materials(c.getStringList("custom.rare-blocks")));

        // Alle Block-Materialien einmal vorberechnen (fuer "total_blocks").
        List<Material> all = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.isBlock()) {
                all.add(m);
            }
        }
        this.allBlocks = List.copyOf(all);
    }

    public List<Material> blockGroup(String name) {
        return blockGroups.getOrDefault(name.toLowerCase(Locale.ROOT), List.of());
    }

    public List<Statistic> distanceStats() {
        return distanceStats;
    }

    public List<Material> allBlocks() {
        return allBlocks;
    }

    public Set<Material> crops() {
        return crops;
    }

    public Set<Material> rareBlocks() {
        return rareBlocks;
    }

    private List<Material> materials(List<String> names) {
        List<Material> out = new ArrayList<>();
        for (String name : names) {
            Material m = Material.matchMaterial(name.trim());
            if (m != null) {
                out.add(m);
            } else {
                warn("Unbekanntes Material: " + name);
            }
        }
        return out;
    }

    private void warn(String msg) {
        if (log != null) {
            log.warning("[Gruppen] " + msg);
        }
    }
}
