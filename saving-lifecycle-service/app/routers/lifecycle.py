"""
Lifecycle management endpoints.
Cho phép trigger thủ công các scheduled jobs (dùng để test / ops).
"""
import asyncio
import logging
from datetime import date

from fastapi import APIRouter, BackgroundTasks

from app import jobs
from app.scheduler import scheduler

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/lifecycle", tags=["Lifecycle"])


@router.get("/health")
def health():
    return {
        "status":           "UP",
        "service":          "saving-lifecycle-service",
        "scheduler_running": scheduler.running,
    }


@router.get("/jobs")
def list_jobs():
    """Danh sách các scheduled jobs và thời gian chạy tiếp theo."""
    result = []
    for job in scheduler.get_jobs():
        result.append({
            "id":       job.id,
            "name":     job.name,
            "next_run": str(job.next_run_time) if job.next_run_time else None,
            "trigger":  str(job.trigger),
        })
    return {"jobs": result}


# ── Manual triggers ───────────────────────────────────────────────────────────

@router.post("/trigger/process-matured")
async def trigger_process_matured(background_tasks: BackgroundTasks):
    """Trigger thủ công: đánh dấu hợp đồng đáo hạn."""
    logger.info("[manual trigger] process_matured_contracts")
    background_tasks.add_task(_run_in_executor, jobs.process_matured_contracts)
    return {
        "message": "Maturity processing job triggered",
        "date":    date.today().isoformat(),
    }


@router.post("/trigger/pre-maturity-check")
async def trigger_pre_maturity(background_tasks: BackgroundTasks):
    """Trigger thủ công: kiểm tra gần đáo hạn."""
    logger.info("[manual trigger] check_pre_maturity")
    background_tasks.add_task(_run_in_executor, jobs.check_pre_maturity)
    return {
        "message": "Pre-maturity check triggered",
        "date":    date.today().isoformat(),
    }


@router.post("/trigger/pay-periodic-interest")
async def trigger_pay_periodic_interest(background_tasks: BackgroundTasks):
    """
    Trigger thủ công: trả lãi định kỳ (MONTHLY / QUARTERLY).
    Chạy theo background — trả response ngay, kết quả xem trong log.
    """
    logger.info("[manual trigger] pay_periodic_interest")
    background_tasks.add_task(_run_in_executor, jobs.pay_periodic_interest)
    return {
        "message": "Periodic interest payment job triggered",
        "date":    date.today().isoformat(),
        "note":    "Xem kết quả trong log của saving-lifecycle-service",
    }


# ── Helper ────────────────────────────────────────────────────────────────────

async def _run_in_executor(fn):
    loop = asyncio.get_event_loop()
    await loop.run_in_executor(None, fn)
