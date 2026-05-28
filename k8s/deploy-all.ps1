# ==============================================================
# PHASE 3: Deploy tat ca manifests len K8s cluster
# ==============================================================
# Chay: .\k8s\deploy-all.ps1
# Them -SkipInfra   de bo qua cai NGINX Ingress Controller
# Them -Namespace <ten> neu dung namespace khac (default: saving-banking)
# ==============================================================

param(
    [switch]$SkipIngress,
    [string]$Namespace = "saving-banking"
)

Set-Location $PSScriptRoot\..

# --- Helper: wait for a Deployment to be available ----
function Wait-Deployment {
    param([string]$Name)
    Write-Host "  Waiting for deployment/$Name ..." -ForegroundColor Yellow
    kubectl rollout status deployment/$Name -n $Namespace --timeout=5m
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Deployment $Name khong san sang sau 5 phut!"
        exit 1
    }
    Write-Host "  -> $Name READY" -ForegroundColor Green
}

# --- Helper: wait for a StatefulSet to be ready --------
function Wait-StatefulSet {
    param([string]$Name)
    Write-Host "  Waiting for statefulset/$Name ..." -ForegroundColor Yellow
    kubectl rollout status statefulset/$Name -n $Namespace --timeout=5m
    if ($LASTEXITCODE -ne 0) {
        Write-Error "StatefulSet $Name khong san sang sau 5 phut!"
        exit 1
    }
    Write-Host "  -> $Name READY" -ForegroundColor Green
}

# ==============================================================
# STEP 1: NGINX Ingress Controller
# ==============================================================
if (-not $SkipIngress) {
    Write-Host "`n[1/7] Cai NGINX Ingress Controller cho Kind..." -ForegroundColor Cyan
    $ingressDeployed = kubectl get deployment ingress-nginx-controller -n ingress-nginx 2>&1
    if ($LASTEXITCODE -ne 0) {
        kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
        if ($LASTEXITCODE -ne 0) { Write-Error "Khong the cai Ingress Controller"; exit 1 }
        Write-Host "  Dang cho Ingress Controller san sang (toi da 90s)..." -ForegroundColor Yellow
        kubectl wait --namespace ingress-nginx `
            --for=condition=ready pod `
            --selector=app.kubernetes.io/component=controller `
            --timeout=90s
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "Ingress Controller chua san sang. Tiep tuc deploy nhung Ingress co the chua hoat dong."
        } else {
            Write-Host "  -> Ingress Controller READY" -ForegroundColor Green
        }
    } else {
        Write-Host "  -> Ingress Controller da ton tai" -ForegroundColor Yellow
    }
} else {
    Write-Host "`n[1/7] Bo qua NGINX Ingress Controller (-SkipIngress)" -ForegroundColor Yellow
}

# ==============================================================
# STEP 2: Namespace + Config
# ==============================================================
Write-Host "`n[2/7] Apply namespace + ConfigMap + Secret..." -ForegroundColor Cyan
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/config/secret.yaml
kubectl apply -f k8s/config/configmap.yaml
Write-Host "  -> Namespace va config da apply" -ForegroundColor Green

# ==============================================================
# STEP 3: Infrastructure (PostgreSQL + RabbitMQ)
# ==============================================================
Write-Host "`n[3/7] Deploy PostgreSQL + RabbitMQ..." -ForegroundColor Cyan
kubectl apply -f k8s/infra/postgres/postgres.yaml
kubectl apply -f k8s/infra/rabbitmq/rabbitmq.yaml

Wait-StatefulSet "postgres"
Wait-StatefulSet "rabbitmq"

# ==============================================================
# STEP 4: Core Banking Mock
# ==============================================================
Write-Host "`n[4/7] Deploy core-banking-mock..." -ForegroundColor Cyan
kubectl apply -f k8s/apps/core-banking-mock.yaml
Wait-Deployment "core-banking-mock"

# ==============================================================
# STEP 5: Auth Service (other services depend on it)
# ==============================================================
Write-Host "`n[5/7] Deploy auth-service..." -ForegroundColor Cyan
kubectl apply -f k8s/apps/auth-service.yaml
Wait-Deployment "auth-service"

# ==============================================================
# STEP 6: Application services (in dependency order)
# ==============================================================
Write-Host "`n[6/7] Deploy application services..." -ForegroundColor Cyan

# Tier 1: customer, account, saving-product (depend on auth only)
Write-Host "  -- Tier 1: customer / account / saving-product --" -ForegroundColor DarkCyan
kubectl apply -f k8s/apps/customer-service.yaml
kubectl apply -f k8s/apps/account-service.yaml
kubectl apply -f k8s/apps/saving-product-service.yaml
Wait-Deployment "customer-service"
Wait-Deployment "account-service"
Wait-Deployment "saving-product-service"

# Tier 2: saving-contract (depends on tier-1)
Write-Host "  -- Tier 2: saving-contract --" -ForegroundColor DarkCyan
kubectl apply -f k8s/apps/saving-contract-service.yaml
Wait-Deployment "saving-contract-service"

# Tier 3: saving-transaction (depends on contract)
Write-Host "  -- Tier 3: saving-transaction --" -ForegroundColor DarkCyan
kubectl apply -f k8s/apps/saving-transaction-service.yaml
Wait-Deployment "saving-transaction-service"

# Tier 4: interest (depends on contract) — can run in parallel with transaction
Write-Host "  -- Tier 4: saving-interest --" -ForegroundColor DarkCyan
kubectl apply -f k8s/apps/saving-interest-service.yaml
Wait-Deployment "saving-interest-service"

# Tier 5: lifecycle (depends on interest + transaction)
Write-Host "  -- Tier 5: saving-lifecycle --" -ForegroundColor DarkCyan
kubectl apply -f k8s/apps/saving-lifecycle-service.yaml
Wait-Deployment "saving-lifecycle-service"

# Tier 6: notification (depends on rabbitmq + auth)
Write-Host "  -- Tier 6: notification --" -ForegroundColor DarkCyan
kubectl apply -f k8s/apps/saving-notification-service.yaml
Wait-Deployment "saving-notification-service"

# ==============================================================
# STEP 7: API Gateway + Ingress (last, after all services ready)
# ==============================================================
Write-Host "`n[7/7] Deploy api-gateway + Ingress..." -ForegroundColor Cyan
kubectl apply -f k8s/apps/api-gateway.yaml
kubectl apply -f k8s/apps/ingress.yaml
Wait-Deployment "api-gateway"

# ==============================================================
# Summary
# ==============================================================
Write-Host "`n============================================================" -ForegroundColor Green
Write-Host " Deploy thanh cong!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host " API Gateway  : http://localhost:3000  (NodePort)" -ForegroundColor Green
Write-Host "             : http://localhost        (NGINX Ingress)" -ForegroundColor Green
Write-Host " RabbitMQ UI : http://localhost:15672  (guest / guest)" -ForegroundColor Green
Write-Host "------------------------------------------------------------" -ForegroundColor Green
Write-Host " Xem trang thai pods:" -ForegroundColor Cyan
Write-Host "   kubectl get pods -n $Namespace" -ForegroundColor Cyan
Write-Host " Xem logs 1 service:" -ForegroundColor Cyan
Write-Host "   kubectl logs -n $Namespace deploy/auth-service -f" -ForegroundColor Cyan
Write-Host " Xem tat ca services:" -ForegroundColor Cyan
Write-Host "   kubectl get svc -n $Namespace" -ForegroundColor Cyan
Write-Host "============================================================`n" -ForegroundColor Green

kubectl get pods -n $Namespace
