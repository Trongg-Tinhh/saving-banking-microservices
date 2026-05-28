from decimal import Decimal
from typing import Optional
from pydantic import BaseModel, Field, field_validator


# ── Requests ─────────────────────────────────────────────────────────────────

class InterestCalculationRequest(BaseModel):
    """Calculate interest for a saving contract."""

    principal_amount: Decimal = Field(..., gt=0, description="Principal amount deposited")
    annual_rate: Decimal      = Field(..., ge=0, description="Annual interest rate in percent (e.g. 6.5 = 6.5%)")
    term_days: int            = Field(..., gt=0, description="Contract duration in days")
    interest_payment_method: str = Field(
        default="END_OF_TERM",
        description="END_OF_TERM | MONTHLY | QUARTERLY"
    )

    @field_validator("interest_payment_method")
    @classmethod
    def validate_method(cls, v: str) -> str:
        allowed = {"END_OF_TERM", "MONTHLY", "QUARTERLY"}
        if v not in allowed:
            raise ValueError(f"interest_payment_method must be one of {allowed}")
        return v


class EarlyWithdrawalRequest(BaseModel):
    """Calculate early-withdrawal interest."""

    principal_amount: Decimal = Field(..., gt=0)
    demand_rate: Decimal      = Field(..., ge=0, description="Demand (penalty) annual rate in percent")
    days_held: int            = Field(..., ge=0, description="Days the contract was held before closure")


class ProjectionRequest(BaseModel):
    """Project interest payments over the full term (useful for MONTHLY/QUARTERLY display)."""

    principal_amount: Decimal = Field(..., gt=0)
    annual_rate: Decimal      = Field(..., ge=0)
    term_days: int            = Field(..., gt=0)
    interest_payment_method: str = Field(default="END_OF_TERM")


# ── Responses ─────────────────────────────────────────────────────────────────

class InterestCalculationResponse(BaseModel):
    principal_amount: Decimal
    annual_rate: Decimal
    term_days: int
    interest_payment_method: str
    interest_earned: Decimal
    total_payout: Decimal


class EarlyWithdrawalResponse(BaseModel):
    principal_amount: Decimal
    demand_rate: Decimal
    days_held: int
    interest_earned: Decimal
    total_payout: Decimal


class PaymentPeriod(BaseModel):
    period_number: int
    days_in_period: int
    interest_amount: Decimal
    cumulative_interest: Decimal


class ProjectionResponse(BaseModel):
    principal_amount: Decimal
    annual_rate: Decimal
    term_days: int
    interest_payment_method: str
    total_interest: Decimal
    total_payout: Decimal
    periods: list[PaymentPeriod]
