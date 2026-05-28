"""
HTTP client for calling saving-contract-service internal endpoints.

The lifecycle service uses INTERNAL endpoints (no JWT required).
All calls go over the Docker network — never through the public API gateway.
"""
import logging
import uuid

import httpx

from app.config import settings

logger = logging.getLogger(__name__)


class ContractServiceClient:
    """Thin async HTTP wrapper around saving-contract-service."""

    BASE = settings.contract_service_url
    TIMEOUT = httpx.Timeout(15.0, connect=5.0)

    @staticmethod
    def _correlation_id() -> str:
        return str(uuid.uuid4())

    # ── Fetch matured contracts ───────────────────────────────────────────────

    async def get_matured_contracts(self, page: int = 0, size: int = 100) -> list[dict]:
        """
        GET /api/v1/contracts/internal/matured?page=&size=
        Returns list of contract summaries with status=MATURED.
        """
        url = f"{self.BASE}/api/v1/contracts/internal/matured"
        cid = self._correlation_id()
        async with httpx.AsyncClient(timeout=self.TIMEOUT) as client:
            try:
                resp = await client.get(
                    url,
                    params={"page": page, "size": size},
                    headers={"X-Correlation-ID": cid},
                )
                resp.raise_for_status()
                body = resp.json()
                page_data = body.get("data", {})
                # Spring Page response: {"content": [...], "totalElements": ...}
                return page_data.get("content", [])
            except httpx.HTTPStatusError as exc:
                logger.error("get_matured_contracts HTTP error %s: %s", exc.response.status_code, exc)
                return []
            except httpx.RequestError as exc:
                logger.error("get_matured_contracts connection error: %s", exc)
                return []

    # ── Mark a single contract as matured ────────────────────────────────────

    async def mark_matured(self, contract_no: str) -> bool:
        """
        POST /api/v1/contracts/internal/{contractNo}/mark-matured
        Returns True on success.
        """
        url = f"{self.BASE}/api/v1/contracts/internal/{contract_no}/mark-matured"
        cid = self._correlation_id()
        async with httpx.AsyncClient(timeout=self.TIMEOUT) as client:
            try:
                resp = await client.post(url, headers={"X-Correlation-ID": cid})
                resp.raise_for_status()
                logger.info("Marked contract %s as MATURED", contract_no)
                return True
            except httpx.HTTPStatusError as exc:
                logger.warning(
                    "mark_matured %s HTTP error %s: %s",
                    contract_no, exc.response.status_code, exc,
                )
                return False
            except httpx.RequestError as exc:
                logger.error("mark_matured %s connection error: %s", contract_no, exc)
                return False

    # ── Fetch ACTIVE contracts with periodic interest payment ─────────────────

    async def get_periodic_interest_contracts(self,
                                               page: int = 0,
                                               size: int = 500) -> dict:
        """
        GET /api/v1/contracts/internal/periodic-interest-due?page=&size=
        Trả về Spring Page (có field 'content' và 'totalElements').
        """
        url = f"{self.BASE}/api/v1/contracts/internal/periodic-interest-due"
        cid = self._correlation_id()
        async with httpx.AsyncClient(timeout=self.TIMEOUT) as client:
            try:
                resp = await client.get(
                    url,
                    params={"page": page, "size": size},
                    headers={"X-Correlation-ID": cid},
                )
                resp.raise_for_status()
                body = resp.json()
                return body.get("data", {})
            except Exception as exc:
                logger.error("get_periodic_interest_contracts error: %s", exc)
                return {"content": [], "totalElements": 0}

    # ── Fetch ACTIVE contracts due for maturity today ─────────────────────────

    async def get_active_contracts_by_status(self, status: str = "ACTIVE",
                                              page: int = 0, size: int = 100) -> list[dict]:
        """
        GET /api/v1/contracts/status/{status}?page=&size=
        Used to find ACTIVE contracts and filter those past maturityDate.
        """
        url = f"{self.BASE}/api/v1/contracts/status/{status}"
        cid = self._correlation_id()
        async with httpx.AsyncClient(timeout=self.TIMEOUT) as client:
            try:
                resp = await client.get(
                    url,
                    params={"page": page, "size": size},
                    headers={"X-Correlation-ID": cid},
                )
                resp.raise_for_status()
                body = resp.json()
                page_data = body.get("data", {})
                return page_data.get("content", [])
            except Exception as exc:
                logger.error("get_active_contracts error: %s", exc)
                return []


# Singleton
contract_client = ContractServiceClient()
