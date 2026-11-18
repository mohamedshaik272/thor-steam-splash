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

public class MainActivity extends Activity {
    private static final String DEFAULT_URL = "http://192.168.1.237:8787/";
    private static final long RETRY_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView web;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        web.getSettings().setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    view.loadData(offlinePage(), "text/html", "utf-8");
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(MainActivity.this::load, RETRY_MS);
                }
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

    private void load() {
        web.loadUrl(url());
    }

    private String offlinePage() {
        return "<html><body style='margin:0;background:#000;color:#888;display:flex;"
                + "align-items:center;justify-content:center;height:100vh;font:5vw sans-serif'>"
                + "PC not reachable<br><small style='font-size:3vw'>" + url()
                + "<br>Long-press to change</small></body></html>";
    }

    private void editUrl() {
        EditText input = new EditText(this);
        input.setText(url());
        new AlertDialog.Builder(this)
                .setTitle("PC address")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    prefs.edit().putString("url", input.getText().toString().trim()).apply();
                    load();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        web.destroy();
        super.onDestroy();
    }
}
