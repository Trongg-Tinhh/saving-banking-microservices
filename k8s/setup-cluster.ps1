# ==============================================================
# PHASE 1: Tao Kind cluster + local registry
# Chay 1 lan duy nhat khi bat dau
# ==============================================================
# Chay: .\k8s\setup-cluster.ps1
# ==============================================================

Set-Location $PSScriptRoot\..

Write-Host "`n[1/5] Kiem tra Docker dang chay..." -ForegroundColor Cyan
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker chua chay. Mo Docker Desktop truoc."
    exit 1
}

Write-Host "[2/5] Tao local registry container (localhost:5001)..." -ForegroundColor Cyan
$registryExists = docker ps -a --format "{{.Names}}" | Select-String "kind-registry"
if (-not $registryExists) {
    docker run -d --restart=always -p 5001:5000 --name kind-registry registry:2
    Write-Host "  -> Registry tao thanh cong" -ForegroundColor Green
} else {
    docker start kind-registry 2>&1 | Out-Null
    Write-Host "  -> Registry da ton tai, da start" -ForegroundColor Yellow
}

Write-Host "[3/5] Tao Kind cluster 'saving-banking'..." -ForegroundColor Cyan
$clusterExists = kind get clusters 2>&1 | Select-String "saving-banking"
if ($clusterExists) {
    Write-Host "  -> Cluster da ton tai. Bo qua." -ForegroundColor Yellow
} else {
    kind create cluster --config k8s/kind-config.yaml
    if ($LASTEXITCODE -ne 0) { Write-Error "Tao cluster that bai"; exit 1 }
    Write-Host "  -> Cluster tao thanh cong" -ForegroundColor Green
}

Write-Host "[4/5] Ket noi registry vao Kind network..." -ForegroundColor Cyan
$connected = docker inspect kind-registry --format "{{.NetworkSettings.Networks.kind}}" 2>&1
if ($connected -eq "<no value>" -or $connected -match "null") {
    docker network connect kind kind-registry
    Write-Host "  -> Registry da ket noi vao mang kind" -ForegroundColor Green
} else {
    Write-Host "  -> Registry da trong mang kind" -ForegroundColor Yellow
}

Write-Host "[5/5] Dang ky local registry voi cluster..." -ForegroundColor Cyan
kubectl apply -f - @"
apiVersion: v1
kind: ConfigMap
metadata:
  name: local-registry-hosting
  namespace: kube-public
data:
  localRegistryHosting.v1: |
    host: "localhost:5001"
    help: "https://kind.sigs.k8s.io/docs/user/local-registry/"
"@

Write-Host "`n============================================" -ForegroundColor Green
Write-Host " Cluster san sang!" -ForegroundColor Green
Write-Host " Registry : localhost:5001" -ForegroundColor Green
Write-Host " Buoc tiep : .\k8s\build-push.ps1" -ForegroundColor Green
Write-Host "============================================`n" -ForegroundColor Green

kubectl get nodes
