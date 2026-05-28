import { useQuery } from '@tanstack/react-query';
import { transactionService, type TransactionListFilter } from '@/services/transactionService';
import { QUERY_KEYS } from '@/constants/queryKeys';

export function useTransactions(filters: TransactionListFilter = {}) {
  return useQuery({
    queryKey: QUERY_KEYS.TRANSACTIONS(filters),
    queryFn: () => transactionService.listTransactions(filters),
    staleTime: 30_000,
  });
}
