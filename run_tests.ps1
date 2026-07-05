# Setup script to bootstrap Gradle and run tests
# This script downloads Gradle if needed and runs the unit tests

param(
    [string]$Action = "test"
)

$ProjectDir = Get-Location
$GradleVersion = "8.4"
$GradleHome = "$env:USERPROFILE\.gradle"
$GragglesDir = "$GradleHome\gradle-$GradleVersion"

Write-Host "ShoppiList Android Project - Test Setup" -ForegroundColor Cyan
Write-Host "Current directory: $ProjectDir"
Write-Host ""

# Create necessary directories
New-Item -ItemType Directory -Path $GradleHome -Force | Out-Null

# Download Gradle if not present
if (-not (Test-Path "$GragglesDir\bin\gradle.bat")) {
    Write-Host "Downloading Gradle $GradleVersion..." -ForegroundColor Yellow
    $GradleZip = "$env:TEMP\gradle-$GradleVersion-bin.zip"

    # Use curl if available, otherwise PowerShell
    try {
        Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" `
            -OutFile $GradleZip `
            -ErrorAction Stop

        Write-Host "Extracting Gradle..." -ForegroundColor Yellow
        Expand-Archive -Path $GradleZip -DestinationPath $GradleHome -Force
        Remove-Item $GradleZip
        Write-Host "Gradle installed successfully" -ForegroundColor Green
    } catch {
        Write-Host "Failed to download Gradle. Please install manually." -ForegroundColor Red
        exit 1
    }
}

# Add Gradle to PATH
$env:PATH = "$GragglesDir\bin;$env:PATH"

Write-Host ""
Write-Host "Running tests..." -ForegroundColor Cyan
Write-Host ""

# Run tests
switch ($Action) {
    "test" {
        & gradle test --info
    }
    "build" {
        & gradle build
    }
    "clean" {
        & gradle clean
    }
    default {
        & gradle test
    }
}

