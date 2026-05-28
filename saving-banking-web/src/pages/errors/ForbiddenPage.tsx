import { Result, Button, Space } from 'antd';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import { useAuthStore } from '@/stores/authStore';

export default function ForbiddenPage() {
  const navigate = useNavigate();
  const { clearAuth } = useAuthStore();

  function handleLogout() {
    clearAuth();
    navigate(ROUTES.LOGIN, { replace: true });
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f5f5f5',
      }}
    >
      <Result
        status="403"
        title="403"
        subTitle="Bạn không có quyền truy cập trang này. Vui lòng liên hệ quản trị viên nếu cần hỗ trợ."
        extra={
          <Space>
            <Button onClick={() => navigate(-1)}>
              Quay lại
            </Button>
            <Button type="primary" onClick={() => navigate(ROUTES.DASHBOARD)}>
              Về trang chủ
            </Button>
            <Button danger onClick={handleLogout}>
              Đăng xuất
            </Button>
          </Space>
        }
      />
    </div>
  );
}
