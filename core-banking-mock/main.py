"""
Core Banking Mock Service
=========================
Mô phỏng Core Banking System (CBS) đơn giản để test integration.
Tất cả API đều return success với delay ngẫu nhiên để simulate real CBS.

Port: 8099
"""

import uuid
import random
import logging
from datetime import datetime, timezone
from typing import Optional, Dict, Any

from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
import asyncio

# ─── Logging ────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | CBS-MOCK | %(levelname)s | %(message)s"
)
logger = logging.getLogger("core-banking-mock")

# ─── App ────────────────────────────────────────────────────────
app = FastAPI(
    title="Core Banking Mock Service",
    description="Mock Core Banking System for Saving Banking Microservices testing",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─── In-memory ledger (for mock state) ──────────────────────────
MOCK_LEDGER: Dict[str, Any] = {}
MOCK_FAIL_RATE = 0.0  # Set to 0.1 to simulate 10% failure rate


# ─── Request/Response Models ─────────────────────────────────────

class LedgerEntryRequest(BaseModel):
    tx_ref: str = Field(..., description="Internal transaction reference")
    debit_account: str = Field(..., description="Account to debit")
    credit_account: str = Field(..., description="Account to credit")
    amount: float = Field(..., gt=0, description="Amount in VND")
    currency: str = Field(default="VND")
    description: Optional[str] = None
    correlation_id: Optional[str] = None


class AccountDebitRequest(BaseModel):
    account_no: str
    amount: float = Field(..., gt=0)
    currency: str = Field(default="VND")
    tx_ref: str
    description: Optional[str] = None
    correlation_id: Optional[str] = None


class AccountCreditRequest(BaseModel):
    account_no: str
    amount: float = Field(..., gt=0)
    currency: str = Field(default="VND")
    tx_ref: str
    description: Optional[str] = None
    correlation_id: Optional[str] = None


class CbsResponse(BaseModel):
    success: bool
    cbs_ref: str
    message: str
    data: Optional[Dict[str, Any]] = None
    timestamp: str


# ─── Helpers ────────────────────────────────────────────────────

def generate_cbs_ref() -> str:
    now = datetime.now(timezone.utc)
    rand = random.randint(100000, 999999)
    return f"CBS-{now.strftime('%Y%m%d%H%M%S')}-{rand}"


def success_response(data: Dict[str, Any] = None, message: str = "CBS transaction posted successfully") -> CbsResponse:
    return CbsResponse(
        success=True,
        cbs_ref=generate_cbs_ref(),
        message=message,
        data=data,
        timestamp=datetime.now(timezone.utc).isoformat(),
    )


async def simulate_cbs_delay():
    """Simulate CBS processing time (50-200ms)"""
    delay = random.uniform(0.05, 0.2)
    await asyncio.sleep(delay)


def maybe_fail():
    """Simulate occasional CBS failure for testing"""
    if random.random() < MOCK_FAIL_RATE:
        raise HTTPException(
            status_code=503,
            detail={
                "success": False,
                "error": "CBS_TIMEOUT",
                "message": "Core Banking System temporarily unavailable",
                "timestamp": datetime.now(timezone.utc).isoformat(),
            }
        )


# ─── Middleware: Request logging ─────────────────────────────────

@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = datetime.now(timezone.utc)
    logger.info(f"→ {request.method} {request.url.path}")
    response = await call_next(request)
    elapsed = (datetime.now(timezone.utc) - start).total_seconds() * 1000
    logger.info(f"← {request.method} {request.url.path} | {response.status_code} | {elapsed:.1f}ms")
    return response


# ─── Health ──────────────────────────────────────────────────────

@app.get("/health", tags=["Health"])
async def health():
    return {
        "status": "UP",
        "service": "core-banking-mock",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "ledger_entries": len(MOCK_LEDGER),
    }


# ─── CBS APIs ────────────────────────────────────────────────────

@app.post("/api/cbs/v1/ledger-entry", response_model=CbsResponse, tags=["Ledger"])
async def post_ledger_entry(request: LedgerEntryRequest):
    """
    Post a double-entry ledger record to Core Banking.
    Used for: open saving, close saving, interest payment.
    """
    await simulate_cbs_delay()
    maybe_fail()

    cbs_ref = generate_cbs_ref()

    # Store in mock ledger
    entry = {
        "cbs_ref": cbs_ref,
        "tx_ref": request.tx_ref,
        "debit_account": request.debit_account,
        "credit_account": request.credit_account,
        "amount": request.amount,
        "currency": request.currency,
        "description": request.description,
        "posted_at": datetime.now(timezone.utc).isoformat(),
    }
    MOCK_LEDGER[cbs_ref] = entry

    logger.info(
        f"Ledger posted: {request.debit_account} → {request.credit_account} | "
        f"{request.amount:,.0f} {request.currency} | ref={cbs_ref}"
    )

    return CbsResponse(
        success=True,
        cbs_ref=cbs_ref,
        message="Ledger entry posted successfully",
        data={
            "tx_ref": request.tx_ref,
            "debit_account": request.debit_account,
            "credit_account": request.credit_account,
            "amount": request.amount,
            "currency": request.currency,
            "posted_at": entry["posted_at"],
        },
        timestamp=datetime.now(timezone.utc).isoformat(),
    )


@app.post("/api/cbs/v1/account/debit", response_model=CbsResponse, tags=["Account"])
async def debit_account(request: AccountDebitRequest):
    """
    Debit a customer account in Core Banking.
    Returns new balance (mocked).
    """
    await simulate_cbs_delay()
    maybe_fail()

    cbs_ref = generate_cbs_ref()

    logger.info(
        f"Account debit: {request.account_no} | "
        f"{request.amount:,.0f} {request.currency} | ref={cbs_ref}"
    )

    return CbsResponse(
        success=True,
        cbs_ref=cbs_ref,
        message="Account debited successfully",
        data={
            "account_no": request.account_no,
            "debited_amount": request.amount,
            "currency": request.currency,
            "tx_ref": request.tx_ref,
            "cbs_ref": cbs_ref,
        },
        timestamp=datetime.now(timezone.utc).isoformat(),
    )


@app.post("/api/cbs/v1/account/credit", response_model=CbsResponse, tags=["Account"])
async def credit_account(request: AccountCreditRequest):
    """
    Credit a customer account in Core Banking.
    Returns new balance (mocked).
    """
    await simulate_cbs_delay()
    maybe_fail()

    cbs_ref = generate_cbs_ref()

    logger.info(
        f"Account credit: {request.account_no} | "
        f"{request.amount:,.0f} {request.currency} | ref={cbs_ref}"
    )

    return CbsResponse(
        success=True,
        cbs_ref=cbs_ref,
        message="Account credited successfully",
        data={
            "account_no": request.account_no,
            "credited_amount": request.amount,
            "currency": request.currency,
            "tx_ref": request.tx_ref,
            "cbs_ref": cbs_ref,
        },
        timestamp=datetime.now(timezone.utc).isoformat(),
    )


@app.post("/api/cbs/v1/account/debit-credit", response_model=CbsResponse, tags=["Account"])
async def debit_credit(
    debit_account_no: str,
    credit_account_no: str,
    amount: float,
    tx_ref: str,
    currency: str = "VND",
    description: Optional[str] = None,
):
    """
    Combined debit-credit (transfer) operation in Core Banking.
    Atomic operation: debit source, credit destination.
    """
    await simulate_cbs_delay()
    maybe_fail()

    cbs_ref = generate_cbs_ref()

    entry = {
        "cbs_ref": cbs_ref,
        "tx_ref": tx_ref,
        "debit_account": debit_account_no,
        "credit_account": credit_account_no,
        "amount": amount,
        "currency": currency,
        "description": description,
        "posted_at": datetime.now(timezone.utc).isoformat(),
    }
    MOCK_LEDGER[cbs_ref] = entry

    logger.info(
        f"Transfer: {debit_account_no} → {credit_account_no} | "
        f"{amount:,.0f} {currency} | ref={cbs_ref}"
    )

    return CbsResponse(
        success=True,
        cbs_ref=cbs_ref,
        message="Debit-credit transfer completed",
        data=entry,
        timestamp=datetime.now(timezone.utc).isoformat(),
    )


@app.get("/api/cbs/v1/ledger/{cbs_ref}", tags=["Ledger"])
async def get_ledger_entry(cbs_ref: str):
    """Get a specific ledger entry by CBS reference"""
    if cbs_ref not in MOCK_LEDGER:
        raise HTTPException(status_code=404, detail=f"CBS reference {cbs_ref} not found")
    return {"success": True, "data": MOCK_LEDGER[cbs_ref]}


@app.get("/api/cbs/v1/ledger", tags=["Ledger"])
async def list_ledger_entries(limit: int = 20):
    """List recent ledger entries (for debugging)"""
    entries = list(MOCK_LEDGER.values())
    return {
        "success": True,
        "total": len(entries),
        "data": entries[-limit:],
    }


@app.get("/api/cbs/v1/account/{account_no}/balance", tags=["Account"])
async def get_account_balance(account_no: str):
    """Mock account balance query from CBS"""
    await simulate_cbs_delay()
    # Return mock balance
    mock_balances = {
        "ACC001001": 150000000.00,
        "ACC001002": 50000000.00,
        "ACC002001": 200000000.00,
        "ACC003001": 80000000.00,
        "ACC004001": 30000000.00,
    }
    balance = mock_balances.get(account_no, random.uniform(1000000, 100000000))
    return {
        "success": True,
        "data": {
            "account_no": account_no,
            "balance": balance,
            "currency": "VND",
            "as_of": datetime.now(timezone.utc).isoformat(),
        }
    }


# ─── Admin/Debug Endpoints ───────────────────────────────────────

@app.delete("/api/cbs/v1/ledger/clear", tags=["Admin"])
async def clear_ledger():
    """Clear mock ledger (dev/test only)"""
    MOCK_LEDGER.clear()
    return {"success": True, "message": "Mock ledger cleared"}


@app.post("/api/cbs/v1/config/fail-rate", tags=["Admin"])
async def set_fail_rate(rate: float):
    """Set failure simulation rate (0.0 - 1.0) for testing circuit breaker"""
    global MOCK_FAIL_RATE
    MOCK_FAIL_RATE = max(0.0, min(1.0, rate))
    return {"success": True, "fail_rate": MOCK_FAIL_RATE}


# ─── Main ────────────────────────────────────────────────────────

if __name__ == "__main__":
    import os
    import uvicorn

    port = int(os.getenv("PORT", 8099))
    logger.info(f"Starting Core Banking Mock Service on port {port}")
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=False, log_level="info")
