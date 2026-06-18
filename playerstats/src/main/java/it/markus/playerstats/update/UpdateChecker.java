package it.markus.playerstats.update;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.config.PluginConfig;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prueft asynchron auf eine neue Plugin-Version.
 *
 * Quelle konfigurierbar: GitHub-Releases-API oder eine einfache Versions-URL.
 * Standard ist „nur Hinweis"; auto-download laedt das JAR (nur ueber HTTPS) in
 * den Update-Ordner, den Paper beim naechsten Neustart anwendet.
 *
 * Sicherheitshinweis: Auto-Download fuehrt fremden Code aus. Nur mit einer
 * vertrauenswuerdigen Quelle aktivieren – idealerweise ergaenzt um eine
 * Checksummen-Pruefung (hier bewusst nicht implementiert, da es eine
 * veroeffentlichte Checksumme voraussetzt).
 */
public final class UpdateChecker {

    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JAR_ASSET =
            Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");

    private final PlayerStatsPlugin plugin;

    private volatile boolean updateAvailable;
    private volatile String latestVersion;
    private volatile String downloadUrl;

    public UpdateChecker(PlayerStatsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String latestVersion() {
        return latestVersion;
    }

    public String downloadUrl() {
        return downloadUrl;
    }

    /** Stoesst eine asynchrone Pruefung an (No-op, wenn deaktiviert). */
    public void check() {
        if (!plugin.config().updaterEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::runCheck);
    }

    private void runCheck() {
        try {
            Release release = fetchLatest(plugin.config());
            if (release == null || release.version() == null) {
                return;
            }
            String current = plugin.getPluginMeta().getVersion();
            if (!isNewer(release.version(), current)) {
                plugin.getLogger().info("PlayerStats ist aktuell (Version " + current + ").");
                return;
            }
            this.latestVersion = release.version();
            this.downloadUrl = release.downloadUrl();
            this.updateAvailable = true;
            plugin.getLogger().info("Update verfuegbar: " + current + " -> " + release.version()
                    + (release.downloadUrl() != null ? " (" + release.downloadUrl() + ")" : ""));

            plugin.discord().updateAvailable(release.version());

            if (plugin.config().updaterAutoDownload()) {
                download(release.downloadUrl());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Update-Pruefung fehlgeschlagen: " + ex.getMessage());
        }
    }

    private Release fetchLatest(PluginConfig cfg) throws Exception {
        if ("url".equalsIgnoreCase(cfg.updaterSource())) {
            if (cfg.updaterVersionUrl().isBlank()) {
                plugin.getLogger().warning("updater.version-url ist leer – Pruefung uebersprungen.");
                return null;
            }
            String version = httpGet(cfg.updaterVersionUrl()).trim();
            return version.isEmpty() ? null
                    : new Release(stripV(version), emptyToNull(cfg.updaterDownloadUrl()));
        }

        // Standard: GitHub-Releases
        String repo = cfg.updaterGithubRepo();
        if (repo == null || repo.isBlank()) {
            plugin.getLogger().warning("updater.github-repo ist nicht gesetzt – Pruefung uebersprungen.");
            return null;
        }
        String json = httpGet("https://api.github.com/repos/" + repo + "/releases/latest");
        Matcher tag = TAG.matcher(json);
        if (!tag.find()) {
            return null;
        }
        Matcher jar = JAR_ASSET.matcher(json);
        String url = jar.find() ? jar.group(1) : null;
        return new Release(stripV(tag.group(1)), url);
    }

    private String httpGet(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "PlayerStats-Updater")
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + " bei " + url);
        }
        return response.body();
    }

    private void download(String url) {
        if (url == null || !url.startsWith("https://")) {
            plugin.getLogger().warning("Auto-Download abgebrochen: keine gueltige HTTPS-Download-URL.");
            return;
        }
        try {
            File jar = plugin.pluginJarFile();
            if (jar == null) {
                plugin.getLogger().warning("Auto-Download abgebrochen: eigene JAR-Datei nicht gefunden.");
                return;
            }
            File updateFolder = Bukkit.getUpdateFolderFile();
            if (!updateFolder.exists() && !updateFolder.mkdirs()) {
                plugin.getLogger().warning("Auto-Download abgebrochen: Update-Ordner nicht anlegbar.");
                return;
            }
            File target = new File(updateFolder, jar.getName());

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "PlayerStats-Updater")
                    .GET()
                    .build();
            HttpResponse<java.nio.file.Path> response =
                    client.send(request, HttpResponse.BodyHandlers.ofFile(target.toPath()));
            if (response.statusCode() / 100 != 2) {
                plugin.getLogger().warning("Auto-Download fehlgeschlagen: HTTP " + response.statusCode());
                return;
            }
            plugin.getLogger().info("Update heruntergeladen nach " + target.getName()
                    + " – wird beim naechsten Neustart installiert.");
        } catch (Exception ex) {
            plugin.getLogger().warning("Auto-Download fehlgeschlagen: " + ex.getMessage());
        }
    }

    // --- Versionsvergleich -------------------------------------------------

    /** true, wenn {@code remote} eine hoehere Version als {@code current} ist. */
    static boolean isNewer(String remote, String current) {
        int[] r = parse(remote);
        int[] c = parse(current);
        int n = Math.max(r.length, c.length);
        for (int i = 0; i < n; i++) {
            int rv = i < r.length ? r[i] : 0;
            int cv = i < c.length ? c[i] : 0;
            if (rv != cv) {
                return rv > cv;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        List<Integer> parts = new ArrayList<>();
        for (String chunk : stripV(version).split("[^0-9]+")) {
            if (!chunk.isEmpty()) {
                try {
                    parts.add(Integer.parseInt(chunk));
                } catch (NumberFormatException ignored) {
                    // sehr grosse/ungueltige Zahl -> ignorieren
                }
            }
        }
        int[] out = new int[parts.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = parts.get(i);
        }
        return out;
    }

    private static String stripV(String v) {
        String t = v.trim();
        return (t.startsWith("v") || t.startsWith("V")) ? t.substring(1) : t;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private record Release(String version, String downloadUrl) {
    }
}
