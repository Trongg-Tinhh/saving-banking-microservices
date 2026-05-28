import api from '@/services/api';
import type { ApiResponse, PaginatedResponse, PaginationParams } from '@/types/api.types';
import type { Transaction, TransactionFilter } from '@/types';

export interface TransactionListFilter extends TransactionFilter, PaginationParams {}

export const transactionService = {
  async listTransactions(
    filters: TransactionListFilter = {},
  ): Promise<PaginatedResponse<Transaction>> {
    const params: Record<string, unknown> = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
      sort: filters.sort ?? 'createdAt,desc',
    };
    if (filters.contractNo) params.contractNo = filters.contractNo;
    if (filters.cif)        params.cif        = filters.cif;
    if (filters.txType)     params.txType     = filters.txType;
    if (filters.status)     params.status     = filters.status;
    if (filters.fromDate)   params.fromDate   = filters.fromDate;
    if (filters.toDate)     params.toDate     = filters.toDate;

    const { data } = await api.get<ApiResponse<PaginatedResponse<Transaction>>>(
      '/api/v1/transactions',
      { params },
    );
    return (
      data.data ?? {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        numberOfElements: 0,
        first: true,
        last: true,
        empty: true,
      }
    );
  },
};
