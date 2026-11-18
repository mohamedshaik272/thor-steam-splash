# Registers the splash server so Apollo/Sunshine starts it when a stream begins and stops it when the stream ends,
# and opens its port on private networks. Run from an elevated PowerShell.
$ErrorActionPreference = "Stop"
$name = "Thor Steam Splash"
$port = 8787
$dir = $PSScriptRoot
$pythonw = (Get-Command pythonw.exe -ErrorAction Stop).Source

Get-NetFirewallRule -DisplayName $name -ErrorAction SilentlyContinue | Remove-NetFirewallRule
New-NetFirewallRule -DisplayName $name -Direction Inbound -Protocol TCP -LocalPort $port -Profile Private -Action Allow | Out-Null

# No trigger: the streaming host runs this task on demand.
$action = New-ScheduledTaskAction -Execute $pythonw -Argument "`"$dir\server.py`"" -WorkingDirectory $dir
$principal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
Register-ScheduledTask -TaskName $name -Action $action -Principal $principal -Settings $settings -Force | Out-Null

# Add start/stop commands to the streaming host's global_prep_cmd, keeping any that are already there.
$hosts = @(
    @{ Conf = "$env:ProgramFiles\Apollo\config\sunshine.conf"; Service = "ApolloService" },
    @{ Conf = "$env:ProgramFiles\Sunshine\config\sunshine.conf"; Service = "SunshineService" }
)
$entry = [pscustomobject]@{
    do       = "schtasks /run /tn `"$name`""
    undo     = "schtasks /end /tn `"$name`""
    elevated = $false
}
$configured = $false
foreach ($h in $hosts) {
    if (-not (Test-Path $h.Conf)) { continue }
    $lines = [System.Collections.Generic.List[string]](Get-Content $h.Conf)
    $i = $lines.FindIndex({ param($l) $l -match '^\s*global_prep_cmd\s*=' })
    $cmds = @()
    # ForEach-Object unrolls the array; Windows PowerShell 5.1's ConvertFrom-Json returns it as one object.
    if ($i -ge 0) { $cmds = @(($lines[$i] -replace '^\s*global_prep_cmd\s*=\s*', '') | ConvertFrom-Json | ForEach-Object { $_ }) }
    if (-not ($cmds | Where-Object { $_.do -like "*$name*" })) {
        Copy-Item $h.Conf "$($h.Conf).bak" -Force
        $line = "global_prep_cmd = " + (ConvertTo-Json -InputObject @($cmds + $entry) -Compress)
        if ($i -ge 0) { $lines[$i] = $line } else { $lines.Add($line) }
        [IO.File]::WriteAllLines($h.Conf, $lines)
        Write-Host "Added start/stop commands to $($h.Conf)."
        if (Get-Service $h.Service -ErrorAction SilentlyContinue) {
            Restart-Service $h.Service -Force
            Write-Host "Restarted $($h.Service)."
        }
    } else {
        Write-Host "$($h.Conf) already starts and stops the splash server."
    }
    $configured = $true
}
if (-not $configured) {
    Write-Host "Apollo/Sunshine config not found. Add these under Configuration > General > Command Preparations:"
    Write-Host "  Do:   $($entry.do)"
    Write-Host "  Undo: $($entry.undo)"
}

$ip = (Get-NetIPAddress -AddressFamily IPv4 -PrefixOrigin Dhcp, Manual |
    Where-Object { $_.IPAddress -notlike "169.*" } | Select-Object -First 1).IPAddress
Write-Host "Installed. While streaming, open http://${ip}:$port on the Thor."
