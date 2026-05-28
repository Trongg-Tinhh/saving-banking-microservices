import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { accountService } from '@/services/accountService';
import { QUERY_KEYS } from '@/constants/queryKeys';
import type { AccountOption, CreateAccountRequest } from '@/types';
import { formatVND } from '@/utils/formatCurrency';

// ─── Accounts by CIF ──────────────────────────────────────────────

export function useAccountsByCif(cif: string | undefined) {
  return useQuery({
    queryKey: QUERY_KEYS.ACCOUNTS(cif),
    queryFn: () => accountService.getAccountsByCif(cif!),
    enabled: Boolean(cif) && cif!.trim().length > 0,
    staleTime: 60_000,
    select: (accounts): AccountOption[] =>
      accounts
        .filter((a) => a.status === 'ACTIVE')
        .map((a) => ({
          accountNo: a.accountNo,
          currency: a.currency,
          availableBalance: 0, // balance is fetched separately
          status: a.status,
          label: `${a.accountNo} (${a.currency})`,
        })),
  });
}

// ─── Account Balance ──────────────────────────────────────────────

export function useAccountBalance(accountNo: string | undefined) {
  return useQuery({
    queryKey: QUERY_KEYS.ACCOUNT_BALANCE(accountNo ?? ''),
    queryFn: () => accountService.getBalance(accountNo!),
    enabled: Boolean(accountNo),
    staleTime: 30_000,
    select: (data) => ({
      ...data,
      formattedBalance: formatVND(data.availableBalance),
    }),
  });
}

// ─── Create Account ───────────────────────────────────────────────

export function useCreateAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: CreateAccountRequest) => accountService.createAccount(req),
    onSuccess: (_, req) => {
      // Invalidate account list so the new account appears immediately
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.ACCOUNTS(req.cif) });
    },
  });
}
