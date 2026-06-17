package it.markus.playerstats.unit;

import it.markus.playerstats.config.PluginConfig;
import it.markus.playerstats.stat.StatCategory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rechnet Roh-Statistikwerte in die konfigurierte Einheit um und formatiert sie
 * als anzeigefertigen Text (deutsche Tausender-Trennung).
 */
public final class UnitConverter {

    private static final long TICKS_PER_SECOND = 20L;
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private UnitConverter() {
    }

    public static String format(StatCategory category, double raw, PluginConfig cfg) {
        return switch (category) {
            case TIME -> time((long) raw, cfg);
            case DISTANCE -> distance((long) raw, cfg);
            case DAMAGE -> damage((long) raw, cfg);
            case DATE -> date((long) raw);
            case RATIO -> String.format(Locale.GERMAN, "%.2f", raw);
            case PERCENT -> String.format(Locale.GERMAN, "%.1f %%", raw);
            case GENERIC -> cfg.abbreviateNumbers() ? abbreviate(Math.round(raw)) : number(Math.round(raw));
        };
    }

    private static String number(long value) {
        return String.format(Locale.GERMAN, "%,d", value);
    }

    /** Kuerzt grosse Zahlen ab: 1.234 -> "1,2K", 5.600.000 -> "5,6M". */
    private static String abbreviate(long value) {
        if (Math.abs(value) < 1000) {
            return number(value);
        }
        String[] suffixes = {"K", "M", "Mrd", "Bio"};
        double d = value;
        int i = -1;
        while (Math.abs(d) >= 1000 && i < suffixes.length - 1) {
            d /= 1000.0;
            i++;
        }
        return String.format(Locale.GERMAN, "%.1f%s", d, suffixes[i]);
    }

    private static String date(long epochMs) {
        return epochMs <= 0 ? "—" : DATE.format(Instant.ofEpochMilli(epochMs));
    }

    // --- Zeit (Rohwert in Ticks) ------------------------------------------

    private static String time(long ticks, PluginConfig cfg) {
        return switch (cfg.timeUnit()) {
            case TICKS -> number(ticks) + " Ticks";
            case SECONDS -> number(ticks / TICKS_PER_SECOND) + " s";
            case MINUTES -> number(ticks / (TICKS_PER_SECOND * 60)) + " min";
            case HOURS -> number(ticks / (TICKS_PER_SECOND * 3_600)) + " h";
            case DAYS -> number(ticks / (TICKS_PER_SECOND * 86_400)) + " d";
            case AUTO -> autoTime(ticks, cfg.autoDetail());
        };
    }

    private static String autoTime(long ticks, int detail) {
        long totalSeconds = ticks / TICKS_PER_SECOND;
        if (totalSeconds <= 0) {
            return "0s";
        }
        long[] values = {
                totalSeconds / 86_400,
                (totalSeconds % 86_400) / 3_600,
                (totalSeconds % 3_600) / 60,
                totalSeconds % 60
        };
        String[] labels = {"d", "h", "m", "s"};

        List<String> parts = new ArrayList<>();
        boolean started = false;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > 0 || started) {
                started = true;
                parts.add(values[i] + labels[i]);
                if (parts.size() >= Math.max(1, detail)) {
                    break;
                }
            }
        }
        return String.join(" ", parts);
    }

    // --- Distanz (Rohwert in Zentimetern) ---------------------------------

    private static String distance(long cm, PluginConfig cfg) {
        return switch (cfg.distanceUnit()) {
            case CM -> number(cm) + " cm";
            case BLOCKS -> number(cm / 100) + " Bloecke";
            case KM -> String.format(Locale.GERMAN, "%.2f km", cm / 100_000.0);
            case AUTO -> cm >= 100_000
                    ? String.format(Locale.GERMAN, "%.2f km", cm / 100_000.0)
                    : number(cm / 100) + " Bloecke";
        };
    }

    // --- Schaden (Rohwert in 1/10 Lebenspunkt) ----------------------------

    private static String damage(long raw, PluginConfig cfg) {
        return switch (cfg.damageUnit()) {
            case RAW -> number(raw);
            case HP -> String.format(Locale.GERMAN, "%.1f HP", raw / 10.0);
            case HEARTS -> String.format(Locale.GERMAN, "%.1f Herzen", raw / 20.0);
        };
    }
}
