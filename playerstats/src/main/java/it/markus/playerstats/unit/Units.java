package it.markus.playerstats.unit;

import java.util.Locale;

/**
 * Die waehlbaren Einheiten je Statistik-Kategorie, samt Parser fuer die
 * Config-Strings.
 */
public final class Units {

    public enum Time {
        TICKS, SECONDS, MINUTES, HOURS, DAYS, AUTO
    }

    public enum Distance {
        CM, BLOCKS, KM, AUTO
    }

    public enum Damage {
        RAW, HP, HEARTS
    }

    private Units() {
    }

    public static Time time(String raw, Time fallback) {
        return parse(Time.class, raw, fallback);
    }

    public static Distance distance(String raw, Distance fallback) {
        return parse(Distance.class, raw, fallback);
    }

    public static Damage damage(String raw, Damage fallback) {
        return parse(Damage.class, raw, fallback);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String raw, E fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
