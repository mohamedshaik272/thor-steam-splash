package dev.mohamedshaik.nowplaying;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String DEFAULT_URL = "http://192.168.1.237:8787/";
    private static final long RETRY_MS = 2000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView web;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Screens.moveToSecondary(this)) {
            finish();
            return;
        }
        WatcherService.viewer = new java.lang.ref.WeakReference<>(this);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        web.getSettings().setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) load();
            }
        });
        web.setOnLongClickListener(v -> {
            editUrl();
            return true;
        });
        setContentView(web);
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        WindowInsetsController c = getWindow().getInsetsController();
        if (c != null) {
            c.hide(WindowInsets.Type.systemBars());
            c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private String url() {
        return prefs.getString("url", DEFAULT_URL);
    }

    // Shows the waiting screen and switches to the page once the server answers.
    private void load() {
        handler.removeCallbacksAndMessages(null);
        web.loadDataWithBaseURL(null, offlinePage(), "text/html", "utf-8", null);
        probe();
    }

    private void probe() {
        String target = url();
        new Thread(() -> {
            boolean up = false;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(target + "api/now").openConnection();
                c.setConnectTimeout(1500);
                c.setReadTimeout(1500);
                up = c.getResponseCode() == 200;
                c.disconnect();
            } catch (IOException ignored) {
            }
            boolean ok = up;
            handler.post(() -> {
                if (isDestroyed()) return;
                if (ok) web.loadUrl(target);
                else handler.postDelayed(this::probe, RETRY_MS);
            });
        }).start();
    }

    private String offlinePage() {
        return "<html><body style='margin:0;background:#000;color:#fff;display:flex;flex-direction:column;"
                + "align-items:center;justify-content:center;height:100vh;font-family:sans-serif;gap:2vh'>"
                + "<div style='font-size:7vw;font-weight:700'>Waiting for stream</div>"
                + "<div style='font-size:3.2vw;color:#777'>The art appears once Moonlight connects</div>"
                + "<div style='font-size:2.6vw;color:#444;margin-top:4vh'>" + url() + " \u00b7 long-press to change</div>"
                + "</body></html>";
    }

    private void editUrl() {
        EditText input = new EditText(this);
        input.setText(url());
        new AlertDialog.Builder(this)
                .setTitle("PC address")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String u = input.getText().toString().trim();
                    if (!u.endsWith("/")) u += "/";
                    prefs.edit().putString("url", u).apply();
                    load();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (WatcherService.viewer.get() == this) WatcherService.viewer.clear();
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
