package it.markus.playerstats.update;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.config.PluginConfig;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prueft asynchron auf eine neue Plugin-Version (GitHub-Releases oder Versions-URL).
 *
 * Absicherung:
 *   - nur HTTPS,
 *   - Abfrage-Throttle (mind. {@value #MIN_CHECK_INTERVAL_MS} ms zwischen Checks,
 *     schont das GitHub-Limit von 60 Anfragen/Stunde; pro Join wird NIE gepollt,
 *     der UpdateNotifyListener liest nur den gecachten Status),
 *   - Auto-Download standardmaessig AUS (nur Hinweis), und falls aktiviert wird
 *     – sofern vorhanden – die SHA-256-Checksumme des Release-Assets geprueft.
 */
public final class UpdateChecker {

    private static final long MIN_CHECK_INTERVAL_MS = 60_000L;

    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JAR_ASSET =
            Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");
    private static final Pattern SHA_ASSET =
            Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.sha256)\"");
    private static final Pattern SHA_HEX = Pattern.compile("\\b[a-fA-F0-9]{64}\\b");

    private final PlayerStatsPlugin plugin;

    private volatile boolean updateAvailable;
    private volatile String latestVersion;
    private volatile String downloadUrl;
    private volatile long lastCheck;

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

    /** Stoesst eine asynchrone Pruefung an (No-op, wenn deaktiviert oder zu frisch). */
    public void check() {
        if (!plugin.config().updaterEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastCheck != 0 && now - lastCheck < MIN_CHECK_INTERVAL_MS) {
            return; // Throttle: nicht zu oft die API anfragen
        }
        lastCheck = now;
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
                download(release);
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
                    : new Release(stripV(version), emptyToNull(cfg.updaterDownloadUrl()), null);
        }

        // Standard: GitHub-Releases
        String repo = cfg.updaterGithubRepo();
        if (repo == null || repo.isBlank()) {
            plugin.getLogger().warning("updater.github-repo ist nicht gesetzt – Pruefung uebersprungen.");
            return null;
        }

        String scope;
        if (isPrerelease(cfg.updaterChannel())) {
            // Kanal "prerelease": neuestes Release JEDER Art (inkl. Beta/Pre-Release).
            String json = httpGet("https://api.github.com/repos/" + repo + "/releases?per_page=10");
            int first = json.indexOf("\"tag_name\"");
            if (first < 0) {
                return null;
            }
            int second = json.indexOf("\"tag_name\"", first + 1);
            scope = second < 0 ? json : json.substring(0, second); // nur das erste Release
        } else {
            // Kanal "stable": nur stabile, veroeffentlichte Releases.
            scope = httpGet("https://api.github.com/repos/" + repo + "/releases/latest");
        }

        Matcher tag = TAG.matcher(scope);
        if (!tag.find()) {
            return null;
        }
        Matcher jar = JAR_ASSET.matcher(scope);
        String url = jar.find() ? jar.group(1) : null;
        Matcher sha = SHA_ASSET.matcher(scope);
        String shaUrl = sha.find() ? sha.group(1) : null;
        return new Release(stripV(tag.group(1)), url, shaUrl);
    }

    private static boolean isPrerelease(String channel) {
        return "prerelease".equalsIgnoreCase(channel) || "beta".equalsIgnoreCase(channel);
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

    private void download(Release release) {
        String url = release.downloadUrl();
        if (url == null || !url.startsWith("https://")) {
            plugin.getLogger().warning("Auto-Download abgebrochen: keine gueltige HTTPS-Download-URL.");
            return;
        }
        Path target = null;
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
            target = new File(updateFolder, jar.getName()).toPath();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "PlayerStats-Updater")
                    .GET()
                    .build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
            if (response.statusCode() / 100 != 2) {
                plugin.getLogger().warning("Auto-Download fehlgeschlagen: HTTP " + response.statusCode());
                Files.deleteIfExists(target);
                return;
            }

            if (!verifyChecksum(release, target)) {
                return; // verifyChecksum hat ggf. geloescht/geloggt
            }

            plugin.getLogger().info("Update heruntergeladen nach " + target.getFileName()
                    + " – wird beim naechsten Neustart installiert.");
        } catch (Exception ex) {
            plugin.getLogger().warning("Auto-Download fehlgeschlagen: " + ex.getMessage());
            if (target != null) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException ignored) {
                    // egal
                }
            }
        }
    }

    /** true, wenn der Download installiert werden darf (Checksumme ok oder erlaubt). */
    private boolean verifyChecksum(Release release, Path target) throws Exception {
        if (release.sha256Url() != null) {
            String expected = extractSha(httpGet(release.sha256Url()));
            String actual = sha256(target);
            if (expected != null && expected.equalsIgnoreCase(actual)) {
                plugin.getLogger().info("Checksumme verifiziert (SHA-256).");
                return true;
            }
            plugin.getLogger().warning("Checksumme stimmt nicht ueberein – Download verworfen.");
            Files.deleteIfExists(target);
            return false;
        }

        // Keine Checksumme im Release vorhanden.
        if (plugin.config().updaterRequireChecksum()) {
            plugin.getLogger().warning("Keine .sha256-Checksumme im Release – Auto-Download verworfen "
                    + "(updater.require-checksum=true).");
            Files.deleteIfExists(target);
            return false;
        }
        plugin.getLogger().warning("Keine .sha256-Checksumme im Release – ungeprueft uebernommen.");
        return true;
    }

    private static String extractSha(String text) {
        Matcher m = SHA_HEX.matcher(text);
        return m.find() ? m.group() : null;
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
        }
        StringBuilder sb = new StringBuilder(64);
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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

    private record Release(String version, String downloadUrl, String sha256Url) {
    }
}
