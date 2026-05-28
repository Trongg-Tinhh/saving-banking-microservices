export default () => ({
  port: parseInt(process.env.PORT ?? '3000', 10),
  jwtSecret: process.env.JWT_SECRET ?? 'your-super-secret-jwt-key-must-be-at-least-256-bits-long-for-hs256',

  services: {
    auth:         process.env.AUTH_SERVICE_URL         ?? 'http://auth-service:8081',
    customer:     process.env.CUSTOMER_SERVICE_URL     ?? 'http://customer-service:8082',
    account:      process.env.ACCOUNT_SERVICE_URL      ?? 'http://account-service:8083',
    product:      process.env.PRODUCT_SERVICE_URL      ?? 'http://saving-product-service:8084',
    contract:     process.env.CONTRACT_SERVICE_URL     ?? 'http://saving-contract-service:8085',
    transaction:  process.env.TRANSACTION_SERVICE_URL  ?? 'http://saving-transaction-service:8086',
    interest:     process.env.INTEREST_SERVICE_URL     ?? 'http://saving-interest-service:8087',
    lifecycle:    process.env.LIFECYCLE_SERVICE_URL    ?? 'http://saving-lifecycle-service:8088',
    notification: process.env.NOTIFICATION_SERVICE_URL ?? 'http://saving-notification-service:8089',
  },

  rateLimit: {
    ttl:   parseInt(process.env.RATE_LIMIT_TTL   ?? '60000', 10),  // ms
    limit: parseInt(process.env.RATE_LIMIT_MAX   ?? '100',   10),   // requests per TTL
  },
});
