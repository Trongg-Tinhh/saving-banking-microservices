import api from '@/services/api';
import type { ApiResponse } from '@/types/api.types';
import type { Account, AccountBalance, CreateAccountRequest, CreatedAccountResponse } from '@/types';

// ─── Account Service ──────────────────────────────────────────────

export const accountService = {
  /** Get all accounts for a customer (by CIF) */
  async getAccountsByCif(cif: string): Promise<Account[]> {
    const { data } = await api.get<ApiResponse<Account[]>>('/api/v1/accounts', {
      params: { cif },
    });
    return data.data ?? [];
  },

  /** Get balance for a specific account */
  async getBalance(accountNo: string): Promise<AccountBalance> {
    const { data } = await api.get<ApiResponse<AccountBalance>>(
      `/api/v1/accounts/${accountNo}/balance`,
    );
    return data.data!;
  },

  /** Create a new bank account (TELLER/ADMIN only) */
  async createAccount(request: CreateAccountRequest): Promise<CreatedAccountResponse> {
    const { data } = await api.post<ApiResponse<CreatedAccountResponse>>(
      '/api/v1/accounts',
      request,
    );
    return data.data!;
  },
};
