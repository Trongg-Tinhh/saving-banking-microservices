"""
Saving Interest Calculation Service — FastAPI application entry point.

Port: 8087
Auth: JWT (parse-only) — used only for authenticated external endpoints.
      Internal calculation endpoints (/api/v1/interest/*) are public.

This service is STATELESS — no database, no RabbitMQ.
It exposes pure HTTP endpoints that perform interest calculations.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.middleware import CorrelationIdMiddleware
from app.routers import interest

app = FastAPI(
    title="Saving Interest Calculation Service",
    description=(
        "Pure stateless service for computing saving contract interest.\n\n"
        "Endpoints:\n"
        "- `POST /api/v1/interest/calculate` — full-term interest\n"
        "- `POST /api/v1/interest/early-withdrawal` — early withdrawal interest\n"
        "- `POST /api/v1/interest/projection` — payment schedule projection"
    ),
    version="1.0.0",
    docs_url="/swagger-ui.html",
    redoc_url="/redoc",
    openapi_url="/v3/api-docs",
)

# ── Middleware ────────────────────────────────────────────────────────────────

app.add_middleware(CorrelationIdMiddleware)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Health ────────────────────────────────────────────────────────────────────

@app.get("/health", tags=["Health"])
def health():
    return {"status": "UP", "service": "saving-interest-service"}

# ── Routers ───────────────────────────────────────────────────────────────────

app.include_router(interest.router)
