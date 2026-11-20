package dev.mohamedshaik.nowplaying;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Shows the bottom-screen page while a streaming client, or a game Cocoon just launched, is on the top screen.
// The page is an accessibility overlay rather than an activity, so it never takes controller focus from the
// top screen and never lingers in the bottom screen's task stack after it is hidden.
public class WatcherService extends AccessibilityService {
    private static final long CHECK_MS = 1500;
    private static final long HIDE_AFTER_MS = 5000;
    // A Cocoon launch counts for the app that comes to the top screen within this long of it.
    private static final long LAUNCH_WINDOW_MS = 30000;
    // These draw their own second screen, so the page must stay out of the way.
    private static final Set<String> DUAL_SCREEN = new HashSet<>(Arrays.asList(
            "me.magnum.melondualds", "org.azahar_emu.azahar", "info.cemu.cemu"));

    private static WatcherService running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String mode = "";
    private long goneSince;
    private String topPkg;
    private long topSince;
    private boolean editing;
    private CocoonLaunch shown;
    private WindowManager wm;
    private Splash splash;

    static boolean isRunning() {
        return running != null;
    }

    static void editorClosed() {
        WatcherService s = running;
        if (s == null) return;
        s.editing = false;
        if (!s.mode.isEmpty()) s.reshow();
    }

    @Override
    protected void onServiceConnected() {
        running = this;
        handler.removeCallbacksAndMessages(null);
        handler.post(this::check);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    // null when the top screen can't be read right now.
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
        if (StreamActivity.isClient(pkg)) return "stream";
        if (pkg.equals(CocoonLaunch.COCOON) || pkg.equals(getPackageName()) || DUAL_SCREEN.contains(pkg)) return "";
        CocoonLaunch l = CocoonLaunch.latest();
        if (l == null || !l.player.equals(pkg)) return "";
        // Opening the same app later from somewhere else must not bring back an old launch's art.
        if (l.time < topSince - LAUNCH_WINDOW_MS || l.time > topSince + LAUNCH_WINDOW_MS) return "";
        out[0] = l;
        return "game:" + l.name;
    }

    private void check() {
        handler.postDelayed(this::check, CHECK_MS);
        // Window lists are unreliable while the screens are off, so leave everything as it is.
        if (!getSystemService(PowerManager.class).isInteractive()) return;
        String pkg = topPackage();
        if (pkg == null) return;
        long now = System.currentTimeMillis();
        if (!pkg.equals(topPkg)) {
            topPkg = pkg;
            topSince = now;
        }
        CocoonLaunch[] launch = new CocoonLaunch[1];
        String next = wanted(pkg, launch);
        if (!next.isEmpty()) {
            goneSince = 0;
            if (!next.equals(mode)) {
                mode = next;
                show(launch[0]);
            }
        } else if (!mode.isEmpty() || splash != null) {
            if (goneSince == 0) goneSince = now;
            if (now - goneSince >= HIDE_AFTER_MS) {
                mode = "";
                goneSince = 0;
                hide();
            }
        }
    }

    private void show(CocoonLaunch l) {
        shown = l;
        if (editing) return;
        if (splash == null && !attach()) return;
        if (l == null) splash.showStream();
        else splash.showGame(l);
    }

    private void reshow() {
        show(shown);
    }

    private boolean attach() {
        Display bottom = Screens.secondary(this);
        if (bottom == null) return false;
        Context ctx = createDisplayContext(bottom)
                .createWindowContext(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, null);
        wm = ctx.getSystemService(WindowManager.class);
        splash = new Splash(ctx);
        splash.web.setOnLongClickListener(v -> {
            editAddress();
            return true;
        });
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE);
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        try {
            wm.addView(splash.web, lp);
        } catch (RuntimeException e) {
            splash.destroy();
            splash = null;
            return false;
        }
        return true;
    }

    // The overlay sits above every activity, so it steps aside while the address dialog is open.
    private void editAddress() {
        editing = true;
        hide();
        Display bottom = Screens.secondary(this);
        Intent i = new Intent(this, MainActivity.class);
        Screens.launchOn(this, i, bottom == null ? Display.DEFAULT_DISPLAY : bottom.getDisplayId());
    }

    private void hide() {
        if (splash == null) return;
        try {
            wm.removeViewImmediate(splash.web);
        } catch (RuntimeException ignored) {
            // The display went away and took the window with it.
        }
        splash.destroy();
        splash = null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (running == this) running = null;
        handler.removeCallbacksAndMessages(null);
        hide();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (running == this) running = null;
        handler.removeCallbacksAndMessages(null);
        hide();
        super.onDestroy();
    }
}
