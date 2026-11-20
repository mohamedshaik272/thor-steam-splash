package dev.mohamedshaik.nowplaying;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Display;
import android.widget.Toast;

// Opens the streaming client on the top screen. WatcherService puts the page on the bottom screen.
public class StreamActivity extends Activity {
    // Artemis (the Moonlight fork for Apollo) first, then Moonlight. Keep in sync with <queries> in the manifest.
    private static final String[] CLIENTS = {"com.limelight.noir", "com.limelight"};

    static boolean isClient(String pkg) {
        for (String c : CLIENTS) {
            if (c.equals(pkg)) return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent client = null;
        for (String pkg : CLIENTS) {
            client = getPackageManager().getLaunchIntentForPackage(pkg);
            if (client != null) break;
        }
        if (client == null) {
            Toast.makeText(this, "Artemis or Moonlight isn't installed", Toast.LENGTH_LONG).show();
        } else {
            Screens.launchOn(this, client, Display.DEFAULT_DISPLAY);
            if (!WatcherService.isRunning()) {
                Toast.makeText(this, "Turn on Stream PC auto-open in Accessibility settings for the bottom screen",
                        Toast.LENGTH_LONG).show();
            }
        }
        finish();
    }
}
