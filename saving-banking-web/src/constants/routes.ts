export const ROUTES = {
  // Auth
  LOGIN:    '/login',
  LOGOUT:   '/logout',

  // App
  ROOT:     '/',
  DASHBOARD: '/dashboard',

  // Products
  PRODUCTS:        '/products',
  PRODUCT_CREATE:  '/products/create',
  PRODUCT_EDIT:    '/products/:productCode/edit',
  PRODUCT_DETAIL:  '/products/:productCode',

  // Simulate
  SIMULATE: '/simulate',

  // Contracts
  CONTRACTS:          '/contracts',
  CONTRACT_OPEN:      '/contracts/open',
  CONTRACT_DETAIL:    '/contracts/:contractNo',
  CONTRACT_CLOSE:     '/contracts/:contractNo/close',
  CONTRACT_MATURITY:  '/contracts/:contractNo/maturity',

  // Accounts
  ACCOUNTS:        '/accounts',
  ACCOUNTS_CREATE: '/accounts/create',
  ACCOUNT_DETAIL:  '/accounts/:accountNo',

  // Customers
  CUSTOMERS:       '/customers',
  CUSTOMER_DETAIL: '/customers/:cif',

  // Transactions
  TRANSACTIONS: '/transactions',

  // Notifications
  NOTIFICATIONS: '/notifications',

  // Settings
  SETTINGS: '/settings',

  // Errors
  NOT_FOUND:  '/404',
  FORBIDDEN:  '/403',
} as const;

// Helper: build runtime path (replace :param with actual value)
export function buildPath(route: string, params: Record<string, string>): string {
  return Object.entries(params).reduce(
    (path, [key, val]) => path.replace(`:${key}`, val),
    route
  );
}
