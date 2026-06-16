package it.markus.playerstats.stat;

import org.bukkit.Statistic;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry der unterstuetzten Statistiken.
 *
 * Bewusst eine kuratierte Auswahl aussagekraeftiger UNTYPED-Statistiken (also
 * ohne Block-/Item-/Entity-Unterkategorien). Jede traegt:
 *   - eine stabile id (fuer config/command/language.yml)
 *   - die zugehoerige Bukkit-{@link Statistic}
 *   - den Minecraft-Translation-Key (fuer clientseitige Uebersetzung)
 *   - die {@link StatCategory} (fuer die Einheiten-Umrechnung)
 */
public enum StatType {

    PLAYTIME("playtime", Statistic.PLAY_ONE_MINUTE, "stat.minecraft.play_time", StatCategory.TIME),
    DEATHS("deaths", Statistic.DEATHS, "stat.minecraft.deaths", StatCategory.GENERIC),
    MOB_KILLS("mob_kills", Statistic.MOB_KILLS, "stat.minecraft.mob_kills", StatCategory.GENERIC),
    PLAYER_KILLS("player_kills", Statistic.PLAYER_KILLS, "stat.minecraft.player_kills", StatCategory.GENERIC),
    DAMAGE_DEALT("damage_dealt", Statistic.DAMAGE_DEALT, "stat.minecraft.damage_dealt", StatCategory.DAMAGE),
    DAMAGE_TAKEN("damage_taken", Statistic.DAMAGE_TAKEN, "stat.minecraft.damage_taken", StatCategory.DAMAGE),
    JUMPS("jumps", Statistic.JUMP, "stat.minecraft.jump", StatCategory.GENERIC),
    DISTANCE_WALKED("distance_walked", Statistic.WALK_ONE_CM, "stat.minecraft.walk_one_cm", StatCategory.DISTANCE),
    DISTANCE_FALLEN("distance_fallen", Statistic.FALL_ONE_CM, "stat.minecraft.fall_one_cm", StatCategory.DISTANCE),
    DISTANCE_FLOWN("distance_flown", Statistic.FLY_ONE_CM, "stat.minecraft.fly_one_cm", StatCategory.DISTANCE),
    DISTANCE_CLIMBED("distance_climbed", Statistic.CLIMB_ONE_CM, "stat.minecraft.climb_one_cm", StatCategory.DISTANCE),
    ANIMALS_BRED("animals_bred", Statistic.ANIMALS_BRED, "stat.minecraft.animals_bred", StatCategory.GENERIC),
    FISH_CAUGHT("fish_caught", Statistic.FISH_CAUGHT, "stat.minecraft.fish_caught", StatCategory.GENERIC),
    ITEMS_ENCHANTED("items_enchanted", Statistic.ITEM_ENCHANTED, "stat.minecraft.item_enchanted", StatCategory.GENERIC),
    ITEMS_DROPPED("items_dropped", Statistic.DROP_COUNT, "stat.minecraft.drop", StatCategory.GENERIC),
    RAIDS_WON("raids_won", Statistic.RAID_WIN, "stat.minecraft.raid_win", StatCategory.GENERIC);

    private static final Map<String, StatType> BY_ID = new LinkedHashMap<>();

    static {
        for (StatType type : values()) {
            BY_ID.put(type.id, type);
        }
    }

    private final String id;
    private final Statistic statistic;
    private final String translationKey;
    private final StatCategory category;

    StatType(String id, Statistic statistic, String translationKey, StatCategory category) {
        this.id = id;
        this.statistic = statistic;
        this.translationKey = translationKey;
        this.category = category;
    }

    public String id() {
        return id;
    }

    public Statistic statistic() {
        return statistic;
    }

    public String translationKey() {
        return translationKey;
    }

    public StatCategory category() {
        return category;
    }

    /** Sucht eine Statistik per id (gross-/kleinschreibungsunabhaengig). */
    public static Optional<StatType> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(id.toLowerCase(Locale.ROOT)));
    }

    /** Alle bekannten ids (in Anzeige-Reihenfolge). */
    public static Set<String> ids() {
        return BY_ID.keySet();
    }
}
