import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { App } from 'antd';
import { authService } from '@/services/authService';
import { useAuthStore } from '@/stores/authStore';
import { storage } from '@/utils/storage';
import { ROUTES } from '@/constants/routes';
import { getApiErrorMessage } from '@/services/api';
import type { LoginRequest } from '@/types';

// ─── useLogin ───────────────────────────────────────────────────

/**
 * React Query mutation for login.
 *
 * Usage:
 *   const { mutate: login, isPending } = useLogin();
 *   login({ username, password }, { onSuccess: () => navigate(from) });
 */
export function useLogin() {
  const { setAuth } = useAuthStore();
  const { message } = App.useApp();

  return useMutation({
    mutationFn: async (req: LoginRequest) => {
      // 1. Login → get tokens
      const result = await authService.login(req);
      storage.setRefreshToken(result.refreshToken);

      // 2. Fetch full user profile with new access token
      const user = await authService.getMe(result.accessToken);

      return { result, user };
    },

    onSuccess: ({ result, user }) => {
      setAuth(result.accessToken, user);
      message.success(`Chào mừng, ${user.fullName ?? user.username}!`);
    },

    onError: (error) => {
      const msg = getApiErrorMessage(error, 'Tên đăng nhập hoặc mật khẩu không đúng');
      message.error(msg);
    },
  });
}

// ─── useLogout ──────────────────────────────────────────────────

/**
 * React Query mutation for logout.
 * Invalidates refresh token server-side then clears local auth state.
 *
 * Usage:
 *   const { mutate: logout, isPending } = useLogout();
 */
export function useLogout() {
  const { clearAuth } = useAuthStore();
  const navigate = useNavigate();
  const { message } = App.useApp();

  return useMutation({
    mutationFn: async () => {
      const rt = storage.getRefreshToken();
      if (rt) {
        // Fire-and-forget: server-side invalidation is best-effort
        await authService.logout(rt).catch(() => {});
      }
    },

    onSettled: () => {
      // Always clear local state regardless of server response
      clearAuth();
      navigate(ROUTES.LOGIN, { replace: true });
      message.info('Đã đăng xuất');
    },
  });
}

// ─── useCurrentUser ─────────────────────────────────────────────

/** Convenience selector — subscribes to authStore user. */
export function useCurrentUser() {
  return useAuthStore((s) => s.user);
}

/** Convenience selector — returns all role helpers. */
export function useRoles() {
  return useAuthStore((s) => ({
    roles:       s.user?.roles ?? [],
    isAdmin:     s.isAdmin(),
    isTeller:    s.isTeller(),
    isCustomer:  s.isCustomer(),
    hasRole:     s.hasRole,
    hasAnyRole:  s.hasAnyRole,
  }));
}
