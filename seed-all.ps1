# ============================================================
#  seed-all.ps1  -  ProductionDataSeeder automation script
#  Chạy: powershell -ExecutionPolicy Bypass -File seed-all.ps1
#
#  Nạp dữ liệu thật vào MongoDB Atlas theo 2 phase:
#    Phase 1: collections độc lập (categories → users → shops → ...)
#    Phase 2: collections phụ thuộc (orders → reviews → ...)
#
#  Mỗi batch = 100 bản ghi. Tổng ~28.000 bản ghi / 320 batch.
#  Thời gian ước tính: ~30-60 phút tuỳ tốc độ mạng tới Atlas.
# ============================================================

param(
    [switch]$SkipPhase1 = $false,
    [switch]$SkipPhase2 = $false,
    [int]$PauseSeconds = 2,
    [switch]$WipeAll = $false
)

$ErrorActionPreference = "Continue"
$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not (Test-Path "$ProjectDir\pom.xml")) {
    $ProjectDir = "d:\Ecommerce_Java"
}
Set-Location $ProjectDir
Write-Host "Working dir: $ProjectDir" -ForegroundColor Cyan

# ----- cấu hình batch -----
$Phase1Targets = @(
    @{ Name = "categories";    Batches = 1  }
    @{ Name = "users";         Batches = 15 }
    @{ Name = "shops";         Batches = 3  }
    @{ Name = "products";      Batches = 20 }
    @{ Name = "vouchers";      Batches = 2  }
    @{ Name = "banners";       Batches = 1  }
    @{ Name = "legal";         Batches = 1  }
)

$Phase2Targets = @(
    @{ Name = "orders";        Batches = 50 }
    @{ Name = "reviews";       Batches = 30 }
    @{ Name = "carts";         Batches = 15 }
    @{ Name = "kyc";           Batches = 3  }
    @{ Name = "complaints";    Batches = 15 }
    @{ Name = "returns";       Batches = 10 }
    @{ Name = "refunds";       Batches = 10 }
    @{ Name = "reportcases";  Batches = 15 }
    @{ Name = "moderation";   Batches = 20 }
    @{ Name = "violations";   Batches = 3  }
    @{ Name = "escalations";  Batches = 3  }
    @{ Name = "auditlogs";    Batches = 50 }
    @{ Name = "auditentries"; Batches = 50 }
    @{ Name = "scheduler";    Batches = 2  }
)

# ----- helpers -----
function Run-Batch($target, $batch, [switch]$Wipe) {
    $seedArgs = @(
        '-Dspring-boot.run.profiles=seed'
        '-Dspring-boot.run.arguments=--spring.main.web-application-type=none --app.seed.enabled=true --seed.target=' + $target + ' --seed.batch=' + $batch
    )
    Write-Host ""
    Write-Host ("[seed] target={0,-14} batch={1,-3}" -f $target, $batch) -ForegroundColor Green
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $logFile = "$ProjectDir\seed-run.log"
    & mvn spring-boot:run @seedArgs *> $logFile
    $code = $LASTEXITCODE
    $sw.Stop()
    if ($code -ne 0) {
        Write-Host ("  FAILED in {0:N0}s (exit={1})" -f $sw.Elapsed.TotalSeconds, $code) -ForegroundColor Red
        return $false
    }
    Write-Host ("  OK in {0:N0}s" -f $sw.Elapsed.TotalSeconds) -ForegroundColor DarkGreen
    return $true
}

# ----- main loop -----
$results = @()
$totalOk = 0
$totalFail = 0
$globalSw = [System.Diagnostics.Stopwatch]::StartNew()

function Invoke-Phase($phaseName, $targets) {
    Write-Host ""
    Write-Host "================ PHASE $phaseName ================" -ForegroundColor Magenta
    foreach ($t in $targets) {
        for ($b = 0; $b -lt $t.Batches; $b++) {
            if (Run-Batch -target $t.Name -batch $b) {
                $totalOk++
                $script:results += [PSCustomObject]@{ Phase = $phaseName; Target = $t.Name; Batch = $b; Status = "OK" }
            } else {
                $totalFail++
                $script:results += [PSCustomObject]@{ Phase = $phaseName; Target = $t.Name; Batch = $b; Status = "FAIL" }
                # dừng ngay nếu fail (tránh lỗi cascade FK)
                Write-Host "  Halting — fix lỗi rồi chạy lại." -ForegroundColor Red
                return $false
            }
            Start-Sleep -Seconds $PauseSeconds
        }
    }
    return $true
}

if (-not $SkipPhase1) {
    if (-not (Invoke-Phase "1" $Phase1Targets)) { $globalSw.Stop(); break }
}
if (-not $SkipPhase2) {
    if (-not (Invoke-Phase "2" $Phase2Targets)) { $globalSw.Stop(); break }
}

$globalSw.Stop()
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ("DONE in {0:N0}s   OK={1}   FAIL={2}" -f $globalSw.Elapsed.TotalSeconds, $totalOk, $totalFail) -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
$results | Group-Object Phase, Target | Select-Object Name, Count | Format-Table -AutoSize

# ----- final: in DB stats -----
Write-Host ""
Write-Host "================ DB STATS SAU KHI SEED ================" -ForegroundColor Magenta
$env:MONGODB_URI = "mongodb+srv://nguyenquocanh170205_db_user:FJGAwn7TMMPkcslN@cluster0.2pu37z6.mongodb.net"
& mvn '-q' 'exec:java' '-Dexec.mainClass=com.ecommerce.cnj70.config.DbStats' '-Dexec.classpathScope=runtime' 2>&1 |
    Where-Object { $_ -match "^[A-Za-z].*=\s+\d+|TOTAL|Database" } |
    Select-Object -First 25
