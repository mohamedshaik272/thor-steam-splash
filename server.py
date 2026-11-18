"""Serves the running Steam game's artwork to a second screen. See README.md."""

import json
import os
import re
import sys
import time
import urllib.request
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

PORT = int(os.environ.get("SPLASH_PORT", "8787"))
STATIC = Path(__file__).resolve().parent / "static"
CDN = "https://cdn.cloudflare.steamstatic.com/steam/apps"

_names: dict[int, str] = {}
_current = {"appid": 0, "since": 0.0}


def _steam_key(value: str):
    import winreg

    with winreg.OpenKey(winreg.HKEY_CURRENT_USER, r"Software\Valve\Steam") as key:
        return winreg.QueryValueEx(key, value)[0]


def running_appid() -> int:
    if sys.platform != "win32":
        return int(os.environ.get("SPLASH_FAKE_APPID", "0"))
    try:
        return int(_steam_key("RunningAppID"))
    except OSError:
        return 0


def _library_dirs() -> list[Path]:
    try:
        root = Path(_steam_key("SteamPath"))
    except (OSError, ImportError):
        return []
    dirs = [root / "steamapps"]
    vdf = root / "steamapps" / "libraryfolders.vdf"
    if vdf.exists():
        for path in re.findall(r'"path"\s+"([^"]+)"', vdf.read_text(errors="ignore")):
            dirs.append(Path(path.replace("\\\\", "\\")) / "steamapps")
    return dirs


def _local_name(appid: int) -> str | None:
    for lib in _library_dirs():
        acf = lib / f"appmanifest_{appid}.acf"
        if acf.exists():
            match = re.search(r'"name"\s+"([^"]+)"', acf.read_text(errors="ignore"))
            if match:
                return match.group(1)
    return None


def _store_name(appid: int) -> str | None:
    url = f"https://store.steampowered.com/api/appdetails?appids={appid}&filters=basic"
    try:
        with urllib.request.urlopen(url, timeout=5) as resp:
            data = json.load(resp)[str(appid)]
        return data["data"]["name"] if data.get("success") else None
    except (OSError, ValueError, KeyError):
        return None


def game_name(appid: int) -> str:
    if appid not in _names:
        name = _local_name(appid) or _store_name(appid)
        if name is None:
            return f"App {appid}"
        _names[appid] = name
    return _names[appid]


def now_playing() -> dict:
    appid = running_appid()
    if appid != _current["appid"]:
        _current.update(appid=appid, since=time.time())
    if not appid:
        return {"appid": 0}
    base = f"{CDN}/{appid}"
    return {
        "appid": appid,
        "name": game_name(appid),
        "since": _current["since"],
        "hero": f"{base}/library_hero.jpg",
        "logo": f"{base}/logo.png",
        "header": f"{base}/header.jpg",
    }


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(STATIC), **kwargs)

    def do_GET(self):
        if self.path.split("?")[0] == "/api/now":
            body = json.dumps(now_playing()).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        super().do_GET()

    def log_message(self, format, *args):
        pass


def main():
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Serving on port {PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
