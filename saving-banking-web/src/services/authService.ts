import api from '@/services/api';
import type { LoginRequest, LoginResponse, UserProfile } from '@/types';
import type { ApiResponse } from '@/types/api.types';

// ─── Auth Service ───────────────────────────────────────────────
// All auth API calls. Keeps API concerns out of components/hooks.

export const authService = {
  /**
   * Login — returns tokens + basic user info from auth-service.
   * Caller is responsible for storing tokens and fetching full profile.
   */
  async login(req: LoginRequest): Promise<LoginResponse> {
    const { data } = await api.post<ApiResponse<LoginResponse>>(
      '/api/v1/auth/login',
      req,
    );
    return data.data ?? (data as unknown as LoginResponse);
  },

  /**
   * Fetch authenticated user's profile (/auth/me).
   * Pass accessToken explicitly during bootstrap (before store is populated).
   */
  async getMe(accessToken?: string): Promise<UserProfile> {
    const headers = accessToken
      ? { Authorization: `Bearer ${accessToken}` }
      : undefined;

    const { data } = await api.get<ApiResponse<UserProfile>>(
      '/api/v1/auth/me',
      { headers },
    );
    return data.data ?? (data as unknown as UserProfile);
  },

  /**
   * Logout — invalidates the refresh token server-side.
   * Fire-and-forget safe: caller should clear local auth regardless of result.
   */
  async logout(refreshToken: string): Promise<void> {
    await api.post('/api/v1/auth/logout', { refreshToken });
  },

  /**
   * Refresh access token — used by Axios interceptor and AuthBootstrapper.
   */
  async refresh(refreshToken: string): Promise<LoginResponse> {
    const { data } = await api.post<ApiResponse<LoginResponse>>(
      '/api/v1/auth/refresh-token',
      { refreshToken },
    );
    return data.data ?? (data as unknown as LoginResponse);
  },
};
