import type { InterestPaymentMethod } from './product.types';

export interface InterestSimulationRequest {
  principal: number;
  annualRate: number;
  termDays: number;
  paymentMethod: InterestPaymentMethod;
}

export interface InterestScheduleItem {
  periodNo: number;
  fromDate: string;
  toDate: string;
  days: number;
  interest: number;
}

export interface InterestSimulationResponse {
  principal: number;
  annualRate: number;
  termDays: number;
  totalInterest: number;
  totalPayout: number;
  maturityDate: string;
  schedule: InterestScheduleItem[];
}
