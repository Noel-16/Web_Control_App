# Java Setup Script for Web Control App
# Run this as Administrator to set up Java environment

Write-Host "=== Java Setup for Web Control App ===" -ForegroundColor Green
Write-Host ""

# Step 1: Find Java installation
Write-Host "Searching for Java installations..." -ForegroundColor Yellow

$javaInstalls = Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue

if ($javaInstalls.Count -eq 0) {
    Write-Host "ERROR: Java not found in C:\Program Files\Java" -ForegroundColor Red
    Write-Host "Please install Java JDK 17+ first from: https://www.oracle.com/java/technologies/downloads/" -ForegroundColor Red
    exit 1
}

# Step 2: Select the latest Java version
$latestJava = $javaInstalls | Sort-Object -Property Name -Descending | Select-Object -First 1
$javaHome = $latestJava.FullName

Write-Host "Found Java at: $javaHome" -ForegroundColor Green

# Step 3: Verify java executable exists
$javaExe = Join-Path $javaHome "bin\java.exe"
if (-not (Test-Path $javaExe)) {
    Write-Host "ERROR: Java executable not found at $javaExe" -ForegroundColor Red
    exit 1
}

# Step 4: Set JAVA_HOME environment variable (Machine-wide)
Write-Host "Setting JAVA_HOME environment variable..." -ForegroundColor Yellow

try {
    # Set for current session
    $env:JAVA_HOME = $javaHome
    
    # Set permanently for all future sessions (requires admin)
    [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "Machine")
    Write-Host "✓ JAVA_HOME set to: $javaHome" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to set JAVA_HOME. Run this script as Administrator." -ForegroundColor Red
    exit 1
}

# Step 5: Verify Java works
Write-Host "Verifying Java installation..." -ForegroundColor Yellow
& $javaExe -version 2>&1 | ForEach-Object { Write-Host $_ }

Write-Host ""
Write-Host "✓ Java setup complete!" -ForegroundColor Green
Write-Host "You can now build the app with: .\gradlew installDebug" -ForegroundColor Green
