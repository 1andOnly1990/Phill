$env:PATH = "D:\AndroidDev\SDK\platform-tools;" + $env:PATH
$authToken = (Get-Content "$env:USERPROFILE\.emulator_console_auth_token" -ErrorAction SilentlyContinue).Trim()

Write-Output "=== Google Caller ID Simulation Test ==="
Write-Output ""

# Step 1: Simulate incoming call via emulator console
try {
    $tcp = New-Object System.Net.Sockets.TcpClient("localhost", 5554)
    $s = $tcp.GetStream()
    $w = New-Object System.IO.StreamWriter($s)
    $w.AutoFlush = $true
    Start-Sleep -Milliseconds 500
    # Drain greeting
    while($s.DataAvailable) { [void]$s.ReadByte() }
    $w.WriteLine("auth $authToken")
    Start-Sleep -Milliseconds 500
    while($s.DataAvailable) { [void]$s.ReadByte() }
    
    Write-Output "[1/4] Sending incoming call from 800-555-1234 (simulated business)..."
    $w.WriteLine("gsm call 8005551234")
    Start-Sleep -Seconds 3
    while($s.DataAvailable) { [void]$s.ReadByte() }
    Write-Output "  -> Call ringing on emulator"

    Write-Output ""
    Write-Output "[2/4] Checking call notification from Google Dialer..."
    $notifDump = adb shell dumpsys notification --noredact 2>&1
    $callNotifs = $notifDump | Select-String "dialer|IncomingCall|phone_account|caller" 
    if ($callNotifs) {
        $callNotifs | ForEach-Object { Write-Output "  NOTIF: $_" }
    } else {
        Write-Output "  (No matching call notifications found in dump)"
    }

    # Cancel the call
    Write-Output ""
    Write-Output "[3/4] Cancelling simulated call..."
    $w.WriteLine("gsm cancel 8005551234")
    Start-Sleep -Seconds 1
    while($s.DataAvailable) { [void]$s.ReadByte() }

    $w.WriteLine("quit")
    $tcp.Close()
    Write-Output "  -> Call ended"
} catch {
    Write-Output "  ERROR with emulator console: $_"
}

# Step 2: Now simulate a Google Messages RCS notification (mimicking verified sender)
Write-Output ""
Write-Output "[4/4] Simulating Google Messages notification (RCS with caller ID)..."
Write-Output "  Posting notification mimicking Google Messages verified business message..."

# Post a notification that looks like it came from Google Messages
# This tests whether RcsNotificationListener intercepts it
adb shell "cmd notification post -S messaging -t 'Phillips Auto Service' --style messaging --conversation 'Phillips Auto Service' 'Phillips Auto Service' 'Your vehicle service is complete. Please call 864-555-0199 to schedule pickup.' test_rcs_notif" 2>&1

Start-Sleep -Seconds 3

Write-Output ""
Write-Output "=== Checking Phill app state ==="
$phillPid = adb shell pidof com.phillips.phill 2>&1
Write-Output "Phill PID: $phillPid"

Write-Output ""
Write-Output "=== Checking logcat for RCS/caller processing ==="
adb logcat -d -t 100 | Select-String "RcsNotification|CallerID|phill|FATAL|ContactResolver" | Select-Object -First 20

Write-Output ""
Write-Output "=== Test Complete ==="
