package dev.mohamedshaik.nowplaying;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.view.Display;

final class Screens {
    private Screens() {}

    static Display secondary(Context ctx) {
        DisplayManager dm = ctx.getSystemService(DisplayManager.class);
        for (Display d : dm.getDisplays()) {
            if (d.getDisplayId() != Display.DEFAULT_DISPLAY) return d;
        }
        return null;
    }

    // Brings an existing task for the intent to the front if there is one, otherwise starts it on displayId.
    static void launchOn(Context ctx, Intent intent, int displayId) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityOptions opts = ActivityOptions.makeBasic().setLaunchDisplayId(displayId);
        ctx.startActivity(intent, opts.toBundle());
    }

    // Relaunches the viewer on the bottom screen if it was opened on the top one.
    static boolean moveToSecondary(Activity a) {
        Display here = a.getDisplay();
        Display target = secondary(a);
        if (target == null || here == null || here.getDisplayId() == target.getDisplayId()) return false;
        // The viewer's own task is the one on the wrong screen, so force a new one. The caller finishes the old one.
        Intent intent = new Intent(a, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        launchOn(a, intent, target.getDisplayId());
        return true;
    }
}
