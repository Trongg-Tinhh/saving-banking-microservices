"""
HTTP client for calling saving-transaction-service internal endpoint.
Records interest disbursement in the transaction ledger.
No JWT required.
"""
import logging
import uuid
from decimal import Decimal

import httpx

from app.config import settings

logger = logging.getLogger(__name__)


class TransactionServiceClient:
    BASE    = settings.transaction_service_url
    TIMEOUT = httpx.Timeout(10.0, connect=5.0)

    @staticmethod
    def _cid() -> str:
        return str(uuid.uuid4())

    async def record_interest(
        self,
        contract_no: str,
        account_no: str,
        cif: str,
        amount: Decimal,
        reference: str,
        description: str,
        currency: str = "VND",
    ) -> bool:
        """
        POST /api/v1/transactions/internal
        Ghi nhận giao dịch trả lãi định kỳ vào ledger.
        """
        url = f"{self.BASE}/api/v1/transactions/internal"
        payload = {
            "transactionRef":  reference,
            "accountNo":       account_no,
            "cif":             cif,
            "transactionType": "INTEREST",
            "amount":          str(amount),
            "currency":        currency,
            "description":     description,
            "contractNo":      contract_no,
        }
        try:
            async with httpx.AsyncClient(timeout=self.TIMEOUT) as client:
                resp = await client.post(
                    url,
                    json=payload,
                    headers={"X-Correlation-ID": self._cid()},
                )
                resp.raise_for_status()
                logger.info("Transaction recorded: ref=%s contract=%s amount=%s",
                            reference, contract_no, amount)
                return True
        except httpx.HTTPStatusError as exc:
            # 409 Conflict = duplicate ref (idempotency) — đã xử lý rồi, bỏ qua
            if exc.response.status_code == 409:
                logger.warning("Duplicate transaction ref=%s (already processed)", reference)
                return True
            logger.error("transaction record HTTP error %s: %s", exc.response.status_code, exc)
            return False
        except Exception as exc:
            logger.error("transaction record failed for contract=%s: %s", contract_no, exc)
            return False


transaction_client = TransactionServiceClient()
