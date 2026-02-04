# Set JAVA_HOME permanently for this user
$javaHome = "C:\Program Files\Android\Android Studio\jbr"

Write-Host "Setting JAVA_HOME to: $javaHome" -ForegroundColor Green

# Set for current session
$env:JAVA_HOME = $javaHome

# Set permanently for the user
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, [System.EnvironmentVariableTarget]::User)

Write-Host "JAVA_HOME has been set permanently for your user account." -ForegroundColor Green
Write-Host "Current value: $env:JAVA_HOME" -ForegroundColor Cyan

# Verify
Write-Host "`nVerifying Java installation..." -ForegroundColor Yellow
& "$javaHome\bin\java.exe" -version
