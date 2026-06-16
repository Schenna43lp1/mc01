package it.markus.playerstats.unit;

import it.markus.playerstats.config.PluginConfig;
import it.markus.playerstats.stat.StatCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rechnet Roh-Statistikwerte in die konfigurierte Einheit um und formatiert sie
 * als anzeigefertigen Text (deutsche Tausender-Trennung).
 */
public final class UnitConverter {

    private static final long TICKS_PER_SECOND = 20L;

    private UnitConverter() {
    }

    public static String format(StatCategory category, long raw, PluginConfig cfg) {
        return switch (category) {
            case TIME -> time(raw, cfg);
            case DISTANCE -> distance(raw, cfg);
            case DAMAGE -> damage(raw, cfg);
            case GENERIC -> number(raw);
        };
    }

    private static String number(long value) {
        return String.format(Locale.GERMAN, "%,d", value);
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
