package dev.mohamedshaik.nowplaying;

// Full-screen art page for a game running on the top screen, styled like the PC page.
final class GamePage {
    private GamePage() {}

    static String render(String name, String hero, String logo, long since) {
        String safe = name.replace("&", "&amp;").replace("<", "&lt;");
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>"
                + "*{box-sizing:border-box;margin:0}html,body{height:100%;background:#000;color:#fff;overflow:hidden;"
                + "font-family:system-ui,sans-serif}.l{position:fixed;inset:0}"
                + "#bg{background:center/cover no-repeat;transform:scale(1.06);filter:brightness(.55)}"
                + "#sh{background:linear-gradient(to top,rgba(0,0,0,.85),rgba(0,0,0,.1) 55%,rgba(0,0,0,.35))}"
                + "#g{display:flex;flex-direction:column;justify-content:flex-end;padding:6vh 6vw;gap:3vh}"
                + "#logo{max-width:78vw;max-height:34vh;object-fit:contain;object-position:left bottom;display:none;"
                + "filter:drop-shadow(0 6px 24px rgba(0,0,0,.6))}"
                + "#t{font-size:9vw;font-weight:800;line-height:1.05}"
                + ".m{display:flex;justify-content:space-between;font-size:4.2vw;color:rgba(255,255,255,.78)}"
                + "</style></head><body><div id='bg' class='l'></div><div id='sh' class='l'></div>"
                + "<div id='g' class='l'><img id='logo'><div id='t'>" + safe + "</div>"
                + "<div class='m'><span id='e'></span><span id='c'></span></div></div><script>"
                + "const since=" + since + ";"
                + "const hero=" + js(hero) + ",logo=" + js(logo) + ";"
                + "if(hero){const i=new Image();i.onload=()=>document.getElementById('bg').style.backgroundImage="
                + "'url(\"'+hero+'\")';i.src=hero;}"
                + "if(logo){const l=document.getElementById('logo');l.onload=()=>{l.style.display='block';"
                + "document.getElementById('t').style.display='none'};l.src=logo;}"
                + "function tick(){const m=Math.floor((Date.now()-since)/60000),h=Math.floor(m/60);"
                + "document.getElementById('e').textContent=(h?h+'h '+m%60+'m':m+'m')+' this session';"
                + "document.getElementById('c').textContent=new Date().toLocaleTimeString([],{hour:'numeric',minute:'2-digit'});}"
                + "tick();setInterval(tick,1000);</script></body></html>";
    }

    private static String js(String s) {
        if (s == null) return "null";
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
