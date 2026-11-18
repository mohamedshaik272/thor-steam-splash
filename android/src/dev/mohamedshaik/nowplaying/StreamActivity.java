package dev.mohamedshaik.nowplaying;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Display;
import android.widget.Toast;

// Opens Moonlight on the top screen and the viewer on the bottom screen.
public class StreamActivity extends Activity {
    private static final String MOONLIGHT = "com.limelight";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent moonlight = getPackageManager().getLaunchIntentForPackage(MOONLIGHT);
        if (moonlight == null) {
            Toast.makeText(this, "Moonlight isn't installed", Toast.LENGTH_LONG).show();
        } else {
            Screens.launchOn(this, moonlight, Display.DEFAULT_DISPLAY);
        }
        Display bottom = Screens.secondary(this);
        if (bottom != null) {
            Screens.launchOn(this, new Intent(this, MainActivity.class), bottom.getDisplayId());
        }
        finish();
    }
}
