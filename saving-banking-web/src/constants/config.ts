export const CONFIG = {
  APP_NAME:    import.meta.env.VITE_APP_NAME    ?? 'Saving Banking System',
  APP_VERSION: import.meta.env.VITE_APP_VERSION ?? '1.0.0',
  API_BASE_URL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:3000',

  // Token storage keys
  STORAGE_KEYS: {
    REFRESH_TOKEN: 'sbs_refresh_token',
    USER_PROFILE:  'sbs_user_profile',
  },

  // Pagination defaults
  PAGE_SIZE:      20,
  PAGE_SIZE_OPTS: [10, 20, 50, 100],

  // Locale
  LOCALE: 'vi-VN',
  CURRENCY: 'VND',
  DATE_FORMAT: 'DD/MM/YYYY',
  DATETIME_FORMAT: 'DD/MM/YYYY HH:mm',
} as const;
