import { Layout } from 'antd';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import { useUiStore } from '@/stores/uiStore';

const { Content } = Layout;

const SIDEBAR_WIDTH = 240;
const SIDEBAR_COLLAPSED_WIDTH = 64;

export default function MainLayout() {
  const { sidebarCollapsed } = useUiStore();
  const sidebarWidth = sidebarCollapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_WIDTH;

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* Fixed Sidebar */}
      <Sidebar />

      {/* Content area offset by sidebar width */}
      <Layout
        style={{
          marginLeft: sidebarWidth,
          transition: 'margin-left 0.2s',
          minHeight: '100vh',
          background: '#f5f5f5',
        }}
      >
        {/* Sticky Topbar */}
        <Topbar />

        {/* Page Content */}
        <Content
          style={{
            padding: '24px',
            minHeight: 'calc(100vh - 64px)',
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
