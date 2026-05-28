"""
Pure interest calculation logic — no I/O or external dependencies.

Formula:
    Simple Interest = P × (R / 100) × (D / 365)
    where:
        P = principal
        R = annual rate (%)
        D = number of days
"""

from decimal import Decimal, ROUND_HALF_UP

DAYS_IN_YEAR = Decimal("365")
MONTHLY_DAYS  = Decimal("30")
QUARTERLY_DAYS = Decimal("91")
SCALE = Decimal("0.000001")   # 6 decimal places during calculation
MONEY = Decimal("0.01")       # Round final monetary values to 2 dp


def simple_interest(principal: Decimal, annual_rate: Decimal, days: int) -> Decimal:
    """
    Core formula: P × (R / 100) × (days / 365)
    Returns value rounded to 6 decimal places.
    """
    return (
        principal
        * (annual_rate / Decimal("100"))
        * (Decimal(days) / DAYS_IN_YEAR)
    ).quantize(SCALE, rounding=ROUND_HALF_UP)


def calculate_full_term(
    principal: Decimal,
    annual_rate: Decimal,
    term_days: int,
) -> Decimal:
    """Interest for a contract held to full maturity."""
    raw = simple_interest(principal, annual_rate, term_days)
    return raw.quantize(MONEY, rounding=ROUND_HALF_UP)


def calculate_early_withdrawal(
    principal: Decimal,
    demand_rate: Decimal,
    days_held: int,
) -> Decimal:
    """Prorated interest at the demand/penalty rate for days actually held."""
    raw = simple_interest(principal, demand_rate, days_held)
    return raw.quantize(MONEY, rounding=ROUND_HALF_UP)


def total_payout(principal: Decimal, interest: Decimal) -> Decimal:
    return (principal + interest).quantize(MONEY, rounding=ROUND_HALF_UP)


def build_projection(
    principal: Decimal,
    annual_rate: Decimal,
    term_days: int,
    payment_method: str,
) -> list[dict]:
    """
    Break the term into payment periods and compute interest per period.

    END_OF_TERM → single period = term_days
    MONTHLY     → periods of 30 days (last period may be shorter)
    QUARTERLY   → periods of 91 days (last period may be shorter)
    """
    if payment_method == "MONTHLY":
        period_len = 30
    elif payment_method == "QUARTERLY":
        period_len = 91
    else:
        # END_OF_TERM — single period
        period_len = term_days

    periods = []
    remaining = term_days
    period_num = 1
    cumulative = Decimal("0")

    while remaining > 0:
        days = min(period_len, remaining)
        interest = simple_interest(principal, annual_rate, days).quantize(MONEY, rounding=ROUND_HALF_UP)
        cumulative = (cumulative + interest).quantize(MONEY, rounding=ROUND_HALF_UP)
        periods.append({
            "period_number":       period_num,
            "days_in_period":      days,
            "interest_amount":     interest,
            "cumulative_interest": cumulative,
        })
        remaining -= days
        period_num += 1

    return periods
