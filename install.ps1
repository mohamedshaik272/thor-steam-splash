# Starts the splash server at logon and opens its port on private networks. Run from an elevated PowerShell.
$ErrorActionPreference = "Stop"
$name = "Thor Steam Splash"
$port = 8787
$dir = $PSScriptRoot
$pythonw = (Get-Command pythonw.exe -ErrorAction Stop).Source

Get-NetFirewallRule -DisplayName $name -ErrorAction SilentlyContinue | Remove-NetFirewallRule
New-NetFirewallRule -DisplayName $name -Direction Inbound -Protocol TCP -LocalPort $port -Profile Private -Action Allow | Out-Null

$action = New-ScheduledTaskAction -Execute $pythonw -Argument "`"$dir\server.py`"" -WorkingDirectory $dir
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
Register-ScheduledTask -TaskName $name -Action $action -Trigger $trigger -Settings $settings -Force | Out-Null
Start-ScheduledTask -TaskName $name

$ip = (Get-NetIPAddress -AddressFamily IPv4 -PrefixOrigin Dhcp, Manual |
    Where-Object { $_.IPAddress -notlike "169.*" } | Select-Object -First 1).IPAddress
Write-Host "Running. Open http://${ip}:$port on the Thor."
