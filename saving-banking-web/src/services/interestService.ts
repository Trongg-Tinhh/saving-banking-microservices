import api from '@/services/api';
import type {
  InterestSimulationRequest,
  InterestSimulationResponse,
  InterestScheduleItem,
} from '@/types';
import { dayjs } from '@/utils/formatDate';

// ─── FastAPI snake_case shapes ───────────────────────────────────

interface FastApiRequest {
  principal_amount: number;
  annual_rate: number;
  term_days: number;
  interest_payment_method: string;
}

interface FastApiPeriod {
  period_number: number;
  days_in_period: number;
  interest_amount: number;
  cumulative_interest: number;
}

// /projection response — contains everything we need
interface FastApiProjectionResponse {
  principal_amount: number;
  annual_rate: number;
  term_days: number;
  interest_payment_method: string;
  total_interest: number;
  total_payout: number;
  periods: FastApiPeriod[];
}

// ─── Interest Service ─────────────────────────────────────────────

export const interestService = {
  /**
   * Simulate interest for a saving deposit.
   * Calls FastAPI POST /api/v1/interest/projection (single call).
   * maturityDate is computed client-side from startDate + termDays.
   *
   * @param req        Simulation parameters (camelCase)
   * @param startDate  ISO date string, e.g. "2026-05-27"
   */
  async simulate(
    req: InterestSimulationRequest,
    startDate: string,
  ): Promise<InterestSimulationResponse> {
    const body: FastApiRequest = {
      principal_amount:        req.principal,
      annual_rate:             req.annualRate,
      term_days:               req.termDays,
      interest_payment_method: req.paymentMethod,
    };

    const projRes = await api.post<FastApiProjectionResponse>(
      '/api/v1/interest/projection',
      body,
    );

    const proj = projRes.data;

    // Compute maturityDate client-side (API does not return it)
    const maturityDate = dayjs(startDate)
      .add(req.termDays, 'day')
      .format('YYYY-MM-DD');

    // Build schedule with computed dates
    let cursor = dayjs(startDate);
    const schedule: InterestScheduleItem[] = (proj.periods ?? []).map((p) => {
      const fromDate = cursor.format('YYYY-MM-DD');
      const toDate   = cursor.add(p.days_in_period, 'day').format('YYYY-MM-DD');
      cursor = dayjs(toDate);
      return {
        periodNo: p.period_number,
        fromDate,
        toDate,
        days:     p.days_in_period,
        interest: Number(p.interest_amount),
      };
    });

    return {
      principal:     req.principal,
      annualRate:    req.annualRate,
      termDays:      req.termDays,
      totalInterest: Number(proj.total_interest),
      totalPayout:   Number(proj.total_payout),
      maturityDate,
      schedule,
    };
  },
};
