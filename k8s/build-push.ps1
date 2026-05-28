# ==============================================================
# PHASE 2: Build tat ca Docker images va push len local registry
# ==============================================================
# Chay: .\k8s\build-push.ps1
# Them -Service <ten> de build lai mot service:
#   .\k8s\build-push.ps1 -Service saving-contract-service
# ==============================================================

param(
    [string]$Service = "",   # Neu truyen vao chi build service do
    [string]$Tag     = "latest"
)

Set-Location $PSScriptRoot\..
$REGISTRY = "localhost:5001"

$services = @(
    @{ name = "db";                          context = "db" },
    @{ name = "core-banking-mock";           context = "core-banking-mock" },
    @{ name = "auth-service";                context = "auth-service" },
    @{ name = "customer-service";            context = "customer-service" },
    @{ name = "account-service";             context = "account-service" },
    @{ name = "saving-product-service";      context = "saving-product-service" },
    @{ name = "saving-contract-service";     context = "saving-contract-service" },
    @{ name = "saving-transaction-service";  context = "saving-transaction-service" },
    @{ name = "saving-interest-service";     context = "saving-interest-service" },
    @{ name = "saving-lifecycle-service";    context = "saving-lifecycle-service" },
    @{ name = "saving-notification-service"; context = "saving-notification-service" },
    @{ name = "api-gateway";                 context = "api-gateway" },
    @{ name = "saving-banking-web";          context = "saving-banking-web" }
)

# Loc neu chi build 1 service
if ($Service -ne "") {
    $services = $services | Where-Object { $_.name -eq $Service }
    if ($services.Count -eq 0) {
        Write-Error "Khong tim thay service: $Service"
        exit 1
    }
}

$total   = $services.Count
$current = 0

foreach ($svc in $services) {
    $current++
    $image = "$REGISTRY/$($svc.name):$Tag"
    Write-Host "`n[$current/$total] Building $image ..." -ForegroundColor Cyan

    docker build -t $image "./$($svc.context)"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build that bai: $($svc.name)"
        exit 1
    }

    Write-Host "  Pushing $image ..." -ForegroundColor Yellow
    docker push $image
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Push that bai: $($svc.name)"
        exit 1
    }
    Write-Host "  -> OK: $image" -ForegroundColor Green
}

Write-Host "`n============================================" -ForegroundColor Green
Write-Host " Tat ca $total images da duoc push thanh cong!" -ForegroundColor Green
Write-Host " Registry: $REGISTRY" -ForegroundColor Green
Write-Host " Buoc tiep: .\k8s\deploy-all.ps1" -ForegroundColor Green
Write-Host "============================================`n" -ForegroundColor Green
