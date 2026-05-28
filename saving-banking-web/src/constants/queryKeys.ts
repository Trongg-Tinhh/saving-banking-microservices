// TanStack Query cache keys — centralized to avoid typos and enable targeted invalidation

export const QUERY_KEYS = {
  // Auth
  ME: ['auth', 'me'] as const,

  // Dashboard
  DASHBOARD: ['dashboard'] as const,

  // Products
  PRODUCTS: ['products'] as const,
  PRODUCT: (productCode: string) => ['products', productCode] as const,

  // Interest
  INTEREST_SIMULATE: (params: object) => ['interest', 'simulate', params] as const,

  // Contracts
  CONTRACTS: (filters?: object) => ['contracts', filters ?? {}] as const,
  CONTRACT: (contractNo: string) => ['contracts', contractNo] as const,

  // Transactions
  TRANSACTIONS: (filters?: object) => ['transactions', filters ?? {}] as const,

  // Notifications
  NOTIFICATIONS: (page?: number) => ['notifications', page ?? 0] as const,

  // Accounts
  ACCOUNTS:         (cif?: string)   => ['accounts', cif ?? '']            as const,
  ACCOUNT:          (accountNo: string) => ['accounts', 'detail', accountNo] as const,
  ACCOUNT_BALANCE:  (accountNo: string) => ['accounts', 'balance', accountNo] as const,
} as const;
