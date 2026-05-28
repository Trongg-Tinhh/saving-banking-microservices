import { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ConfigProvider, App as AntApp, theme } from 'antd';
import viVN from 'antd/locale/vi_VN';
import { router } from '@/routes';
import { useAuthStore } from '@/stores/authStore';
import { storage } from '@/utils/storage';
import { authService } from '@/services/authService';

// ─── TanStack Query Client ──────────────────────────────────────

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,      // 5 minutes
      gcTime:    10 * 60 * 1000,      // 10 minutes (formerly cacheTime)
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
});

// ─── Ant Design Theme ───────────────────────────────────────────

const antTheme = {
  token: {
    colorPrimary: '#1677ff',
    borderRadius: 8,
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  },
  algorithm: theme.defaultAlgorithm,
};

// ─── Auth Bootstrapper ──────────────────────────────────────────
// Runs once on app load to restore session from stored refresh token

function AuthBootstrapper({ children }: { children: React.ReactNode }) {
  const { setAuth, setInitializing, clearAuth } = useAuthStore();

  useEffect(() => {
    async function bootstrap() {
      const refreshToken = storage.getRefreshToken();

      if (!refreshToken) {
        setInitializing(false);
        return;
      }

      try {
        // Exchange refresh token → new access token
        const result = await authService.refresh(refreshToken);

        if (result.accessToken) {
          storage.setRefreshToken(result.refreshToken ?? refreshToken);

          // Fetch full user profile
          const user = await authService.getMe(result.accessToken);
          setAuth(result.accessToken, user);
        } else {
          clearAuth();
        }
      } catch {
        // Refresh token expired or revoked
        clearAuth();
      } finally {
        setInitializing(false);
      }
    }

    bootstrap();
  }, [setAuth, setInitializing, clearAuth]);

  return <>{children}</>;
}

// ─── Root App ──────────────────────────────────────────────────

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={viVN} theme={antTheme}>
        <AntApp>
          <AuthBootstrapper>
            <RouterProvider router={router} />
          </AuthBootstrapper>
        </AntApp>
      </ConfigProvider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
