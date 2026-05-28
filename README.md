# 🏦 Saving Banking Microservices System

> **A full-stack banking microservices platform for managing savings accounts (tiền gửi tiết kiệm)**  
> Built with **Java Spring Boot** · **Python FastAPI** · **Node.js NestJS** · **React + Vite** · **PostgreSQL** · **RabbitMQ**

---

## 📐 Kiến trúc tổng quan

```
 Browser / Mobile App
        │
        ▼
 ┌──────────────────────┐     ┌─────────────────────┐
 │  saving-banking-web  │     │  Core Banking Mock  │ :8099
 │  React + Vite  :80   │     │  (CBS Integration)  │
 │  (Nginx SPA)         │     └─────────────────────┘
 └─────────┬────────────┘              ▲
           │ /api/* proxy              │
           ▼                           │ CBS sync
 ┌────────────────────┐                │
 │   API Gateway      │────────────────┘
 │   NestJS  :3000    │
 └────────┬───────────┘
          │ JWT validation
          ▼
 ┌──────────────────────────────────────────────────────────────┐
 │                    BUSINESS SERVICES                         │
 │                                                              │
 │  Auth Service          (Spring Boot)  :8081                  │
 │  Customer Service      (Spring Boot)  :8082                  │
 │  Account Service       (Spring Boot)  :8083                  │
 │  Saving Product Svc    (Spring Boot)  :8084                  │
 │  Saving Contract Svc   (Spring Boot)  :8085 ───────────────┐ │
 │  Transaction Service   (Spring Boot)  :8086 ───────────────┘ │
 │  Interest Calc. Svc    (FastAPI)      :8087                  │
 │  Saving Lifecycle Svc  (FastAPI)      :8088                  │
 │  Notification Service  (NestJS)       :8089                  │
 └──────────────────────────────────┬───────────────────────────┘
                                    │
               ┌────────────────────┴─────────────────────┐
               ▼                                          ▼
        PostgreSQL :5432                         RabbitMQ :5672
        (9 schemas)                         saving.events exchange
                                              notification.queue
```

---

## 🗂 Cấu trúc thư mục

```
saving-banking-microservices/
├── api-gateway/                  ← Node.js NestJS  — Routing, JWT middleware, rate limit
├── auth-service/                 ← Java Spring Boot — JWT issue, refresh, OTP
├── customer-service/             ← Java Spring Boot — CIF, KYC management
├── account-service/              ← Java Spring Boot — Balance, debit/credit
├── saving-product-service/       ← Java Spring Boot — Products, terms, interest rates
├── saving-contract-service/      ← Java Spring Boot — Saving contract lifecycle
├── saving-transaction-service/   ← Java Spring Boot — Ledger, CBS sync, outbox
├── saving-interest-service/      ← Python FastAPI   — Interest calculation engine
├── saving-lifecycle-service/     ← Python FastAPI   — APScheduler, maturity jobs
├── saving-notification-service/  ← Node.js NestJS   — RabbitMQ consumer, event log
├── core-banking-mock/            ← Python FastAPI   — CBS stub (credit/debit mock)
├── saving-banking-web/           ← React + Vite + TypeScript — Web UI (SPA)
│   ├── src/
│   │   ├── pages/                ← Dashboard, Products, Contracts, Customers, ...
│   │   ├── hooks/                ← React Query hooks
│   │   ├── services/             ← Axios API clients
│   │   ├── stores/               ← Zustand (auth, UI state)
│   │   ├── types/                ← TypeScript interfaces
│   │   └── constants/            ← Routes, config
│   ├── Dockerfile                ← Multi-stage: node build + nginx serve
│   └── nginx.conf                ← SPA routing + /api proxy to api-gateway
├── k8s/
│   ├── 00-namespace.yaml         ← Namespace: saving-banking
│   ├── config/
│   │   ├── configmap.yaml        ← Service URLs, app config
│   │   └── secret.yaml           ← DB credentials, JWT secret
│   ├── infra/
│   │   ├── postgres/             ← StatefulSet + PVC + Service
│   │   └── rabbitmq/             ← StatefulSet + PVC + Service
│   ├── apps/                     ← Deployment + Service for each microservice
│   ├── build-push.ps1            ← Build Docker images → push to Kind registry
│   └── deploy-all.ps1            ← Full deploy pipeline (1-click)
├── db/
│   ├── 01_init_schemas.sql       ← Create 9 schemas + tables + indexes
│   └── 02_seed_data.sql          ← Test users, customers, accounts, contracts
├── docker-compose.yml
├── .env                          ← Root env vars (created from .env.example)
├── .env.example
└── README.md
```

---

## 🚀 Hướng dẫn chạy bằng Docker

### Yêu cầu hệ thống

| Công cụ | Phiên bản tối thiểu |
|---|---|
| Docker Desktop | 24.x+ |
| Docker Compose | v2.x+ (plugin, đi kèm Docker Desktop) |
| Git | 2.x+ |
| RAM | **8 GB+** (khuyến nghị 12 GB để chạy toàn bộ) |
| Disk | ~3 GB (images + volumes) |

### Bước 1 — Clone và cấu hình biến môi trường

```bash
git clone <repo-url> saving-banking-microservices
cd saving-banking-microservices

# Tạo file .env từ example (giữ nguyên giá trị mặc định là đủ để chạy local)
cp .env.example .env
```

> ⚠️ **Không commit file `.env`** — nó đã được thêm vào `.gitignore`.

### Bước 2 — Khởi động Infrastructure trước

```bash
# Start PostgreSQL + RabbitMQ + Core Banking Mock
docker compose up -d postgres rabbitmq core-banking-mock

# Chờ khoảng 30 giây rồi kiểm tra
docker compose ps

# Đảm bảo postgres đang healthy
docker compose logs postgres --tail=20
```

> 💡 Init SQL (`db/01_init_schemas.sql`) sẽ tự động tạo tất cả schemas và tables khi postgres khởi động lần đầu.

### Bước 3 — Build và khởi động toàn bộ hệ thống

```bash
# Build images và start tất cả services (lần đầu mất ~5–10 phút)
docker compose up -d --build

# Theo dõi quá trình khởi động
docker compose logs -f

# Xem logs từng service
docker compose logs -f auth-service
docker compose logs -f saving-transaction-service
docker compose logs -f saving-interest-service
docker compose logs -f saving-notification-service
```

> ⏱ **Thứ tự khởi động**: postgres & rabbitmq → core-banking-mock → auth-service → customer/account/product services → contract/transaction services → interest service → lifecycle service → notification service → **api-gateway** (cuối cùng)

### Bước 4 — Kiểm tra hệ thống

```bash
# Check status tất cả containers
docker compose ps

# Health check nhanh
curl http://localhost:3000/health                            # API Gateway
curl http://localhost:8081/actuator/health                  # Auth Service
curl http://localhost:8082/actuator/health                  # Customer Service
curl http://localhost:8083/actuator/health                  # Account Service
curl http://localhost:8084/actuator/health                  # Saving Product Service
curl http://localhost:8085/actuator/health                  # Saving Contract Service
curl http://localhost:8086/actuator/health                  # Transaction Service
curl http://localhost:8087/health                           # Interest Calc. Service
curl http://localhost:8088/health                           # Saving Lifecycle Service
curl http://localhost:8089/api/v1/notifications/health      # Notification Service
curl http://localhost:8099/health                           # Core Banking Mock
```

---

## 🖥️ Web UI — saving-banking-web

React + Vite + TypeScript SPA dùng **Ant Design** làm component library. Chạy trong container Nginx, proxy `/api/*` sang api-gateway qua K8s DNS.

### Tính năng

| Trang | Mô tả | Phân quyền |
|---|---|---|
| Dashboard | Thống kê tổng quan, hoạt động gần đây | Tất cả |
| Sản phẩm tiết kiệm | Danh sách, tạo/chỉnh sửa, kỳ hạn, lãi suất, chính sách tất toán sớm | ADMIN/TELLER/MANAGER |
| Tính lãi dự kiến | Simulate lãi suất theo sản phẩm + kỳ hạn | Tất cả |
| Tra cứu khách hàng | Tìm theo CIF/tên/CMND | ADMIN/TELLER/MANAGER |
| Tạo tài khoản | Mở tài khoản thanh toán cho khách hàng | ADMIN/TELLER/MANAGER |
| Mở sổ tiết kiệm | Chọn sản phẩm, kỳ hạn, tài khoản nguồn | Tất cả |
| Sổ tiết kiệm của tôi | Xem danh sách, chi tiết, tất toán sớm | Tất cả |
| Lịch sử giao dịch | Xem giao dịch theo tài khoản/thời gian | Tất cả |
| Thông báo | Nhận thông báo khi mở/đóng/đáo hạn sổ | Tất cả |

### Chạy development

```bash
cd saving-banking-web
npm install
npm run dev       # http://localhost:5173
```

> API calls tự động proxy sang `http://localhost:3000` qua `vite.config.ts`.

### Build Docker image

```bash
cd saving-banking-web
docker build -t saving-banking-web:latest .
```

**Multi-stage build:**
1. `node:20-alpine` → cài deps, `npm run build` → `/app/dist`
2. `nginx:alpine` → copy dist, serve SPA + proxy `/api` → api-gateway

### nginx.conf (key points)

```nginx
# Static assets: cache 1 year
location ~* \.(js|css|png|svg|woff2|...)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}

# API proxy (K8s: api-gateway:3000 via cluster DNS)
location /api/ {
    proxy_pass http://api-gateway:3000;
}

# SPA fallback
location / {
    try_files $uri $uri/ /index.html;
}
```

---

## ☸️ Deploy trên Kubernetes (Kind)

Toàn bộ hệ thống có thể deploy lên **Kind** (Kubernetes in Docker) với 1 lệnh.

### Yêu cầu

| Công cụ | Phiên bản |
|---|---|
| Docker Desktop | 24.x+ |
| Kind | 0.22+ |
| kubectl | 1.28+ |
| PowerShell | 5.1+ (Windows) |

### Bước 1 — Tạo Kind cluster + local registry

```powershell
# Tạo cluster với port mapping 80→8080
kind create cluster --config kind-config.yaml --name saving-banking

# (Nếu chưa có registry) Tạo local registry
docker run -d --restart=always -p 5001:5000 --name kind-registry registry:2
docker network connect kind kind-registry
```

> `kind-config.yaml` cần khai báo `extraPortMappings: containerPort: 80 → hostPort: 8080` để Ingress hoạt động.

### Bước 2 — Build và push images vào registry

```powershell
# Build tất cả services và push vào localhost:5001
.\k8s\build-push.ps1
```

Script tự động build Docker image cho từng service và tag `localhost:5001/<name>:latest`.

### Bước 3 — Deploy toàn bộ lên cluster

```powershell
# Deploy đầy đủ (NGINX Ingress + namespace + infra + apps + web)
.\k8s\deploy-all.ps1

# Bỏ qua cài NGINX Ingress nếu đã có
.\k8s\deploy-all.ps1 -SkipIngress
```

**Thứ tự deploy trong script:**
1. NGINX Ingress Controller
2. Namespace `saving-banking` + ConfigMap + Secret
3. PostgreSQL + RabbitMQ (StatefulSet)
4. core-banking-mock
5. auth-service
6. Tier 1: customer / account / saving-product (song song)
7. Tier 2: saving-contract → Tier 3: saving-transaction → Tier 4: saving-interest → Tier 5: saving-lifecycle → Tier 6: notification
8. api-gateway → saving-banking-web + Ingress

### Bước 4 — Kiểm tra

```powershell
# Xem trạng thái pods
kubectl get pods -n saving-banking

# Xem services
kubectl get svc -n saving-banking

# Logs của 1 service
kubectl logs -n saving-banking deploy/auth-service -f
```

### Truy cập sau khi deploy

| URL | Mô tả |
|---|---|
| http://localhost:8080 | Web UI (qua NGINX Ingress) |
| http://localhost:3000 | API Gateway (NodePort - dev only) |
| http://localhost:15672 | RabbitMQ Management UI (guest/guest) |

### Rebuild 1 service

```powershell
# Rebuild image
docker build -t localhost:5001/auth-service:latest ./auth-service
docker push localhost:5001/auth-service:latest

# Rolling restart
kubectl rollout restart deployment/auth-service -n saving-banking
kubectl rollout status deployment/auth-service -n saving-banking
```

---

## 🌐 Service Ports & URLs

| Service | Port | Health URL | API Docs |
|---|---|---|---|
| **Web UI** | 5173 (dev) / 8080 (K8s) | — | http://localhost:5173 hoặc http://localhost:8080 |
| **API Gateway** | 3000 | `/health` | http://localhost:3000/api/docs |
| **Auth Service** | 8081 | `/actuator/health` | http://localhost:8081/swagger-ui.html |
| **Customer Service** | 8082 | `/actuator/health` | http://localhost:8082/swagger-ui.html |
| **Account Service** | 8083 | `/actuator/health` | http://localhost:8083/swagger-ui.html |
| **Saving Product Service** | 8084 | `/actuator/health` | http://localhost:8084/swagger-ui.html |
| **Saving Contract Service** | 8085 | `/actuator/health` | http://localhost:8085/swagger-ui.html |
| **Transaction Service** | 8086 | `/actuator/health` | http://localhost:8086/swagger-ui.html |
| **Interest Calc. Service** | 8087 | `/health` | http://localhost:8087/docs |
| **Saving Lifecycle Service** | 8088 | `/health` | http://localhost:8088/docs |
| **Notification Service** | 8089 | `/api/v1/notifications/health` | http://localhost:8089/api/docs |
| **Core Banking Mock** | 8099 | `/health` | http://localhost:8099/docs |
| **PostgreSQL** | 5432 | — | DB: `saving_banking` |
| **RabbitMQ UI** | 15672 | — | http://localhost:15672 (guest/guest) |

---

## 🔑 Test Credentials

### Tài khoản đăng nhập

| Username | Password | Role | CIF |
|---|---|---|---|
| `admin` | `Admin@123` | ROLE_ADMIN | — |
| `teller01` | `Teller@123` | ROLE_TELLER | — |
| `customer001` | `Test@123` | ROLE_CUSTOMER | `CIF0001` |
| `customer002` | `Test@123` | ROLE_CUSTOMER | `CIF0002` |

### Tài khoản thanh toán (seed data)

| Account No | CIF | Số dư |
|---|---|---|
| `ACC001001` | CIF0001 | 150,000,000 VND |
| `ACC001002` | CIF0001 | 50,000,000 VND |
| `ACC002001` | CIF0002 | 200,000,000 VND |

### Sổ tiết kiệm mẫu

| Contract No | CIF | Trạng thái | Gốc | Kỳ hạn |
|---|---|---|---|---|
| `SC-2025-000001` | CIF0001 | ACTIVE | 50,000,000 VND | 6 tháng |
| `SC-2025-000002` | CIF0002 | ACTIVE | 100,000,000 VND | 12 tháng |
| `SC-2025-000004` | CIF0003 | ACTIVE (sắp đáo hạn) | 20,000,000 VND | 3 tháng |

---

## 📡 API Quick Test

### 1. Đăng nhập — lấy Access Token

```bash
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"customer001","password":"Test@123"}'
```

Response:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "...",
    "expiresIn": 3600
  }
}
```

```bash
# Lưu token để dùng cho các request tiếp theo
TOKEN="eyJhbGc..."
```

### 2. Xem thông tin khách hàng

```bash
curl -X GET http://localhost:3000/api/v1/customers/CIF0001 \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Xem sản phẩm tiết kiệm

```bash
curl -X GET http://localhost:3000/api/v1/saving-products/public
```

### 4. Tính lãi dự kiến

```bash
curl -X POST http://localhost:8087/api/v1/interests/simulate \
  -H "Content-Type: application/json" \
  -d '{
    "productCode": "TERM_SAVING_EOT",
    "termId": "TERM_EOT_6M",
    "amount": 10000000,
    "openDate": "2025-05-26",
    "annualRate": 5.5
  }'
```

### 5. Mở sổ tiết kiệm

```bash
curl -X POST http://localhost:3000/api/v1/saving-contracts/open \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "cif": "CIF0001",
    "productCode": "TERM_SAVING_EOT",
    "termId": "TERM_EOT_6M",
    "principalAmount": 10000000,
    "sourceAccountNo": "ACC001001",
    "maturityInstructionType": "RENEW_PRINCIPAL",
    "receivingAccountNo": "ACC001001"
  }'
```

### 6. Xem danh sách sổ tiết kiệm của khách hàng

```bash
curl -X GET http://localhost:8085/api/v1/customers/CIF0001/saving-contracts \
  -H "Authorization: Bearer $TOKEN"
```

### 7. Tất toán sổ tiết kiệm

```bash
curl -X POST http://localhost:8085/api/v1/saving-contracts/SC-2025-000001/close \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"closeType": "EARLY_CLOSED", "receivingAccountNo": "ACC001001"}'
```

### 8. Trigger scan đáo hạn thủ công

```bash
curl -X POST http://localhost:8088/api/v1/lifecycle/jobs/maturity-scan \
  -H "Content-Type: application/json" \
  -d '{"scanDate": "2025-05-28"}'
```

### 9. Xem thông báo

```bash
curl -X GET "http://localhost:3000/api/v1/notifications?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🔄 Business Flows

### Flow 1: Mở sổ tiết kiệm

```
POST /api/v1/saving-contracts/open (qua API Gateway)
  │
  ├─ Auth Service        → Validate JWT
  ├─ Customer Service    → Kiểm tra KYC = VERIFIED
  ├─ Saving Product Svc  → Lấy sản phẩm & lãi suất
  ├─ Account Service     → Kiểm tra số dư đủ
  ├─ Interest Calc. Svc  → Simulate lãi kỳ hạn
  ├─ Saving Contract Svc → Tạo hợp đồng (status = PENDING)
  ├─ Transaction Service → Tạo giao dịch mở sổ
  ├─ Account Service     → Ghi nợ tài khoản nguồn
  ├─ Core Banking Mock   → Post ledger entry (CBS)
  ├─ Transaction Service → Cập nhật TX → SUCCESS
  ├─ Saving Contract Svc → Cập nhật hợp đồng → ACTIVE
  └─ RabbitMQ            → Publish SAVING_CREATED
       └─ Notification Service → Consume, lưu thông báo
```

### Flow 2: Tất toán sổ tiết kiệm

```
POST /api/v1/saving-contracts/{contractNo}/close
  │
  ├─ Auth Service        → Validate JWT
  ├─ Saving Contract Svc → Verify ACTIVE
  ├─ Interest Calc. Svc  → Tính lãi tất toán (pro-rata hoặc penalty nếu tất toán sớm)
  ├─ Transaction Service → Tạo giao dịch tất toán
  ├─ Account Service     → Cộng gốc + lãi vào tài khoản nhận
  ├─ Core Banking Mock   → Post ledger entry
  ├─ Saving Contract Svc → Cập nhật → CLOSED / EARLY_CLOSED
  └─ RabbitMQ            → Publish SAVING_CLOSED
       └─ Notification Service → Consume, lưu thông báo
```

### Flow 3: Xử lý đáo hạn tự động

```
APScheduler (00:01 UTC hàng ngày)
  │
  ├─ Saving Lifecycle Svc → Scan hợp đồng đáo hạn hôm nay
  ├─ Interest Calc. Svc   → Tính lãi đến ngày đáo hạn
  │
  ├─ Nếu TRANSFER:             Cộng gốc+lãi → đóng hợp đồng (CLOSED)
  ├─ Nếu RENEW_PRINCIPAL:      Trả lãi → tạo hợp đồng mới (gốc giữ nguyên)
  └─ Nếu RENEW_PRINCIPAL_AND_INTEREST: Tạo hợp đồng mới (gốc + lãi)
       │
       ├─ Transaction Service → Ghi ledger tất cả giao dịch
       └─ RabbitMQ            → Publish SAVING_MATURED / SAVING_RENEWED
            └─ Notification Service → Consume, lưu thông báo
```

---

## 🗄️ Database

### Kết nối PostgreSQL

```bash
# Kết nối qua Docker container
docker exec -it saving-postgres psql -U postgres -d saving_banking

# Hoặc dùng client bên ngoài (DBeaver, TablePlus, ...)
# Host: localhost  Port: 5432  DB: saving_banking  User: postgres  Pass: postgres
```

### Schema overview

| Schema | Mô tả |
|---|---|
| `auth_schema` | Users, roles, OTP, refresh tokens |
| `customer_schema` | Customers, KYC |
| `account_schema` | Accounts, account balances |
| `saving_product_schema` | Products, terms, interest rates |
| `saving_contract_schema` | Saving contracts |
| `transaction_schema` | Transactions, outbox events |
| `interest_schema` | Interest calculation history |
| `saving_lifecycle_schema` | Maturity job logs |
| `notification_schema` | Notification event log |

### Queries hữu ích

```sql
-- Xem tất cả schemas
\dn

-- Xem sổ tiết kiệm đang active
SELECT contract_no, cif, principal_amount, interest_rate,
       open_date, maturity_date, status
FROM saving_contract_schema.saving_contracts
WHERE status = 'ACTIVE'
ORDER BY open_date DESC;

-- Xem số dư tài khoản
SELECT a.account_no, a.cif, ab.available_balance, ab.ledger_balance
FROM account_schema.accounts a
JOIN account_schema.account_balances ab ON a.account_no = ab.account_no;

-- Xem lịch sử giao dịch mới nhất
SELECT tx_id, tx_type, contract_no, amount, status, created_at
FROM transaction_schema.transactions
ORDER BY created_at DESC
LIMIT 20;

-- Xem outbox events chưa publish
SELECT outbox_id, event_type, aggregate_id, created_at
FROM transaction_schema.outbox_events
WHERE is_published = FALSE;

-- Xem thông báo
SELECT notification_id, cif, event_type, contract_no, status, created_at
FROM notification_schema.notifications
ORDER BY created_at DESC
LIMIT 20;
```

---

## 📨 RabbitMQ

Truy cập Management UI: **http://localhost:15672** (guest / guest)

### Exchanges & Queues

| Exchange | Type | Queue | Mô tả |
|---|---|---|---|
| `saving.events` | topic | `saving.created` | Khi mở sổ thành công |
| `saving.events` | topic | `saving.closed` | Khi tất toán |
| `saving.events` | topic | `saving.matured` | Khi đáo hạn |
| `saving.events` | topic | `saving.renewed` | Khi tái tục |
| `saving.events` | topic | `interest.paid` | Khi trả lãi định kỳ |
| — | — | `notification.queue` | Notification Service consume |
| — | — | `notification.dlq` | Dead Letter Queue |

---

## 🛠 Local Development (không dùng Docker)

### Yêu cầu thêm

| Công cụ | Phiên bản |
|---|---|
| JDK | 17+ |
| Maven | 3.9+ (hoặc dùng `./mvnw` wrapper) |
| Python | 3.11+ |
| Node.js | 20+ |
| npm | 9+ |

### Bước 1 — Start infra bằng Docker (vẫn cần)

```bash
docker compose up -d postgres rabbitmq core-banking-mock
```

### Bước 2 — Chạy Java services

```bash
# Auth Service
cd auth-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Customer Service (cần auth-service chạy trước)
cd customer-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Tương tự cho các Spring Boot services khác...
```

### Bước 3 — Chạy Python services

```bash
# Interest Calculation Service
cd saving-interest-service
python -m venv .venv
source .venv/bin/activate     # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8087

# Saving Lifecycle Service
cd saving-lifecycle-service
# (tương tự)
uvicorn app.main:app --reload --port 8088
```

### Bước 4 — Chạy Node.js services

```bash
# Notification Service
cd saving-notification-service
npm install
npm run start:dev

# API Gateway
cd api-gateway
npm install
npm run start:dev
```

---

## 🐳 Docker Commands hữu ích

```bash
# Xem status tất cả containers
docker compose ps

# Xem logs real-time của 1 service
docker compose logs -f saving-transaction-service

# Rebuild và restart 1 service (sau khi sửa code)
docker compose up -d --build auth-service

# Restart 1 service (không build lại)
docker compose restart notification-service

# Stop toàn bộ (giữ volumes)
docker compose down

# Stop và xóa toàn bộ data (reset sạch)
docker compose down -v

# Reset hoàn toàn và build lại từ đầu
docker compose down -v --remove-orphans
docker system prune -f
docker compose up -d --build

# Vào shell trong container
docker exec -it saving-auth bash
docker exec -it saving-postgres psql -U postgres -d saving_banking

# Xem resource usage
docker stats
```

---

## 🐛 Troubleshooting

### ❌ Port đã bị chiếm dụng

```bash
# Windows
netstat -ano | findstr ":5432"
taskkill /PID <PID> /F

# macOS/Linux
lsof -i :5432
kill -9 <PID>
```

Hoặc thay đổi port mapping trong `docker-compose.yml`, ví dụ `"5433:5432"`.

### ❌ Service không start — timeout / unhealthy

```bash
# Xem logs chi tiết
docker compose logs --tail=100 <service-name>

# Kiểm tra healthcheck
docker inspect saving-auth | grep -A 20 '"Health"'
```

**Nguyên nhân thường gặp:**
- PostgreSQL hoặc RabbitMQ chưa healthy → đợi thêm, không cần làm gì
- Sai env var → kiểm tra file `.env` và `docker-compose.yml`
- Port conflict → xem mục ở trên

### ❌ Database: table not found / schema not found

```bash
# Init SQL chỉ chạy khi volume trống. Nếu volume cũ đã tồn tại:
docker compose down -v           # Xóa volume cũ
docker compose up -d postgres    # Khởi động lại để chạy lại init SQL
```

### ❌ api-gateway không start

API Gateway phụ thuộc vào **tất cả** các services khác phải healthy. Kiểm tra service nào đang unhealthy:

```bash
docker compose ps
# Service nào STATUS không phải "running (healthy)" → xem logs của service đó
```

### ❌ RabbitMQ connection refused

```bash
# Kiểm tra RabbitMQ đang chạy
docker compose logs rabbitmq --tail=30

# Thử kết nối thủ công
curl http://localhost:15672/api/overview -u guest:guest
```

---

## 📦 Tech Stack

| Layer | Technology | Version |
|---|---|---|
| **Web UI** | React + Vite + TypeScript | 18.x / 5.x |
| **UI Components** | Ant Design | 5.x |
| **State Management** | Zustand | 4.x |
| **API Client** | Axios + TanStack React Query | 1.x / 5.x |
| Java Services | Spring Boot | 3.2.x |
| Java ORM | Spring Data JPA + Hibernate | 6.x |
| Java Security | Spring Security + JWT | 6.x |
| Java API Docs | SpringDoc OpenAPI (Swagger) | 2.x |
| Python Services | FastAPI | 0.110.x |
| Python ORM | SQLAlchemy | 2.x |
| Python Scheduler | APScheduler | 3.x |
| Node.js Services | NestJS | 10.x |
| Node.js ORM | TypeORM | 0.3.x |
| Database | PostgreSQL | 15 |
| Message Queue | RabbitMQ | 3.12 |
| Container | Docker + Docker Compose v2 | — |
| Orchestration | Kubernetes (Kind) | 1.29+ |
| Ingress | NGINX Ingress Controller | 1.x |
| Auth | JWT HS256 | — |

---

## 📋 Thứ tự implement (cho contributor)

Nếu bạn muốn extend hệ thống, implement theo thứ tự sau:

1. `core-banking-mock` — Cần trước cho mọi service tích hợp CBS
2. `auth-service` — Foundation, cần cho token validation
3. `customer-service` — CIF / KYC
4. `account-service` — Balance management
5. `saving-product-service` — Product catalog
6. `saving-interest-service` — Interest calculation engine
7. `saving-contract-service` — Core business logic
8. `saving-transaction-service` — Ledger & CBS sync
9. `saving-notification-service` — Event consumer
10. `saving-lifecycle-service` — Scheduled maturity jobs
11. `api-gateway` — Last, sau khi tất cả services ready

---

## 📝 Changelog

### v1.1.0 — Web UI + K8s deployment + Product management

#### ✨ Tính năng mới

**Web UI (`saving-banking-web`)**
- Thêm toàn bộ giao diện React + Vite + TypeScript + Ant Design
- Dashboard với thống kê tổng quan và hoạt động gần đây
- Quản lý sản phẩm tiết kiệm: CRUD sản phẩm, kỳ hạn, lãi suất, chính sách tất toán sớm
- Tính lãi dự kiến (simulate) theo sản phẩm và kỳ hạn
- Tra cứu khách hàng, chi tiết tài khoản, mở sổ tiết kiệm
- Phân quyền UI theo role: ADMIN/TELLER/MANAGER/CUSTOMER
- Dockerfile multi-stage (node build → nginx serve) + nginx.conf (SPA + API proxy)

**Kubernetes deployment**
- Thêm `k8s/apps/saving-banking-web.yaml` — Deployment + ClusterIP Service
- Cập nhật `k8s/apps/ingress.yaml` — `/api` → api-gateway, `/` → saving-banking-web
- Cập nhật `k8s/build-push.ps1` — thêm saving-banking-web vào danh sách build
- Cập nhật `k8s/deploy-all.ps1` — thêm step 8/8 deploy web + ingress

**Saving Product Service (Backend)**
- API `GET /api/v1/products?activeOnly=false` — admin lấy tất cả sản phẩm (kể cả vô hiệu hóa)
- API `GET /api/v1/products/{code}/terms?activeOnly=false` — lấy tất cả kỳ hạn
- Thêm endpoint bật/tắt kỳ hạn (`PATCH /terms/{id}/toggle`)
- Thêm endpoint upsert chính sách tất toán sớm (`PUT /terms/{id}/early-withdrawal`)
- Thêm kiểm tra trùng lặp lãi suất: cùng `(termId, effectiveFrom)` bị từ chối

#### 🐛 Bug fixes

| Bug | Nguyên nhân | Giải pháp |
|---|---|---|
| EarlyWithdrawalModal: `penaltyRate` ẩn sai | Logic `hidden` bị đảo ngược | Sử dụng `hidden` prop + dynamic `required` rules |
| Crash `/products/{code}`: "terms is not defined" | Biến `terms` bị đổi tên thành `allTerms` nhưng còn 1 chỗ cũ | Cập nhật tham chiếu còn sót |
| Tab "Tất cả" và "Đang hoạt động" hiển thị cùng số | Backend luôn trả `activeOnly=true` | Thêm param `activeOnly` backend; admin gọi 2 query song song |
| Kỳ hạn bị vô hiệu hóa không thể bật lại | `useProductTerms` chỉ fetch kỳ hạn active | Thêm `useAllProductTerms` (activeOnly=false) cho bảng kỳ hạn |
| Lãi suất trùng ngày hiệu lực cho cùng kỳ hạn | Không có validation | Thêm kiểm tra service-layer + DB `@UniqueConstraint` |
| Docker build lỗi TypeScript | `InputNumber<0>`, unused imports, KYC cast | Explicit generic `<InputNumber<number>>`, xóa imports thừa |
| PowerShell ParseException ở deploy-all.ps1 | Em dash `—` (UTF-8 multi-byte) | Thay thế bằng dấu gạch ngang ASCII `-` |
| Ant Design Menu type conflict | Custom `MenuItem` interface | Strip custom props trước khi truyền vào `items` |

#### 🔑 Thiết kế quan trọng

- **Admin role gate**: `saving-product-service` kiểm tra JWT role tại controller; non-admin luôn nhận `activeOnly=true` dù không truyền param
- **Interest rate immutability**: Lịch sử lãi suất là append-only; unique constraint `(product_code, term_id, effective_from)` đảm bảo không trùng
- **K8s access flow**: Browser → `localhost:8080` → Kind port mapping → NGINX Ingress → `/api` → api-gateway `:3000` / `/` → saving-banking-web `:80` → Nginx SPA → (nội bộ) proxy `/api` → api-gateway qua K8s DNS

---

## 📄 License

MIT License — Dự án mục đích học tập / demo kiến trúc Microservices ngân hàng.

---

*Saving Banking Microservices — Education & Demo Purpose*
