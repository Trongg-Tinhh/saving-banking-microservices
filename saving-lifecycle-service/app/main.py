"""
Saving Lifecycle Service — FastAPI application entry point.

Port: 8088
Responsibilities:
  - Daily APScheduler job to detect ACTIVE contracts that have passed
    their maturityDate and call contract-service to mark them MATURED.
  - Pre-maturity notification alerts (N days before maturity).
  - Manual trigger endpoints for ops/testing.

No database — all state lives in contract-service.
"""
import logging

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.middleware import CorrelationIdMiddleware
from app.routers import lifecycle
from app.scheduler import init_scheduler, shutdown_scheduler

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s - %(message)s",
)

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Starting Saving Lifecycle Service...")
    init_scheduler()
    yield
    # Shutdown
    logger.info("Shutting down Saving Lifecycle Service...")
    shutdown_scheduler()


app = FastAPI(
    title="Saving Lifecycle Service",
    description=(
        "Runs scheduled jobs to:\n"
        "- Mark ACTIVE contracts as MATURED when maturityDate is reached\n"
        "- Send pre-maturity alerts N days before maturity\n\n"
        "Manual trigger endpoints available for ops."
    ),
    version="1.0.0",
    docs_url="/swagger-ui.html",
    redoc_url="/redoc",
    openapi_url="/v3/api-docs",
    lifespan=lifespan,
)

app.add_middleware(CorrelationIdMiddleware)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# ── Health ────────────────────────────────────────────────────────────────────

@app.get("/health", tags=["Health"])
def health():
    return {"status": "UP", "service": "saving-lifecycle-service"}

app.include_router(lifecycle.router)
