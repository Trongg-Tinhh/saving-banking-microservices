# 🏦 Saving Banking Microservices System

> **A full-stack banking microservices platform for managing savings accounts (tiền gửi tiết kiệm)**  
> Built with **Java Spring Boot** · **Python FastAPI** · **Node.js NestJS** · **PostgreSQL** · **RabbitMQ**

---

## 📐 Kiến trúc tổng quan

```
                          ┌─────────────────────┐
   Browser / Mobile App   │                     │
          │               │   Core Banking Mock │ :8099
          ▼               │   (CBS Integration) │
 ┌────────────────────┐   └─────────────────────┘
 │   API Gateway      │           ▲
 │   NestJS  :3000    │           │
 └────────┬───────────┘           │
          │ JWT validation        │ CBS sync
          ▼                       │
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

## 🌐 Service Ports & URLs

| Service | Port | Health URL | API Docs |
|---|---|---|---|
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

## 📄 License

MIT License — Dự án mục đích học tập / demo kiến trúc Microservices ngân hàng.

---

*Saving Banking Microservices — Education & Demo Purpose*
