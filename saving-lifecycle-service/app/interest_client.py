"""
HTTP client for calling saving-interest-service.
Stateless calculation — no JWT required.
"""
import logging
import uuid
from decimal import Decimal

import httpx

from app.config import settings

logger = logging.getLogger(__name__)


class InterestServiceClient:
    BASE    = settings.interest_service_url
    TIMEOUT = httpx.Timeout(10.0, connect=5.0)

    @staticmethod
    def _cid() -> str:
        return str(uuid.uuid4())

    async def calculate_period_interest(
        self,
        principal: Decimal,
        annual_rate: Decimal,
        period_days: int,
    ) -> Decimal | None:
        """
        POST /api/v1/interest/calculate
        Tính lãi đơn cho 1 kỳ (30 ngày hoặc 91 ngày).
        Trả về số tiền lãi, hoặc None nếu gọi thất bại.
        """
        url = f"{self.BASE}/api/v1/interest/calculate"
        payload = {
            "principal_amount":        str(principal),
            "annual_rate":             str(annual_rate),
            "term_days":               period_days,
            "interest_payment_method": "END_OF_TERM",   # single-period calc
        }
        try:
            async with httpx.AsyncClient(timeout=self.TIMEOUT) as client:
                resp = await client.post(
                    url,
                    json=payload,
                    headers={"X-Correlation-ID": self._cid()},
                )
                resp.raise_for_status()
                data = resp.json()
                return Decimal(str(data["interest_earned"]))
        except Exception as exc:
            logger.error("interest-service calculate failed: %s", exc)
            return None


interest_client = InterestServiceClient()
