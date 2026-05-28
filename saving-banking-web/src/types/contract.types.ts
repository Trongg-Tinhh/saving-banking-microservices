import type { InterestPaymentMethod } from './product.types';

export type ContractStatus =
  | 'PENDING'
  | 'ACTIVE'
  | 'MATURED'
  | 'CLOSED'
  | 'EARLY_CLOSED'
  | 'CANCELLED'
  | 'FAILED';

export type MaturityInstructionType =
  | 'RENEW_PRINCIPAL'
  | 'RENEW_PRINCIPAL_AND_INTEREST'
  | 'TRANSFER_PRINCIPAL_AND_INTEREST';

export type CloseType = 'MATURITY' | 'EARLY_WITHDRAWAL';

export interface MaturityInstruction {
  instructionType: MaturityInstructionType;
  newTermId?: string | null;
  receivingAccountNo?: string | null;
}

export interface SavingContract {
  contractNo: string;
  cif: string;
  productCode: string;
  productName?: string;
  termId: string;
  termLabel?: string;
  termDays?: number;
  principalAmount: number;
  interestRate: number;
  currency: string;
  openDate: string;
  maturityDate: string;
  status: ContractStatus;
  interestPaymentMethod: InterestPaymentMethod;
  sourceAccountNo: string;
  branchCode?: string;
  openedBy?: string;
  expectedInterest?: number;
  daysRemaining?: number;
  maturityInstruction?: MaturityInstruction;
  version?: number;
}

// ── Open Saving ────────────────────────────────────────────────

export interface OpenSavingRequest {
  cif: string;
  productCode: string;
  termId: string;
  principalAmount: number;
  currency: string;
  sourceAccountNo: string;
  openDate: string;
  maturityInstruction: MaturityInstruction;
}

export interface OpenSavingResponse {
  contractNo: string;
  status: ContractStatus;
  principalAmount: number;
  interestRate: number;
  openDate: string;
  maturityDate: string;
  expectedInterest: number;
  currency: string;
}

// ── Close Saving ───────────────────────────────────────────────

export interface CloseSavingRequest {
  receivingAccountNo: string;
  closeType: CloseType;
  confirmedByUser: boolean;
}

export interface CloseSavingResponse {
  contractNo: string;
  principalAmount: number;
  interestEarned: number;
  earlyWithdrawalPenalty?: number;
  totalPayout: number;
  creditedToAccountNo: string;
  daysHeld: number;
  earlyWithdrawal: boolean;
  closedAt: string;
}

// ── Status History ─────────────────────────────────────────────

export interface ContractStatusHistory {
  fromStatus: ContractStatus | null;
  toStatus: ContractStatus;
  changedBy: string;
  reason: string;
  changedAt: string;
}
