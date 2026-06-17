package it.markus.playerstats.custom;

/**
 * Schluessel der CUSTOM-Statistiken im Storage. Zentral gehalten, damit
 * Listener, Resolver und Persistenz dieselben Namen nutzen.
 */
public final class CustomKeys {

    public static final String KILLSTREAK_CURRENT = "killstreak_current";
    public static final String KILLSTREAK_LONGEST = "killstreak_longest";

    public static final String LOGIN_STREAK_CURRENT = "login_streak_current";
    public static final String LOGIN_STREAK_LONGEST = "login_streak_longest";
    public static final String LOGIN_LAST_DAY = "login_last_day"; // epoch-day

    public static final String ELYTRA_TICKS = "elytra_ticks";

    public static final String ARROWS_SHOT = "arrows_shot";
    public static final String ARROWS_HIT = "arrows_hit";
    public static final String HEADSHOTS = "headshots";

    public static final String RARE_BLOCKS = "rare_blocks";
    public static final String PLANTS_HARVESTED = "plants_harvested";
    public static final String HONEY_COLLECTED = "honey_collected";
    public static final String BERRIES_COLLECTED = "berries_collected";

    public static final String SESSION_BLOCKS_BEST = "session_blocks_best";

    private CustomKeys() {
    }
}
