export type InterestPaymentMethod = 'END_OF_TERM' | 'MONTHLY' | 'QUARTERLY';

export interface SavingTerm {
  termId: string;
  productCode: string;
  termMonths: number;
  termDays: number;
  termLabel: string;
  isActive: boolean;
  annualRate: number;   // current effective rate from interest_rate_configs
}

export interface InterestRate {
  productCode: string;
  termId: string;
  annualRate: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  isActive: boolean;
}

export interface EarlyWithdrawalPolicy {
  productCode: string;
  minDaysHeld: number;
  penaltyRate: number;
  useDemandRate: boolean;
  demandRate: number;
}

export interface SavingProduct {
  productCode: string;
  productName: string;
  currency: string;
  minAmount: number;
  maxAmount: number;
  interestPaymentMethod: InterestPaymentMethod;
  isActive: boolean;
  description?: string;
  terms: SavingTerm[];
  earlyWithdrawalPolicy?: EarlyWithdrawalPolicy;
}

// ── Mutation request types ────────────────────────────────────────

export interface CreateTermRequest {
  termId:     string;
  termMonths: number;
  termDays:   number;
  termLabel:  string;
}

export interface UpdateTermRequest {
  termLabel?: string;
  isActive?:  boolean;
}

export interface CreateRateConfigRequest {
  termId:        string;
  annualRate:    number;
  effectiveFrom: string;       // ISO date "YYYY-MM-DD"
  effectiveTo?:  string | null; // null = open-ended (current rate)
}

export interface UpsertEarlyWithdrawalPolicyRequest {
  minDaysHeld:   number;
  penaltyRate:   number;
  useDemandRate: boolean;
  demandRate:    number;
}

export interface CreateProductRequest {
  productCode:           string;
  productName:           string;
  currency:              string;
  minAmount:             number;
  maxAmount:             number;
  interestPaymentMethod: InterestPaymentMethod;
  description?:          string;
}

export interface UpdateProductRequest {
  productName?:  string;
  minAmount?:    number;
  maxAmount?:    number;
  isActive?:     boolean;
  description?:  string;
}

// Query rate response (from /internal endpoint via Gateway)
export interface ProductRateQueryResponse {
  productCode: string;
  productName: string;
  termId: string;
  termLabel: string;
  termDays: number;
  termMonths: number;
  annualRate: number;
  interestPaymentMethod: InterestPaymentMethod;
  minAmount: number;
  maxAmount: number;
  currency: string;
  effectiveFrom: string;
}
