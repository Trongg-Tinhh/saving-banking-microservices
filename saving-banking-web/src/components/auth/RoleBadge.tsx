import { Tag } from 'antd';
import type { UserRole } from '@/types';

interface RoleBadgeProps {
  role: UserRole;
  size?: 'small' | 'default';
}

const ROLE_CONFIG: Record<UserRole, { label: string; color: string }> = {
  ADMIN:    { label: 'Quản trị viên', color: 'red'     },
  MANAGER:  { label: 'Quản lý',       color: 'orange'  },
  TELLER:   { label: 'Giao dịch viên', color: 'blue'   },
  CUSTOMER: { label: 'Khách hàng',    color: 'green'   },
  SYSTEM:   { label: 'Hệ thống',      color: 'purple'  },
};

export default function RoleBadge({ role, size = 'small' }: RoleBadgeProps) {
  const config = ROLE_CONFIG[role] ?? { label: role, color: 'default' };

  return (
    <Tag
      color={config.color}
      style={{
        margin: 0,
        fontSize: size === 'small' ? 10 : 12,
        lineHeight: size === 'small' ? '16px' : '20px',
        padding: size === 'small' ? '0 4px' : '0 7px',
        borderRadius: 4,
      }}
    >
      {config.label}
    </Tag>
  );
}

/** Render a stack of badges for all roles (some users have multiple). */
export function RoleBadgeList({ roles }: { roles: UserRole[] }) {
  return (
    <span style={{ display: 'inline-flex', gap: 4, flexWrap: 'wrap' }}>
      {roles.map((r) => (
        <RoleBadge key={r} role={r} />
      ))}
    </span>
  );
}
