import { Layout } from 'antd';
import { BankOutlined } from '@ant-design/icons';
import { CONFIG } from '@/constants/config';

interface AuthLayoutProps {
  children: React.ReactNode;
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <Layout
      style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #001529 0%, #003a70 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <div style={{ width: '100%', maxWidth: 420, padding: '0 24px' }}>
        {/* Logo & Brand */}
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div
            style={{
              width: 64,
              height: 64,
              background: '#1677ff',
              borderRadius: '50%',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: 16,
            }}
          >
            <BankOutlined style={{ fontSize: 32, color: '#fff' }} />
          </div>
          <h1 style={{ color: '#fff', margin: 0, fontSize: 22, fontWeight: 700 }}>
            {CONFIG.APP_NAME}
          </h1>
          <p style={{ color: 'rgba(255,255,255,0.65)', margin: '4px 0 0' }}>
            Hệ thống quản lý tiết kiệm ngân hàng
          </p>
        </div>

        {/* Card */}
        <div
          style={{
            background: '#fff',
            borderRadius: 12,
            padding: '32px 32px 24px',
            boxShadow: '0 20px 60px rgba(0,0,0,0.3)',
          }}
        >
          {children}
        </div>

        <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.4)', marginTop: 24, fontSize: 12 }}>
          © 2026 {CONFIG.APP_NAME} v{CONFIG.APP_VERSION}
        </p>
      </div>
    </Layout>
  );
}
