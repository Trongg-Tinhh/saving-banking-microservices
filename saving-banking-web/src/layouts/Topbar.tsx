import { Layout, Button, Avatar, Dropdown, Badge, Space, Typography } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined,
  UserOutlined,
  LogoutOutlined,
  LoadingOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { MenuProps } from 'antd';
import { ROUTES } from '@/constants/routes';
import { useAuthStore } from '@/stores/authStore';
import { useUiStore } from '@/stores/uiStore';
import { useLogout } from '@/hooks/useAuth';
import RoleBadge from '@/components/auth/RoleBadge';

const { Header } = Layout;
const { Text } = Typography;

// Role → avatar background color
const ROLE_COLORS: Record<string, string> = {
  ADMIN:    '#f5222d',
  MANAGER:  '#fa8c16',
  TELLER:   '#1677ff',
  CUSTOMER: '#52c41a',
  SYSTEM:   '#722ed1',
};

function avatarColor(roles: string[]): string {
  const priority = ['ADMIN', 'MANAGER', 'TELLER', 'CUSTOMER', 'SYSTEM'];
  const top = priority.find((r) => roles.includes(r));
  return ROLE_COLORS[top ?? ''] ?? '#8c8c8c';
}

export default function Topbar() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { sidebarCollapsed, toggleSidebar } = useUiStore();
  const { mutate: logout, isPending: isLoggingOut } = useLogout();

  const userRoles = user?.roles ?? [];
  const displayName = user?.fullName ?? user?.username ?? 'User';
  const primaryRole = userRoles[0];

  const userMenuItems: MenuProps['items'] = [
    // ── User info header (non-interactive) ───────────────────────
    {
      key: 'info',
      label: (
        <div style={{ padding: '4px 0', minWidth: 180 }}>
          <Text strong style={{ display: 'block', fontSize: 14 }}>
            {displayName}
          </Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            @{user?.username}
            {user?.cif ? ` · CIF: ${user.cif}` : ''}
          </Text>
          <div style={{ marginTop: 6 }}>
            {userRoles.map((r) => (
              <RoleBadge key={r} role={r} />
            ))}
          </div>
        </div>
      ),
      disabled: true,
    },
    { type: 'divider' },

    // ── Profile ────────────────────────────────────────────────────
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Thông tin tài khoản',
      onClick: () => {
        // Phase 6: user profile drawer
      },
    },

    { type: 'divider' },

    // ── Logout ─────────────────────────────────────────────────────
    {
      key: 'logout',
      icon: isLoggingOut ? <LoadingOutlined /> : <LogoutOutlined />,
      label: isLoggingOut ? 'Đang đăng xuất...' : 'Đăng xuất',
      danger: true,
      disabled: isLoggingOut,
      onClick: () => logout(),
    },
  ];

  return (
    <Header
      style={{
        background: '#fff',
        padding: '0 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottom: '1px solid #f0f0f0',
        position: 'sticky',
        top: 0,
        zIndex: 99,
        boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
        height: 56,
      }}
    >
      {/* Left: Sidebar toggle */}
      <Button
        type="text"
        icon={sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        onClick={toggleSidebar}
        style={{ fontSize: 18, width: 40, height: 40 }}
      />

      {/* Right: Notifications + User menu */}
      <Space size={4}>
        {/* Notification bell */}
        <Badge count={0} size="small" showZero={false}>
          <Button
            type="text"
            icon={<BellOutlined style={{ fontSize: 18 }} />}
            onClick={() => navigate(ROUTES.NOTIFICATIONS)}
            style={{ width: 40, height: 40 }}
          />
        </Badge>

        {/* User dropdown */}
        <Dropdown
          menu={{ items: userMenuItems }}
          placement="bottomRight"
          arrow={{ pointAtCenter: true }}
          trigger={['click']}
        >
          <Space
            style={{
              cursor: 'pointer',
              padding: '4px 8px',
              borderRadius: 8,
              transition: 'background 0.2s',
            }}
            className="topbar-user"
          >
            <Avatar
              icon={<UserOutlined />}
              style={{ backgroundColor: avatarColor(userRoles), flexShrink: 0 }}
              size={32}
            >
              {displayName.charAt(0).toUpperCase()}
            </Avatar>

            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                lineHeight: 1.3,
                maxWidth: 140,
              }}
            >
              <Text
                strong
                style={{ fontSize: 13, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
              >
                {displayName}
              </Text>
              {primaryRole && (
                <span style={{ marginTop: 2 }}>
                  <RoleBadge role={primaryRole} size="small" />
                </span>
              )}
            </div>
          </Space>
        </Dropdown>
      </Space>
    </Header>
  );
}
