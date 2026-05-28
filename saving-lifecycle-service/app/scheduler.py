"""
APScheduler configuration and lifecycle management.
Uses BackgroundScheduler (threaded) để chạy song song với uvicorn.
"""
import logging

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger

from app.config import settings
from app import jobs

logger = logging.getLogger(__name__)

scheduler = BackgroundScheduler(timezone="UTC")


def init_scheduler():
    """Đăng ký các jobs và khởi động scheduler."""

    # Job 1: đánh dấu hợp đồng đáo hạn — 01:00 UTC hàng ngày
    scheduler.add_job(
        jobs.process_matured_contracts,
        trigger=CronTrigger(
            hour=settings.maturity_cron_hour,
            minute=settings.maturity_cron_minute,
            timezone="UTC",
        ),
        id="process_matured_contracts",
        name="Process Matured Contracts",
        replace_existing=True,
        misfire_grace_time=3600,
    )

    # Job 2: kiểm tra gần đáo hạn — 02:00 UTC hàng ngày
    scheduler.add_job(
        jobs.check_pre_maturity,
        trigger=CronTrigger(
            hour=(settings.maturity_cron_hour + 1) % 24,
            minute=settings.maturity_cron_minute,
            timezone="UTC",
        ),
        id="check_pre_maturity",
        name="Pre-Maturity Notification Check",
        replace_existing=True,
        misfire_grace_time=3600,
    )

    # Job 3: trả lãi định kỳ MONTHLY / QUARTERLY — 06:00 UTC hàng ngày
    scheduler.add_job(
        jobs.pay_periodic_interest,
        trigger=CronTrigger(
            hour=settings.interest_cron_hour,
            minute=settings.interest_cron_minute,
            timezone="UTC",
        ),
        id="pay_periodic_interest",
        name="Pay Periodic Interest (Monthly/Quarterly)",
        replace_existing=True,
        misfire_grace_time=3600,
    )

    scheduler.start()
    logger.info(
        "Scheduler khởi động. Jobs: maturity=%02d:%02d UTC | interest=%02d:%02d UTC",
        settings.maturity_cron_hour,
        settings.maturity_cron_minute,
        settings.interest_cron_hour,
        settings.interest_cron_minute,
    )


def shutdown_scheduler():
    if scheduler.running:
        scheduler.shutdown(wait=False)
        logger.info("Scheduler đã dừng.")
