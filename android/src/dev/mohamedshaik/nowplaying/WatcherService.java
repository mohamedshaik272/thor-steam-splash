package dev.mohamedshaik.nowplaying;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.lang.ref.WeakReference;
import java.util.List;

// Opens the viewer on the bottom screen while a streaming client is on the top screen, and closes it after.
public class WatcherService extends AccessibilityService {
    private static final long CHECK_MS = 1500;
    private static final long CLOSE_AFTER_MS = 5000;

    static WeakReference<Activity> viewer = new WeakReference<>(null);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean active;
    private long goneSince;

    @Override
    protected void onServiceConnected() {
        handler.post(this::check);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    private boolean clientOnTop() {
        SparseArray<List<AccessibilityWindowInfo>> all = getWindowsOnAllDisplays();
        List<AccessibilityWindowInfo> top = all.get(Display.DEFAULT_DISPLAY);
        if (top == null) return false;
        for (AccessibilityWindowInfo w : top) {
            if (w.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root = w.getRoot();
            if (root == null) return false;
            CharSequence pkg = root.getPackageName();
            return pkg != null && StreamActivity.isClient(pkg.toString());
        }
        return false;
    }

    private void check() {
        boolean onTop = clientOnTop();
        long now = System.currentTimeMillis();
        if (onTop) {
            goneSince = 0;
            if (!active) {
                active = true;
                Screens.openViewer(this);
            }
        } else if (active) {
            if (goneSince == 0) goneSince = now;
            if (now - goneSince >= CLOSE_AFTER_MS) {
                active = false;
                goneSince = 0;
                Activity a = viewer.get();
                if (a != null) a.finishAndRemoveTask();
            }
        }
        handler.postDelayed(this::check, CHECK_MS);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
