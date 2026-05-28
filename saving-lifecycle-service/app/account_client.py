"""
HTTP client for calling account-service internal endpoints.
No JWT required — internal network only.
"""
import logging
import uuid
from decimal import Decimal

import httpx

from app.config import settings

logger = logging.getLogger(__name__)


class AccountServiceClient:
    BASE    = settings.account_service_url
    TIMEOUT = httpx.Timeout(10.0, connect=5.0)

    @staticmethod
    def _cid() -> str:
        return str(uuid.uuid4())

    async def credit_internal(
        self,
        account_no: str,
        amount: Decimal,
        reference: str,
        description: str,
        currency: str = "VND",
    ) -> bool:
        """
        POST /api/v1/accounts/internal/{accountNo}/credit
        Chuyển tiền lãi vào tài khoản — không cần JWT.
        Trả True nếu thành công.
        """
        url = f"{self.BASE}/api/v1/accounts/internal/{account_no}/credit"
        payload = {
            "amount":      str(amount),
            "description": description,
            "reference":   reference,
            "currency":    currency,
        }
        try:
            async with httpx.AsyncClient(timeout=self.TIMEOUT) as client:
                resp = await client.post(
                    url,
                    json=payload,
                    headers={"X-Correlation-ID": self._cid()},
                )
                resp.raise_for_status()
                logger.info("Credited %s %s to account %s (ref=%s)", amount, currency, account_no, reference)
                return True
        except httpx.HTTPStatusError as exc:
            logger.error("account credit HTTP error %s for account=%s: %s",
                         exc.response.status_code, account_no, exc)
            return False
        except Exception as exc:
            logger.error("account credit failed for account=%s: %s", account_no, exc)
            return False


account_client = AccountServiceClient()
