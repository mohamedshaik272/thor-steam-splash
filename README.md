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

   That opens TCP 8787 on private networks, registers a scheduled task that starts the server at logon, starts it now, and prints the address to use.

To test without installing, run `python server.py` and open `http://localhost:8787`.

To remove it

```powershell
Unregister-ScheduledTask -TaskName "Thor Steam Splash" -Confirm:$false
Get-NetFirewallRule -DisplayName "Thor Steam Splash" | Remove-NetFirewallRule
```

## Thor setup

Install `now-playing.apk` from the latest release (or build it with `android/build.sh`). It's a full-screen viewer for the page, with no browser UI. Launch it on the bottom screen from its icon's Screen Launch option or with Wayfinder.

It loads `http://192.168.1.237:8787/` by default. Long-press anywhere to change the address. If the PC is off it shows "PC not reachable" and retries every 5 seconds.

## Notes

- Detection uses Steam's `RunningAppID` registry value, so only games launched through Steam are picked up.
- Some games have no `logo.png` on the CDN. The page falls back to the game's name.
- `SPLASH_PORT` changes the port. `SPLASH_FAKE_APPID` fakes a running game on non-Windows machines for testing.
