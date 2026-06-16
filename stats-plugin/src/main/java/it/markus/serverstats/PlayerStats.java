package it.markus.serverstats;

/**
 * Reiner Datencontainer fuer die gespeicherten Werte eines Spielers.
 *
 * Bewusst keine Logik hier drin: Diese Klasse haelt nur Zahlen.
 * Das Laden, Speichern und Hochzaehlen passiert im {@link StatsStore}.
 */
public final class PlayerStats {

    private String name;
    private long firstJoin;        // Zeitpunkt des allerersten Beitritts (epoch millis)
    private long lastSeen;         // zuletzt online gesehen (epoch millis)
    private int joinCount;         // wie oft insgesamt beigetreten
    private long playtimeMs;       // gesammelte Spielzeit in ms (ohne laufende Sitzung)

    // --- Gameplay-Statistiken (werden ueber Events gezaehlt) ---
    private long deaths;
    private long mobKills;
    private long blocksMined;
    private long distanceCm;       // zurueckgelegte Strecke in Zentimetern

    // Wie viele Spielzeit-Meilensteine bereits angekuendigt wurden.
    private int milestonesReached;

    public PlayerStats(String name, long firstJoin, long lastSeen, int joinCount, long playtimeMs,
                       long deaths, long mobKills, long blocksMined, long distanceCm,
                       int milestonesReached) {
        this.name = name;
        this.firstJoin = firstJoin;
        this.lastSeen = lastSeen;
        this.joinCount = joinCount;
        this.playtimeMs = playtimeMs;
        this.deaths = deaths;
        this.mobKills = mobKills;
        this.blocksMined = blocksMined;
        this.distanceCm = distanceCm;
        this.milestonesReached = milestonesReached;
    }

    /** Frischer Eintrag fuer einen neuen Spieler. */
    public static PlayerStats fresh(String name, long now) {
        return new PlayerStats(name, now, now, 0, 0L, 0L, 0L, 0L, 0L, 0);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFirstJoin() {
        return firstJoin;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public int getJoinCount() {
        return joinCount;
    }

    public void incrementJoinCount() {
        this.joinCount++;
    }

    public long getPlaytimeMs() {
        return playtimeMs;
    }

    public void addPlaytimeMs(long deltaMs) {
        this.playtimeMs += deltaMs;
    }

    public long getDeaths() {
        return deaths;
    }

    public void incrementDeaths() {
        this.deaths++;
    }

    public long getMobKills() {
        return mobKills;
    }

    public void incrementMobKills() {
        this.mobKills++;
    }

    public long getBlocksMined() {
        return blocksMined;
    }

    public void incrementBlocksMined() {
        this.blocksMined++;
    }

    public long getDistanceCm() {
        return distanceCm;
    }

    public void addDistanceCm(long cm) {
        this.distanceCm += cm;
    }

    public int getMilestonesReached() {
        return milestonesReached;
    }

    public void setMilestonesReached(int milestonesReached) {
        this.milestonesReached = milestonesReached;
    }
}
