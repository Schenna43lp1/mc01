package it.markus.serverstats;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatierungs-Helfer fuer die Anzeige (Dauer, Datum, Distanz).
 */
public final class Format {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private Format() {
    }

    /** Millisekunden -> "Xd Yh Zm". */
    public static String duration(long ms) {
        long totalSeconds = ms / 1000;
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (days > 0 || hours > 0) {
            sb.append(hours).append("h ");
        }
        sb.append(minutes).append("m");
        return sb.toString();
    }

    /** Epoch-Millis -> "dd.MM.yyyy HH:mm" (oder "unbekannt"). */
    public static String date(long epochMs) {
        if (epochMs <= 0) {
            return "unbekannt";
        }
        return DATE.format(Instant.ofEpochMilli(epochMs));
    }

    /** Zentimeter -> "x,y km" ab 1 km, sonst "x m". */
    public static String distance(long cm) {
        double km = cm / 100_000.0;
        if (km >= 1.0) {
            return String.format(Locale.GERMAN, "%.1f km", km);
        }
        return String.format(Locale.GERMAN, "%d m", cm / 100);
    }
}
