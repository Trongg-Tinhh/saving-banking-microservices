import api from '@/services/api';
import type { ApiResponse, PaginatedResponse } from '@/types/api.types';
import type { DashboardSummary, SavingContract, Transaction } from '@/types';

export const dashboardService = {
  /**
   * Aggregate dashboard data from contracts + transactions.
   * No dedicated /api/v1/dashboard endpoint exists — data is composed client-side.
   */
  async getSummary(): Promise<DashboardSummary> {
    const [contractsRes, txRes] = await Promise.allSettled([
      api.get<ApiResponse<PaginatedResponse<SavingContract>>>('/api/v1/contracts', {
        params: { page: 0, size: 100, sort: 'maturityDate,asc' },
      }),
      api.get<ApiResponse<PaginatedResponse<Transaction>>>('/api/v1/transactions', {
        params: { page: 0, size: 10, sort: 'createdAt,desc' },
      }),
    ]);

    const contracts: SavingContract[] =
      contractsRes.status === 'fulfilled'
        ? contractsRes.value.data.data?.content ?? []
        : [];

    const transactions: Transaction[] =
      txRes.status === 'fulfilled'
        ? txRes.value.data.data?.content ?? []
        : [];

    const activeContracts = contracts.filter((c) => c.status === 'ACTIVE');
    const closedContracts = contracts.filter((c) =>
      ['CLOSED', 'EARLY_CLOSED'].includes(c.status),
    );

    const totalPrincipal = activeContracts.reduce(
      (sum, c) => sum + c.principalAmount,
      0,
    );
    const totalInterestExpected = activeContracts.reduce(
      (sum, c) => sum + (c.expectedInterest ?? 0),
      0,
    );

    // Near maturity: active contracts với maturityDate trong vòng 30 ngày tới
    const now      = new Date();
    const in30Days = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);

    const nearMaturity = activeContracts
      .filter((c) => new Date(c.maturityDate) <= in30Days)
      .slice(0, 10)
      .map((c) => {
        const daysRemaining = Math.ceil(
          (new Date(c.maturityDate).getTime() - now.getTime()) / (1000 * 60 * 60 * 24),
        );
        return {
          contractNo:    c.contractNo,
          maturityDate:  c.maturityDate,
          daysRemaining: Math.max(0, daysRemaining),
          principal:     c.principalAmount,
          currency:      c.currency,
        };
      });

    // Map Transaction → RecentTransaction
    // Transaction type dùng transactionId / transactionType (đã align với backend)
    const recentTransactions = transactions.slice(0, 5).map((tx) => ({
      txId:       tx.transactionId,
      txType:     tx.transactionType,
      amount:     tx.amount,
      currency:   tx.currency,
      contractNo: tx.contractNo ?? '',
      createdAt:  tx.createdAt,
    }));

    return {
      summary: {
        totalPrincipal,
        totalContracts:        contracts.length,
        activeContracts:       activeContracts.length,
        closedContracts:       closedContracts.length,
        totalInterestExpected,
      },
      nearMaturity,
      recentTransactions,
    };
  },
};
