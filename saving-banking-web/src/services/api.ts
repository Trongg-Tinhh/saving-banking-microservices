import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { CONFIG } from '@/constants/config';
import { storage } from '@/utils/storage';
import { useAuthStore } from '@/stores/authStore';

// ─── Axios Instance ─────────────────────────────────────────────

export const api = axios.create({
  baseURL: CONFIG.API_BASE_URL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ─── Request Interceptor — Attach JWT ──────────────────────────

api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    config.headers['X-Correlation-ID'] = `WEB-${Date.now()}`;

    return config;
  },
  (error) => Promise.reject(error)
);

// ─── Response Interceptor — Handle 401 + token refresh ─────────

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token!);
  });
  failedQueue = [];
}

api.interceptors.response.use(
  (response) => response,

  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // ── 401 → attempt silent refresh ─────────────────────────
    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = storage.getRefreshToken();

      if (!refreshToken) {
        useAuthStore.getState().clearAuth();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { data } = await axios.post(
          `${CONFIG.API_BASE_URL}/api/v1/auth/refresh-token`,
          { refreshToken }
        );

        const newAccessToken: string = data.data?.accessToken ?? data.accessToken;
        const currentUser = useAuthStore.getState().user;

        if (currentUser) {
          useAuthStore.getState().setAuth(newAccessToken, currentUser);
        }

        if (data.data?.refreshToken) {
          storage.setRefreshToken(data.data.refreshToken as string);
        }

        processQueue(null, newAccessToken);
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);

      } catch (refreshError) {
        processQueue(refreshError, null);
        useAuthStore.getState().clearAuth();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

// ─── Helper: extract readable error message ─────────────────────

export function getApiErrorMessage(error: unknown, fallback = 'Đã có lỗi xảy ra'): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; error?: { details?: string } } | undefined;
    if (data?.message) return data.message;
    if (data?.error?.details) return data.error.details;
    if (error.message === 'Network Error') return 'Không thể kết nối đến server';
    if (error.code === 'ECONNABORTED') return 'Request timeout, vui lòng thử lại';
  }
  return fallback;
}

export default api;
