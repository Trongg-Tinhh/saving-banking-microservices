"""
Interest calculation endpoints.
All endpoints are INTERNAL (no JWT required) — they are pure stateless
calculation services consumed by the Saving Contract Service and the
Saving Lifecycle Service over the Docker network.
"""
from decimal import Decimal

from fastapi import APIRouter, Request

from app import calculator
from app.schemas import (
    EarlyWithdrawalRequest,
    EarlyWithdrawalResponse,
    InterestCalculationRequest,
    InterestCalculationResponse,
    PaymentPeriod,
    ProjectionRequest,
    ProjectionResponse,
)

router = APIRouter(prefix="/api/v1/interest", tags=["Interest Calculation"])


@router.get("/health")
def health():
    """Health check — no auth required."""
    return {"status": "UP", "service": "saving-interest-service"}


@router.post(
    "/calculate",
    response_model=InterestCalculationResponse,
    summary="Calculate interest for a full-term contract",
    description="Pure stateless calculation. No DB access.",
)
def calculate_interest(request: InterestCalculationRequest):
    """
    Returns the total interest and payout for a contract held to full maturity.
    Uses simple interest: P × (R/100) × (D/365).
    """
    interest = calculator.calculate_full_term(
        principal=request.principal_amount,
        annual_rate=request.annual_rate,
        term_days=request.term_days,
    )
    payout = calculator.total_payout(request.principal_amount, interest)

    return InterestCalculationResponse(
        principal_amount=request.principal_amount,
        annual_rate=request.annual_rate,
        term_days=request.term_days,
        interest_payment_method=request.interest_payment_method,
        interest_earned=interest,
        total_payout=payout,
    )


@router.post(
    "/early-withdrawal",
    response_model=EarlyWithdrawalResponse,
    summary="Calculate early-withdrawal interest",
    description="Uses the demand rate and days actually held.",
)
def calculate_early_withdrawal(request: EarlyWithdrawalRequest):
    interest = calculator.calculate_early_withdrawal(
        principal=request.principal_amount,
        demand_rate=request.demand_rate,
        days_held=request.days_held,
    )
    payout = calculator.total_payout(request.principal_amount, interest)

    return EarlyWithdrawalResponse(
        principal_amount=request.principal_amount,
        demand_rate=request.demand_rate,
        days_held=request.days_held,
        interest_earned=interest,
        total_payout=payout,
    )


@router.post(
    "/projection",
    response_model=ProjectionResponse,
    summary="Project interest payment schedule",
    description=(
        "Breaks the term into payment periods (monthly / quarterly / end-of-term) "
        "and returns the interest amount per period. Useful for customer-facing display."
    ),
)
def project_interest(request: ProjectionRequest):
    periods_raw = calculator.build_projection(
        principal=request.principal_amount,
        annual_rate=request.annual_rate,
        term_days=request.term_days,
        payment_method=request.interest_payment_method,
    )
    periods = [PaymentPeriod(**p) for p in periods_raw]

    total_interest = periods[-1].cumulative_interest if periods else Decimal("0")
    total_payout   = calculator.total_payout(request.principal_amount, total_interest)

    return ProjectionResponse(
        principal_amount=request.principal_amount,
        annual_rate=request.annual_rate,
        term_days=request.term_days,
        interest_payment_method=request.interest_payment_method,
        total_interest=total_interest,
        total_payout=total_payout,
        periods=periods,
    )
