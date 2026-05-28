# ==============================================================
# TEARDOWN: Don dep moi truong K8s
# ==============================================================
# Chay: .\k8s\teardown.ps1
#
# Cac option:
#   -AppsOnly       Xoa chi apps (giu nguyen infra postgres/rabbitmq)
#   -KeepPVC        Giu nguyen PersistentVolumeClaims (du lieu DB)
#   -DeleteCluster  Xoa luon Kind cluster (xoa toan bo)
# ==============================================================

param(
    [switch]$AppsOnly,
    [switch]$KeepPVC,
    [switch]$DeleteCluster
)

Set-Location $PSScriptRoot\..
$Namespace = "saving-banking"

# ==============================================================
# Option A: Chi xoa apps, giu infra
# ==============================================================
if ($AppsOnly) {
    Write-Host "`n[AppsOnly] Xoa tat ca app Deployments..." -ForegroundColor Yellow

    $apps = @(
        "k8s/apps/ingress.yaml",
        "k8s/apps/api-gateway.yaml",
        "k8s/apps/saving-notification-service.yaml",
        "k8s/apps/saving-lifecycle-service.yaml",
        "k8s/apps/saving-interest-service.yaml",
        "k8s/apps/saving-transaction-service.yaml",
        "k8s/apps/saving-contract-service.yaml",
        "k8s/apps/saving-product-service.yaml",
        "k8s/apps/account-service.yaml",
        "k8s/apps/customer-service.yaml",
        "k8s/apps/auth-service.yaml",
        "k8s/apps/core-banking-mock.yaml"
    )

    foreach ($f in $apps) {
        Write-Host "  Deleting $f ..." -ForegroundColor DarkYellow
        kubectl delete -f $f --ignore-not-found=true
    }

    Write-Host "`n-> Apps da xoa. Postgres + RabbitMQ van dang chay." -ForegroundColor Green
    kubectl get pods -n $Namespace
    exit 0
}

# ==============================================================
# Option B: Xoa toan bo namespace (va PVC neu khong -KeepPVC)
# ==============================================================
Write-Host "`n[1/3] Xoa tat ca resources trong namespace $Namespace..." -ForegroundColor Cyan
kubectl delete namespace $Namespace --ignore-not-found=true
Write-Host "  -> Namespace da xoa (bao gom tat ca Deployments, Services, StatefulSets)" -ForegroundColor Green

# PVC bi giu lai khi xoa namespace trong mot so storage classes
# Phai xoa tay neu can
if (-not $KeepPVC) {
    Write-Host "`n[2/3] Xoa PersistentVolumeClaims..." -ForegroundColor Cyan
    # PVC bi xoa cung khi namespace bi xoa voi StatefulSet volumeClaimTemplates
    # Nhung de chac chan, xoa them:
    kubectl delete pvc --all -n $Namespace --ignore-not-found=true 2>&1 | Out-Null
    Write-Host "  -> PVCs da xoa (du lieu DB se bi mat)" -ForegroundColor Yellow
} else {
    Write-Host "`n[2/3] Giu nguyen PVCs (-KeepPVC)" -ForegroundColor Yellow
}

# ==============================================================
# Option C: Xoa luon Kind cluster
# ==============================================================
if ($DeleteCluster) {
    Write-Host "`n[3/3] Xoa Kind cluster 'saving-banking'..." -ForegroundColor Red
    $confirm = Read-Host "Ban co chac muon xoa cluster? (yes/no)"
    if ($confirm -eq "yes") {
        kind delete cluster --name saving-banking
        Write-Host "  -> Cluster da xoa" -ForegroundColor Green

        Write-Host "  Stop registry container..." -ForegroundColor Yellow
        docker stop kind-registry 2>&1 | Out-Null
        Write-Host "  -> Registry da stop (du lieu image van con, chay lai bang setup-cluster.ps1)" -ForegroundColor Yellow
    } else {
        Write-Host "  -> Huy xoa cluster" -ForegroundColor Yellow
    }
} else {
    Write-Host "`n[3/3] Giu nguyen Kind cluster (them -DeleteCluster de xoa)" -ForegroundColor Yellow
}

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host " Teardown hoan tat!" -ForegroundColor Green
Write-Host "------------------------------------------------------------" -ForegroundColor Green
Write-Host " De deploy lai:" -ForegroundColor Cyan
Write-Host "   .\k8s\deploy-all.ps1" -ForegroundColor Cyan
Write-Host " De reset toan bo (xoa cluster):" -ForegroundColor Cyan
Write-Host "   .\k8s\teardown.ps1 -DeleteCluster" -ForegroundColor Cyan
Write-Host "============================================================`n" -ForegroundColor Green
