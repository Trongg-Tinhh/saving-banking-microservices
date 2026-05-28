import type { TransactionType } from './transaction.types';

export interface DashboardSummary {
  summary: {
    totalPrincipal: number;
    totalContracts: number;
    activeContracts: number;
    closedContracts: number;
    totalInterestExpected: number;
  };
  nearMaturity: NearMaturityItem[];
  recentTransactions: RecentTransaction[];
}

export interface NearMaturityItem {
  contractNo: string;
  maturityDate: string;
  daysRemaining: number;
  principal: number;
  currency: string;
}

export interface RecentTransaction {
  txId: string;
  txType: TransactionType;
  amount: number;
  currency: string;
  contractNo: string;
  createdAt: string;
}
