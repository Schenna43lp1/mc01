package it.markus.playerstats.stat;

import it.markus.playerstats.custom.CustomKeys;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Zentrale Registry aller Statistiken. Jede {@link StatDefinition} traegt ihren
 * eigenen {@link StatResolver}, sodass Vanilla-, Gruppen-, Custom- und berechnete
 * Statistiken einheitlich abgefragt werden koennen.
 *
 * Die Material-/Statistik-Listen der Gruppen werden NICHT hier eingefroren,
 * sondern zur Laufzeit aus dem {@code GroupRegistry} im Kontext gelesen – so
 * wirkt ein /playerstats reload sofort.
 */
public final class StatRegistry {

    private final Map<String, StatDefinition> defs = new LinkedHashMap<>();

    public StatRegistry() {
        // --- Vanilla (untyped) – clientseitig uebersetzbar ----------------
        vanilla("playtime", StatCategory.TIME, "stat.minecraft.play_time", Statistic.PLAY_ONE_MINUTE);
        vanilla("deaths", StatCategory.GENERIC, "stat.minecraft.deaths", Statistic.DEATHS);
        vanilla("mob_kills", StatCategory.GENERIC, "stat.minecraft.mob_kills", Statistic.MOB_KILLS);
        vanilla("player_kills", StatCategory.GENERIC, "stat.minecraft.player_kills", Statistic.PLAYER_KILLS);
        vanilla("damage_dealt", StatCategory.DAMAGE, "stat.minecraft.damage_dealt", Statistic.DAMAGE_DEALT);
        vanilla("damage_taken", StatCategory.DAMAGE, "stat.minecraft.damage_taken", Statistic.DAMAGE_TAKEN);
        vanilla("jumps", StatCategory.GENERIC, "stat.minecraft.jump", Statistic.JUMP);
        vanilla("fish_caught", StatCategory.GENERIC, "stat.minecraft.fish_caught", Statistic.FISH_CAUGHT);
        vanilla("animals_bred", StatCategory.GENERIC, "stat.minecraft.animals_bred", Statistic.ANIMALS_BRED);
        vanilla("items_enchanted", StatCategory.GENERIC, "stat.minecraft.item_enchanted", Statistic.ITEM_ENCHANTED);
        vanilla("raids_won", StatCategory.GENERIC, "stat.minecraft.raid_win", Statistic.RAID_WIN);
        vanilla("time_since_death", StatCategory.TIME, "stat.minecraft.time_since_death", Statistic.TIME_SINCE_DEATH);
        vanilla("walked", StatCategory.DISTANCE, "stat.minecraft.walk_one_cm", Statistic.WALK_ONE_CM);
        vanilla("sprinted", StatCategory.DISTANCE, "stat.minecraft.sprint_one_cm", Statistic.SPRINT_ONE_CM);
        vanilla("horse_distance", StatCategory.DISTANCE, "stat.minecraft.horse_one_cm", Statistic.HORSE_ONE_CM);
        vanilla("boat_distance", StatCategory.DISTANCE, "stat.minecraft.boat_one_cm", Statistic.BOAT_ONE_CM);
        vanilla("aviate_distance", StatCategory.DISTANCE, "stat.minecraft.aviate_one_cm", Statistic.AVIATE_ONE_CM);
        vanilla("swim_distance", StatCategory.DISTANCE, "stat.minecraft.swim_one_cm", Statistic.SWIM_ONE_CM);

        // --- Vanilla (Mob-Kills nach Typ) – Name aus language.yml ---------
        killEntity("zombies_killed", EntityType.ZOMBIE);
        killEntity("skeletons_killed", EntityType.SKELETON);
        killEntity("creepers_killed", EntityType.CREEPER);

        // --- Vanilla-Gruppen (Summen) -------------------------------------
        add("total_distance", StatCategory.DISTANCE, null, StatKind.VANILLA_GROUP, ctx -> {
            long sum = 0;
            for (Statistic s : ctx.groups().distanceStats()) {
                sum += safe(() -> ctx.player().getStatistic(s));
            }
            return sum;
        });
        add("total_blocks", StatCategory.GENERIC, null, StatKind.VANILLA_GROUP, ctx -> {
            long sum = 0;
            for (Material m : ctx.groups().allBlocks()) {
                sum += safe(() -> ctx.player().getStatistic(Statistic.MINE_BLOCK, m));
            }
            return sum;
        });
        mineGroup("wood", "logs");
        mineGroup("ores", "ores");
        mineGroup("diamonds", "diamonds");
        mineGroup("ancient_debris", "ancient_debris");

        // --- Vanilla (Zeitpunkte via OfflinePlayer) -----------------------
        add("first_join", StatCategory.DATE, null, StatKind.VANILLA, ctx -> ctx.player().getFirstPlayed());
        add("last_login", StatCategory.DATE, null, StatKind.VANILLA, ctx -> ctx.player().getLastSeen());

        // --- Custom (event-getrackt) --------------------------------------
        custom("login_streak", StatCategory.GENERIC, CustomKeys.LOGIN_STREAK_CURRENT);
        custom("login_streak_longest", StatCategory.GENERIC, CustomKeys.LOGIN_STREAK_LONGEST);
        custom("killstreak", StatCategory.GENERIC, CustomKeys.KILLSTREAK_CURRENT);
        custom("killstreak_longest", StatCategory.GENERIC, CustomKeys.KILLSTREAK_LONGEST);
        custom("elytra_time", StatCategory.TIME, CustomKeys.ELYTRA_TICKS);
        custom("rare_blocks", StatCategory.GENERIC, CustomKeys.RARE_BLOCKS);
        custom("plants_harvested", StatCategory.GENERIC, CustomKeys.PLANTS_HARVESTED);
        custom("honey_collected", StatCategory.GENERIC, CustomKeys.HONEY_COLLECTED);
        custom("berries_collected", StatCategory.GENERIC, CustomKeys.BERRIES_COLLECTED);
        custom("session_record", StatCategory.GENERIC, CustomKeys.SESSION_BLOCKS_BEST);
        custom("headshots", StatCategory.GENERIC, CustomKeys.HEADSHOTS);
        custom("arrows_shot", StatCategory.GENERIC, CustomKeys.ARROWS_SHOT);
        custom("arrows_hit", StatCategory.GENERIC, CustomKeys.ARROWS_HIT);

        // --- Berechnet (nie gespeichert) ----------------------------------
        add("kd_ratio", StatCategory.RATIO, null, StatKind.COMPUTED, ctx -> {
            long kills = safe(() -> ctx.player().getStatistic(Statistic.PLAYER_KILLS))
                    + safe(() -> ctx.player().getStatistic(Statistic.MOB_KILLS));
            long deaths = safe(() -> ctx.player().getStatistic(Statistic.DEATHS));
            return (double) kills / Math.max(1L, deaths);
        });
        add("arrow_accuracy", StatCategory.PERCENT, null, StatKind.COMPUTED, ctx -> {
            long shot = ctx.custom().get(ctx.uuid(), CustomKeys.ARROWS_SHOT);
            long hit = ctx.custom().get(ctx.uuid(), CustomKeys.ARROWS_HIT);
            return shot <= 0 ? 0.0 : (double) hit / shot * 100.0;
        });
    }

    // --- Registrierungs-Helfer --------------------------------------------

    private void vanilla(String id, StatCategory cat, String key, Statistic stat) {
        add(id, cat, key, StatKind.VANILLA, ctx -> safe(() -> ctx.player().getStatistic(stat)));
    }

    private void killEntity(String id, EntityType type) {
        add(id, StatCategory.GENERIC, null, StatKind.VANILLA,
                ctx -> safe(() -> ctx.player().getStatistic(Statistic.KILL_ENTITY, type)));
    }

    private void mineGroup(String id, String groupName) {
        add(id, StatCategory.GENERIC, null, StatKind.VANILLA_GROUP, ctx -> {
            long sum = 0;
            for (Material m : ctx.groups().blockGroup(groupName)) {
                sum += safe(() -> ctx.player().getStatistic(Statistic.MINE_BLOCK, m));
            }
            return sum;
        });
    }

    private void custom(String id, StatCategory cat, String key) {
        defs.put(id, new StatDefinition(id, cat, null, StatKind.CUSTOM,
                ctx -> ctx.custom().get(ctx.uuid(), key), key));
    }

    private void add(String id, StatCategory cat, String key, StatKind kind, StatResolver resolver) {
        defs.put(id, new StatDefinition(id, cat, key, kind, resolver, null));
    }

    /** Liefert 0 statt einer Ausnahme, falls eine Statistik einen Qualifier verlangt. */
    private static long safe(java.util.function.LongSupplier supplier) {
        try {
            return supplier.getAsLong();
        } catch (RuntimeException ex) {
            return 0L;
        }
    }

    // --- Zugriff -----------------------------------------------------------

    public Optional<StatDefinition> byId(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(defs.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<StatDefinition> all() {
        return defs.values();
    }

    public Set<String> ids() {
        return defs.keySet();
    }
}
