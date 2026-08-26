# ============================================
#   CNJ70 Ecommerce - Build & Run Script
# ============================================

$ProjectDir = $PSScriptRoot

Write-Host ""
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host "    CNJ70 Ecommerce Application" -ForegroundColor Cyan
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host ""

# Check Java installation
$javaCheck = & java -version 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Java is installed" -ForegroundColor Green
    $javaCheck | Select-Object -First 1
} else {
    Write-Host ""
    Write-Host "  [ERROR] Java is not installed or not in PATH." -ForegroundColor Red
    Write-Host "  Please install Java 17 or higher." -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""

# Check Maven installation
$mvnCheck = & mvn -version 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Maven is installed" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "  [ERROR] Maven is not installed or not in PATH." -ForegroundColor Red
    Write-Host "  Please install Maven." -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "  [INFO] Project directory: $ProjectDir" -ForegroundColor Cyan
Write-Host ""

# Navigate to project directory
Set-Location -Path $ProjectDir

# ============================================
#   Step 1: Clean and Build
# ============================================
$Host.UI.RawUI.WindowTitle = "CNJ70 Ecommerce - Building..."

Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host "   Step 1/2: Cleaning and Building..." -ForegroundColor Cyan
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host ""

mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "  [ERROR] Build failed!" -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "  [OK] Build completed successfully!" -ForegroundColor Green
Write-Host ""

# ============================================
#   Step 2: Run Application
# ============================================
$Host.UI.RawUI.WindowTitle = "CNJ70 Ecommerce - Running..."

Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host "   Step 2/2: Starting Spring Boot..." -ForegroundColor Cyan
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  ----------------------------------------" -ForegroundColor DarkGray
Write-Host "   Application Info:" -ForegroundColor White
Write-Host "   - URL:       http://localhost:8081" -ForegroundColor Cyan
Write-Host "   - API:       http://localhost:8081/api" -ForegroundColor Cyan
Write-Host "  ----------------------------------------" -ForegroundColor DarkGray
Write-Host ""

# Run the application
& java -jar "$ProjectDir\target\cnj70-ecommerce-1.0.0.jar"

# If application exits
Write-Host ""
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host "   Application has stopped." -ForegroundColor Yellow
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Possible issues:" -ForegroundColor White
Write-Host "   1. MongoDB Atlas IP whitelist not configured" -ForegroundColor Yellow
Write-Host "   2. Network/firewall blocking connection" -ForegroundColor Yellow
Write-Host "   3. MongoDB Atlas cluster is paused" -ForegroundColor Yellow
Write-Host ""

Read-Host "Press Enter to exit"
