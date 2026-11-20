package dev.mohamedshaik.nowplaying;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;

// Edits the PC address. Opened by long-pressing the bottom-screen page.
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EditText input = new EditText(this);
        input.setText(Splash.url(this));
        new AlertDialog.Builder(this)
                .setTitle("PC address")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String u = input.getText().toString().trim();
                    if (!u.endsWith("/")) u += "/";
                    Splash.prefs(this).edit().putString("url", u).apply();
                })
                .setNegativeButton("Cancel", null)
                .setOnDismissListener(d -> finish())
                .show();
    }

    @Override
    protected void onDestroy() {
        WatcherService.editorClosed();
        super.onDestroy();
    }
}
