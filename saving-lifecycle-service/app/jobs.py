"""
APScheduler jobs for the Saving Lifecycle Service.

Jobs:
  1. process_matured_contracts  — 01:00 UTC daily
     Tìm hợp đồng ACTIVE đã quá ngày đáo hạn → đánh dấu MATURED.

  2. check_pre_maturity         — 02:00 UTC daily
     Tìm hợp đồng sắp đáo hạn trong N ngày → log cảnh báo (mở rộng sau).

  3. pay_periodic_interest      — 06:00 UTC daily
     Tìm hợp đồng ACTIVE có interestPaymentMethod = MONTHLY / QUARTERLY
     và hôm nay đúng ngày trả lãi → credit lãi vào tài khoản + ghi ledger.
"""
import asyncio
import logging
from datetime import date, timedelta
from decimal import Decimal

from app.contract_client    import contract_client
from app.interest_client    import interest_client
from app.account_client     import account_client
from app.transaction_client import transaction_client
from app.config import settings

logger = logging.getLogger(__name__)

# ── Hằng số kỳ hạn ────────────────────────────────────────────────────────────

PERIOD_DAYS = {
    "MONTHLY":   30,
    "QUARTERLY": 91,
}


# ── Job 1: Xử lý hợp đồng đến hạn ────────────────────────────────────────────

async def _process_matured_contracts_async() -> dict:
    today = date.today()
    logger.info("[lifecycle] Bắt đầu xử lý đáo hạn cho ngày %s", today)

    processed = failed = page = 0
    PAGE_SIZE = 100

    while True:
        contracts = await contract_client.get_active_contracts_by_status(
            status="ACTIVE", page=page, size=PAGE_SIZE
        )
        if not contracts:
            break

        for c in contracts:
            maturity_str = c.get("maturityDate")
            if not maturity_str:
                continue
            try:
                maturity_date = date.fromisoformat(maturity_str)
            except ValueError:
                continue

            if maturity_date <= today:
                contract_no = c.get("contractNo")
                logger.info("[lifecycle] Đánh dấu đáo hạn: %s (maturityDate=%s)",
                            contract_no, maturity_str)
                success = await contract_client.mark_matured(contract_no)
                if success:
                    processed += 1
                else:
                    failed += 1

        if len(contracts) < PAGE_SIZE:
            break
        page += 1

    result = {"date": today.isoformat(), "processed": processed, "failed": failed}
    logger.info("[lifecycle] Xử lý đáo hạn hoàn tất: %s", result)
    return result


def process_matured_contracts():
    """Sync wrapper cho APScheduler."""
    try:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(_process_matured_contracts_async())
        loop.close()
        return result
    except Exception as exc:
        logger.error("[lifecycle] process_matured_contracts thất bại: %s", exc, exc_info=True)


# ── Job 2: Cảnh báo gần đáo hạn ───────────────────────────────────────────────

async def _check_pre_maturity_async() -> dict:
    today     = date.today()
    threshold = today + timedelta(days=settings.pre_maturity_days)
    logger.info("[lifecycle] Kiểm tra gần đáo hạn (trước %s)", threshold)

    notified = page = 0
    PAGE_SIZE = 100

    while True:
        contracts = await contract_client.get_active_contracts_by_status(
            status="ACTIVE", page=page, size=PAGE_SIZE
        )
        if not contracts:
            break

        for c in contracts:
            maturity_str = c.get("maturityDate")
            if not maturity_str:
                continue
            try:
                maturity_date = date.fromisoformat(maturity_str)
            except ValueError:
                continue

            if today < maturity_date <= threshold:
                days_left = (maturity_date - today).days
                logger.info("[lifecycle] Sắp đáo hạn: contract=%s cif=%s còn %d ngày",
                            c.get("contractNo"), c.get("cif"), days_left)
                notified += 1

        if len(contracts) < PAGE_SIZE:
            break
        page += 1

    result = {"date": today.isoformat(), "threshold": threshold.isoformat(), "notified": notified}
    logger.info("[lifecycle] Kiểm tra gần đáo hạn hoàn tất: %s", result)
    return result


def check_pre_maturity():
    try:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(_check_pre_maturity_async())
        loop.close()
        return result
    except Exception as exc:
        logger.error("[lifecycle] check_pre_maturity thất bại: %s", exc, exc_info=True)


# ── Job 3: Trả lãi định kỳ (MONTHLY / QUARTERLY) ──────────────────────────────

def _is_interest_due(open_date_str: str, payment_method: str, today: date) -> bool:
    """
    Kiểm tra hôm nay có phải ngày trả lãi không.
    Logic: (today - openDate).days chia hết cho số ngày 1 kỳ (30 hoặc 91)
           VÀ số ngày đó > 0 (tức không phải ngày mở sổ).
    """
    try:
        open_date = date.fromisoformat(open_date_str)
    except (ValueError, TypeError):
        return False

    period_days = PERIOD_DAYS.get(payment_method)
    if not period_days:
        return False

    days_since_open = (today - open_date).days
    return days_since_open > 0 and days_since_open % period_days == 0


async def _pay_periodic_interest_async() -> dict:
    today = date.today()
    logger.info("[lifecycle] Bắt đầu trả lãi định kỳ cho ngày %s", today)

    paid = skipped = failed = page = 0
    PAGE_SIZE = 500

    while True:
        # Gọi internal endpoint — trả danh sách ACTIVE MONTHLY/QUARTERLY chưa đáo hạn
        response = await contract_client.get_periodic_interest_contracts(
            page=page, size=PAGE_SIZE
        )
        contracts = response.get("content", [])
        if not contracts:
            break

        for c in contracts:
            contract_no    = c.get("contractNo")
            cif            = c.get("cif")
            account_no     = c.get("sourceAccountNo")
            payment_method = c.get("interestPaymentMethod")
            open_date_str  = c.get("openDate")
            maturity_str   = c.get("maturityDate")

            # Kiểm tra đủ thông tin
            if not all([contract_no, cif, account_no, payment_method, open_date_str]):
                logger.warning("[lifecycle] Thiếu thông tin hợp đồng: %s", c)
                skipped += 1
                continue

            # Hôm nay có phải ngày trả lãi không?
            if not _is_interest_due(open_date_str, payment_method, today):
                skipped += 1
                continue

            # Kiểm tra không phải ngày đáo hạn (ngày đó contract-service xử lý riêng)
            try:
                if maturity_str and date.fromisoformat(maturity_str) <= today:
                    skipped += 1
                    continue
            except ValueError:
                pass

            period_days = PERIOD_DAYS[payment_method]

            # Lấy thông tin lãi suất từ contract summary
            try:
                principal   = Decimal(str(c.get("principalAmount", "0")))
                annual_rate = Decimal(str(c.get("interestRate", "0")))
            except Exception:
                logger.warning("[lifecycle] Không parse được số tiền: contract=%s", contract_no)
                failed += 1
                continue

            # Tính lãi kỳ này
            interest = await interest_client.calculate_period_interest(
                principal=principal,
                annual_rate=annual_rate,
                period_days=period_days,
            )
            if interest is None or interest <= 0:
                logger.warning("[lifecycle] Lãi tính được = 0 cho contract=%s", contract_no)
                skipped += 1
                continue

            # Idempotency key: contract_no + ngày trả lãi
            ref = f"INTEREST-{contract_no}-{today.isoformat()}"
            desc = (
                f"Trả lãi {'tháng' if payment_method == 'MONTHLY' else 'quý'} "
                f"ngày {today.isoformat()} — hợp đồng {contract_no}"
            )

            # Credit tiền lãi vào tài khoản nguồn
            credited = await account_client.credit_internal(
                account_no=account_no,
                amount=interest,
                reference=ref,
                description=desc,
            )
            if not credited:
                logger.error("[lifecycle] Credit thất bại: contract=%s account=%s",
                             contract_no, account_no)
                failed += 1
                continue

            # Ghi ledger
            await transaction_client.record_interest(
                contract_no=contract_no,
                account_no=account_no,
                cif=cif,
                amount=interest,
                reference=ref,
                description=desc,
            )

            logger.info("[lifecycle] Đã trả lãi: contract=%s interest=%s account=%s",
                        contract_no, interest, account_no)
            paid += 1

        total = response.get("totalElements", 0)
        fetched_so_far = (page + 1) * PAGE_SIZE
        if fetched_so_far >= total:
            break
        page += 1

    result = {
        "date":    today.isoformat(),
        "paid":    paid,
        "skipped": skipped,
        "failed":  failed,
    }
    logger.info("[lifecycle] Trả lãi định kỳ hoàn tất: %s", result)
    return result


def pay_periodic_interest():
    """Sync wrapper cho APScheduler."""
    try:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(_pay_periodic_interest_async())
        loop.close()
        return result
    except Exception as exc:
        logger.error("[lifecycle] pay_periodic_interest thất bại: %s", exc, exc_info=True)
