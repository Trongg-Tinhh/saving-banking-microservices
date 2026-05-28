export type AccountType = 'PAYMENT' | 'SAVING' | 'LOAN';
export type AccountStatus = 'ACTIVE' | 'BLOCKED' | 'CLOSED';

export interface Account {
  accountNo: string;
  cif: string;
  accountType: AccountType;
  currency: string;
  status: AccountStatus;
  openDate: string;
  branchCode: string;
}

export interface AccountBalance {
  accountNo: string;
  availableBalance: number;
  ledgerBalance: number;
  holdAmount: number;
  currency: string;
  version: number;
}

export interface AccountWithBalance extends Account {
  balance?: AccountBalance;
}

// Used in Open Saving form dropdown
export interface AccountOption {
  accountNo: string;
  currency: string;
  availableBalance: number;
  status: AccountStatus;
  label: string;    // "ACC001001 — 150,000,000 VND"
}

// ── Create Account ─────────────────────────────────────────────────

export interface CreateAccountRequest {
  cif: string;
  accountType: AccountType;
  currency: string;
  branchCode?: string;
  openDate?: string; // YYYY-MM-DD
}

export interface CreatedAccountResponse {
  accountNo: string;
  cif: string;
  accountType: AccountType;
  currency: string;
  status: AccountStatus;
  openDate: string;
  branchCode: string | null;
}
