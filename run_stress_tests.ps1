# Phill Offline Stress Test Automation Script
# Sets environment paths, runs JVM unit tests, check devices, and executes Android instrumented tests.

$env:JAVA_HOME = "D:\AndroidDev\AndroidStudio\jbr"
$env:PATH = "D:\AndroidDev\SDK\platform-tools;" + $env:PATH

Write-Output "======================================================"
Write-Output "   Starting Phill Offline Stress Test Battery"
Write-Output "======================================================"
Write-Output ""

# Phase 1: Local JVM Headless Stress Tests (No Emulator Required)
Write-Output "[PHASE 1/2] Running JVM Local Unit Stress Tests..."
Write-Output "  -> Running BillingEngineStressTest on Host JVM..."

$jvmResult = .\gradlew.bat testDebugUnitTest --tests "com.phillips.phill.BillingEngineStressTest" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Output "  [SUCCESS] Phase 1: JVM stress tests completed successfully!"
} else {
    Write-Output "  [FAILED] Phase 1: JVM stress tests failed!"
    Write-Output $jvmResult
    Exit 1
}

Write-Output ""

# Phase 2: Android Instrumented Stress Tests (Requires Connected Emulator/Device)
Write-Output "[PHASE 2/2] Checking for connected devices/emulators..."
$devices = & D:\AndroidDev\SDK\platform-tools\adb.exe devices

$runningDevice = $devices | Select-String "device$"

if (-not $runningDevice) {
    Write-Output "  [WARNING] No active Android emulator or device was detected."
    Write-Output "  Skipping Instrumented Concurrency, ViewModel, and UI Stress Tests."
    Write-Output "  (To run Phase 2, please start your emulator first and re-run this script)"
    Write-Output ""
    Write-Output "=== Test Run Summary ==="
    Write-Output "JVM Stress Tests:   PASS"
    Write-Output "Android UI Tests:   SKIPPED (No Emulator)"
    Write-Output "========================"
    Exit 0
}

Write-Output "  Found active device: $runningDevice"
Write-Output "  -> Launching System, ViewModel, and UI Compose Monkey Stress Tests..."

$androidTestResult = .\gradlew.bat connectedDebugAndroidTest 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Output "  [SUCCESS] Phase 2: Android instrumented stress tests completed successfully!"
    Write-Output ""
    Write-Output "=== Test Run Summary ==="
    Write-Output "JVM Stress Tests:   PASS"
    Write-Output "Android UI Tests:   PASS"
    Write-Output "========================"
} else {
    Write-Output "  [FAILED] Phase 2: Android instrumented stress tests failed!"
    Write-Output $androidTestResult
    Exit 1
}
