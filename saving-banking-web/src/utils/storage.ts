import { CONFIG } from '@/constants/config';

export const storage = {
  getRefreshToken(): string | null {
    return localStorage.getItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN);
  },
  setRefreshToken(token: string): void {
    localStorage.setItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN, token);
  },
  removeRefreshToken(): void {
    localStorage.removeItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN);
  },

  clearAll(): void {
    Object.values(CONFIG.STORAGE_KEYS).forEach((key) => {
      localStorage.removeItem(key);
    });
  },
};
