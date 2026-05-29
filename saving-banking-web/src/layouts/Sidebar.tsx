import { useEffect, useMemo, useState } from 'react';
import { Layout, Menu } from 'antd';
import {
  DashboardOutlined,
  AppstoreOutlined,
  PlusCircleOutlined,
  FileTextOutlined,
  HistoryOutlined,
  BellOutlined,
  SettingOutlined,
  CalculatorOutlined,
  BankOutlined,
  WalletOutlined,
  TeamOutlined,
  IdcardOutlined,
  UserAddOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import { CONFIG } from '@/constants/config';
import { useUiStore } from '@/stores/uiStore';
import { useAuthStore } from '@/stores/authStore';

const { Sider } = Layout;

// ── Menu items with optional role restriction ─────────────────────

interface MenuItem {
  key: string;
  icon: React.ReactNode;
  label: string;
  /** Only shown for ADMIN / TELLER / MANAGER */
  staffOnly?: boolean;
  /** Only shown for CUSTOMER role */
  customerOnly?: boolean;
}

const ALL_MENU_ITEMS: MenuItem[] = [
  {
    key:   ROUTES.DASHBOARD,
    icon:  <DashboardOutlined />,
    label: 'Dashboard',
  },
  {
    key:   ROUTES.PRODUCTS,
    icon:  <AppstoreOutlined />,
    label: 'Sản phẩm tiết kiệm',
  },
  {
    key:   ROUTES.SIMULATE,
    icon:  <CalculatorOutlined />,
    label: 'Tính lãi dự kiến',
  },
  // CUSTOMER: xem thông tin cá nhân → CustomerSearchPage sẽ tự redirect
  {
    key:          ROUTES.CUSTOMERS,
    icon:         <IdcardOutlined />,
    label:        'Thông tin cá nhân',
    customerOnly: true,
  },
  // Staff: tra cứu bất kỳ khách hàng
  {
    key:       ROUTES.CUSTOMERS,
    icon:      <TeamOutlined />,
    label:     'Tra cứu khách hàng',
    staffOnly: true,
  },
  {
    key:       ROUTES.CUSTOMER_CREATE,
    icon:      <UserAddOutlined />,
    label:     'Tạo khách hàng mới',
    staffOnly: true,
  },
  {
    key:       ROUTES.ACCOUNTS_CREATE,
    icon:      <WalletOutlined />,
    label:     'Tạo tài khoản',
    staffOnly: true,   // ADMIN / TELLER / MANAGER only
  },
  {
    key:   ROUTES.CONTRACT_OPEN,
    icon:  <PlusCircleOutlined />,
    label: 'Mở sổ tiết kiệm',
  },
  {
    key:   ROUTES.CONTRACTS,
    icon:  <FileTextOutlined />,
    label: 'Sổ tiết kiệm của tôi',
  },
  {
    key:   ROUTES.TRANSACTIONS,
    icon:  <HistoryOutlined />,
    label: 'Lịch sử giao dịch',
  },
  {
    key:   ROUTES.NOTIFICATIONS,
    icon:  <BellOutlined />,
    label: 'Thông báo',
  },
  {
    key:   ROUTES.SETTINGS,
    icon:  <SettingOutlined />,
    label: 'Cài đặt',
  },
];

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { sidebarCollapsed } = useUiStore();
  const { hasAnyRole } = useAuthStore();
  const [selectedKey, setSelectedKey] = useState(location.pathname);

  // Filter menu items based on role, then strip custom props for Ant Design Menu
  const menuItems = useMemo(() => {
    const isStaff    = hasAnyRole('ADMIN', 'TELLER', 'MANAGER');
    const isCustomer = hasAnyRole('CUSTOMER');
    return ALL_MENU_ITEMS
      .filter((item) => {
        if (item.staffOnly    && !isStaff)    return false;
        if (item.customerOnly && !isCustomer) return false;
        return true;
      })
      .map(({ key, icon, label }) => ({ key, icon, label }));
  }, [hasAnyRole]);

  // Sync selected menu item with current route
  useEffect(() => {
    // Match longest prefix (for nested routes like /contracts/:no)
    const matched = menuItems.map((i) => i.key)
      .filter((key) => location.pathname.startsWith(key))
      .sort((a, b) => b.length - a.length)[0];
    setSelectedKey(matched ?? location.pathname);
  }, [location.pathname, menuItems]);

  return (
    <Sider
      trigger={null}
      collapsible
      collapsed={sidebarCollapsed}
      width={240}
      collapsedWidth={64}
      style={{
        background: '#001529',
        position: 'fixed',
        left: 0,
        top: 0,
        bottom: 0,
        zIndex: 100,
        overflow: 'auto',
      }}
    >
      {/* Logo */}
      <div
        style={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          justifyContent: sidebarCollapsed ? 'center' : 'flex-start',
          padding: sidebarCollapsed ? 0 : '0 20px',
          borderBottom: '1px solid rgba(255,255,255,0.08)',
          gap: 10,
          cursor: 'pointer',
          transition: 'all 0.2s',
        }}
        onClick={() => navigate(ROUTES.DASHBOARD)}
      >
        <BankOutlined style={{ fontSize: 22, color: '#1677ff', flexShrink: 0 }} />
        {!sidebarCollapsed && (
          <span
            style={{
              color: '#fff',
              fontWeight: 700,
              fontSize: 14,
              lineHeight: '20px',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
            }}
          >
            {CONFIG.APP_NAME}
          </span>
        )}
      </div>

      {/* Navigation */}
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[selectedKey]}
        items={menuItems}
        style={{ borderRight: 0, marginTop: 8 }}
        onClick={({ key }) => navigate(key)}
      />
    </Sider>
  );
}
