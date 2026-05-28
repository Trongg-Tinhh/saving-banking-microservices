import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { ROUTES } from '@/constants/routes';
import type { UserRole } from '@/types';

interface RoleGuardProps {
  children: React.ReactNode;
  roles: UserRole[];         // Allowed roles
  fallback?: React.ReactNode; // Custom fallback instead of redirect
}

/**
 * Renders children only if user has one of the allowed roles.
 * Otherwise redirects to /403.
 */
export default function RoleGuard({ children, roles, fallback }: RoleGuardProps) {
  const { hasAnyRole } = useAuthStore();

  if (!hasAnyRole(...roles)) {
    return fallback ? <>{fallback}</> : <Navigate to={ROUTES.FORBIDDEN} replace />;
  }

  return <>{children}</>;
}
