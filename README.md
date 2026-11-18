# thor-steam-splash

Shows the Steam game you're playing on a second screen, such as the AYN Thor's bottom screen while you stream with Moonlight. A small server on the Windows PC reads which game Steam is running and serves a full-screen page with that game's artwork, logo, session time and a clock. With nothing running it shows a clock.

Python standard library only, no API keys. Artwork is loaded from Steam's CDN.

## PC setup

1. Install Python 3.10+ from python.org or `winget install Python.Python.3.12`.
2. Make sure your Wi-Fi network is set to **Private** in Windows (Settings > Network > Wi-Fi > your network).
3. Clone or download this repo, then in an elevated PowerShell run

   ```powershell
   Set-ExecutionPolicy -Scope Process Bypass
   .\install.ps1
   ```

   That opens TCP 8787 on private networks, registers a scheduled task for the server, and prints the address to use. The server only runs while you're streaming: the script adds start and stop commands to Apollo's or Sunshine's `global_prep_cmd` (keeping any you already have, with a `.bak` copy of the config) and restarts the service. If neither is installed in Program Files, it prints the commands to add under Configuration > General > Command Preparations.

To test without installing, run `python server.py` and open `http://localhost:8787`.

To remove it, delete the Thor Steam Splash entry from Command Preparations in Apollo/Sunshine, then run

```powershell
Unregister-ScheduledTask -TaskName "Thor Steam Splash" -Confirm:$false
Get-NetFirewallRule -DisplayName "Thor Steam Splash" | Remove-NetFirewallRule
```

## Thor setup

Install `now-playing.apk` from the latest release (or build it with `android/build.sh`). Its launcher icon is **Stream PC**, which opens Artemis (or Moonlight, if Artemis isn't installed) on the top screen and the full-screen viewer on the bottom screen. Tapping it again brings both back instead of opening new copies. The viewer always moves itself to the bottom screen, wherever it's opened.

It loads `http://192.168.1.237:8787/` by default. Long-press anywhere to change the address. Until a stream starts, or if the PC is off, it shows "Waiting for stream" and checks again every 2 seconds.

### Auto-open and auto-close

The app includes an accessibility service, **Stream PC auto-open**. When it's on, the viewer opens on the bottom screen whenever Moonlight or Artemis is on the top screen, however you opened it, and closes about 5 seconds after you leave. It only checks which app is on the top screen. Turn it on in Settings > Accessibility > Stream PC auto-open.

## Notes

- Detection uses Steam's `RunningAppID` registry value, so only games launched through Steam are picked up.
- Some games have no `logo.png` on the CDN. The page falls back to the game's name.
- `SPLASH_PORT` changes the port. `SPLASH_FAKE_APPID` fakes a running game on non-Windows machines for testing.
