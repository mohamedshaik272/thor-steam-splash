package dev.mohamedshaik.nowplaying;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// The most recent game Cocoon launched, read from Cocoon's own launch log on the SD card.
final class CocoonLaunch {
    static final String COCOON = "rip.moth.cocoonshell";

    final String name;
    final String player;
    final File rom;
    final long time;

    private CocoonLaunch(String name, String player, File rom, long time) {
        this.name = name;
        this.player = player;
        this.rom = rom;
        this.time = time;
    }

    private static final Pattern LAUNCH = Pattern.compile("Launch: (.+?) ═");
    private static final Pattern PLAYER = Pattern.compile("\\[INFO\\] Player: .*?\\(([\\w.]+)/");
    private static final Pattern FILE = Pattern.compile("Resolved file: (.+?) \\(exists=true");

    static CocoonLaunch latest() {
        File log = findLog();
        if (log == null) return null;
        String tail;
        try (RandomAccessFile f = new RandomAccessFile(log, "r")) {
            long start = Math.max(0, f.length() - 16384);
            byte[] buf = new byte[(int) (f.length() - start)];
            f.seek(start);
            f.readFully(buf);
            tail = new String(buf, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
        int at = tail.lastIndexOf("Launch: ");
        if (at < 0) return null;
        String block = tail.substring(tail.lastIndexOf('\n', at) + 1);
        Matcher n = LAUNCH.matcher(block);
        Matcher p = PLAYER.matcher(block);
        Matcher r = FILE.matcher(block);
        if (!n.find() || !p.find()) return null;
        File rom = r.find() ? new File(r.group(1)) : null;
        return new CocoonLaunch(n.group(1).trim(), p.group(1), rom, stamp(block));
    }

    // Lines start with a local timestamp like [2026-11-19 18:30:03.660].
    private static long stamp(String line) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).parse(line.substring(1, 24)).getTime();
        } catch (ParseException | StringIndexOutOfBoundsException e) {
            return 0;
        }
    }

    private static File findLog() {
        File[] roots = new File("/storage").listFiles();
        if (roots == null) return null;
        for (File root : roots) {
            File log = new File(root, "Cocoon/launch_debug.log");
            if (log.canRead()) return log;
        }
        return null;
    }

    // Steam games are .steam files that hold the app id.
    String steamAppId() {
        if (rom == null || !rom.getName().endsWith(".steam")) return null;
        try {
            String id = new String(Files.readAllBytes(rom.toPath()), StandardCharsets.UTF_8).trim();
            return id.matches("\\d+") ? id : null;
        } catch (IOException e) {
            return null;
        }
    }

    // Cocoon keeps scraped art at Cocoon/downloaded_media/<platform>/<kind>/<rom name>[__cocoon_edit_...].<ext>.
    // The platform folder isn't always the ROM folder (ROMs/steam scrapes into windows), so every platform is
    // searched, and the newest match wins.
    File media(String kind) {
        if (rom == null) return null;
        File platform = rom.getParentFile();
        File roms = platform == null ? null : platform.getParentFile();
        if (roms == null || roms.getParentFile() == null) return null;
        File[] platforms = new File(roms.getParentFile(), "Cocoon/downloaded_media").listFiles(File::isDirectory);
        if (platforms == null) return null;
        String base = rom.getName().replaceFirst("\\.[^.]+$", "");
        File best = null;
        for (File p : platforms) {
            File[] files = new File(p, kind).listFiles((dir, name) ->
                    name.startsWith(base + ".") || name.startsWith(base + "__cocoon_edit_"));
            if (files == null) continue;
            for (File f : files) {
                if (f.canRead() && (best == null || f.lastModified() > best.lastModified())) best = f;
            }
        }
        return best;
    }
}
