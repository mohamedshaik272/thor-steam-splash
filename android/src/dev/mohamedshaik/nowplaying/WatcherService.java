package dev.mohamedshaik.nowplaying;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Opens the viewer on the bottom screen while a streaming client or a game launched from Cocoon is on the
// top screen, and closes it shortly after it leaves.
public class WatcherService extends AccessibilityService {
    private static final long CHECK_MS = 1500;
    private static final long CLOSE_AFTER_MS = 5000;
    // These draw their own second screen, so the viewer must stay out of the way.
    private static final Set<String> DUAL_SCREEN = new HashSet<>(Arrays.asList(
            "me.magnum.melondualds", "org.azahar_emu.azahar", "info.cemu.cemu"));

    static WeakReference<Activity> viewer = new WeakReference<>(null);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String mode = "";
    private long goneSince;

    @Override
    protected void onServiceConnected() {
        handler.post(this::check);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    private String topPackage() {
        SparseArray<List<AccessibilityWindowInfo>> all = getWindowsOnAllDisplays();
        List<AccessibilityWindowInfo> top = all.get(Display.DEFAULT_DISPLAY);
        if (top == null) return null;
        for (AccessibilityWindowInfo w : top) {
            if (w.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root = w.getRoot();
            if (root == null || root.getPackageName() == null) return null;
            return root.getPackageName().toString();
        }
        return null;
    }

    // "" means nothing to show, "stream" the PC page, "game:<name>" a game Cocoon launched.
    private String wanted(String pkg, CocoonLaunch[] out) {
        if (pkg == null) return "";
        if (StreamActivity.isClient(pkg)) return "stream";
        if (pkg.equals(CocoonLaunch.COCOON) || pkg.equals(getPackageName()) || DUAL_SCREEN.contains(pkg)) return "";
        CocoonLaunch l = CocoonLaunch.latest();
        if (l == null || !l.player.equals(pkg)) return "";
        out[0] = l;
        return "game:" + l.name;
    }

    private void check() {
        CocoonLaunch[] launch = new CocoonLaunch[1];
        String next = wanted(topPackage(), launch);
        long now = System.currentTimeMillis();
        if (next.equals(mode)) {
            goneSince = 0;
        } else if (!next.isEmpty()) {
            goneSince = 0;
            close();
            mode = next;
            String m = next;
            CocoonLaunch l = launch[0];
            handler.postDelayed(() -> { if (m.equals(mode)) open(l); }, viewer.get() == null ? 0 : 600);
        } else if (!mode.isEmpty()) {
            if (goneSince == 0) goneSince = now;
            if (now - goneSince >= CLOSE_AFTER_MS) {
                mode = "";
                goneSince = 0;
                close();
            }
        }
        handler.postDelayed(this::check, CHECK_MS);
    }

    private void open(CocoonLaunch l) {
        Intent i = new Intent(this, MainActivity.class);
        if (l != null) {
            i.putExtra(MainActivity.EXTRA_NAME, l.name);
            i.putExtra(MainActivity.EXTRA_STEAM_ID, l.steamAppId());
            java.io.File hero = l.media("hero");
            java.io.File logo = l.media("logo");
            if (hero != null) i.putExtra(MainActivity.EXTRA_HERO, hero.getAbsolutePath());
            if (logo != null) i.putExtra(MainActivity.EXTRA_LOGO, logo.getAbsolutePath());
        }
        Screens.openViewer(this, i);
        String top = topPackage();
        handler.postDelayed(() -> refocusTop(top), 700);
    }

    // Opening the viewer makes the bottom screen the focused one, which takes the controller away from the game.
    // Relaunching the top app's own launcher intent brings its existing task back to the front with focus.
    private void refocusTop(String pkg) {
        if (pkg == null) return;
        Intent back = getPackageManager().getLaunchIntentForPackage(pkg);
        if (back != null) Screens.launchOn(this, back, Display.DEFAULT_DISPLAY);
    }

    private void close() {
        Activity a = viewer.get();
        if (a != null) a.finishAndRemoveTask();
        viewer.clear();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
