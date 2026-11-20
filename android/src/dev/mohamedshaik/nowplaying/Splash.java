package dev.mohamedshaik.nowplaying;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

// The bottom-screen page: the PC's now-playing page while streaming, or art for a game Cocoon launched.
final class Splash {
    static final String DEFAULT_URL = "http://192.168.1.237:8787/";
    private static final long RETRY_MS = 2000;
    private static final String CDN = "https://cdn.cloudflare.steamstatic.com/steam/apps/";

    final WebView web;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences prefs;
    private boolean destroyed;

    Splash(Context ctx) {
        prefs = prefs(ctx);
        web = new WebView(ctx);
        web.setBackgroundColor(Color.BLACK);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame() && !destroyed) showStream();
            }
        });
    }

    static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    static String url(Context ctx) {
        return prefs(ctx).getString("url", DEFAULT_URL);
    }

    // Shows the waiting screen and switches to the PC's page once its server answers.
    void showStream() {
        handler.removeCallbacksAndMessages(null);
        web.loadDataWithBaseURL(null, waitingPage(), "text/html", "utf-8", null);
        probe();
    }

    void showGame(CocoonLaunch l) {
        handler.removeCallbacksAndMessages(null);
        String steam = l.steamAppId();
        String hero = fileUrl(l.media("hero"));
        String logo = fileUrl(l.media("logo"));
        if (steam != null) {
            if (hero == null) hero = CDN + steam + "/library_hero.jpg";
            if (logo == null) logo = CDN + steam + "/logo.png";
        }
        String html = GamePage.render(l.name, hero, logo, System.currentTimeMillis());
        web.loadDataWithBaseURL("file:///", html, "text/html", "utf-8", null);
    }

    void destroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        web.destroy();
    }

    private static String fileUrl(File f) {
        return f == null ? null : Uri.fromFile(f).toString();
    }

    private String url() {
        return prefs.getString("url", DEFAULT_URL);
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
                if (destroyed) return;
                if (ok) web.loadUrl(target);
                else handler.postDelayed(this::probe, RETRY_MS);
            });
        }).start();
    }

    private String waitingPage() {
        return "<html><body style='margin:0;background:#000;color:#fff;display:flex;flex-direction:column;"
                + "align-items:center;justify-content:center;height:100vh;font-family:sans-serif;gap:2vh'>"
                + "<div style='font-size:7vw;font-weight:700'>Waiting for stream</div>"
                + "<div style='font-size:3.2vw;color:#777'>The art appears once Moonlight connects</div>"
                + "<div style='font-size:2.6vw;color:#444;margin-top:4vh'>" + url() + " · long-press to change</div>"
                + "</body></html>";
    }
}
