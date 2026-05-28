import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Form, Input, Button, Checkbox, Typography, Divider, Space } from 'antd';
import { UserOutlined, LockOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useLogin } from '@/hooks/useAuth';
import { useAuthStore } from '@/stores/authStore';
import { ROUTES } from '@/constants/routes';
import type { LoginRequest } from '@/types';

const { Text, Link } = Typography;

// ─── Extended form values ───────────────────────────────────────
interface LoginFormValues extends LoginRequest {
  remember?: boolean;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, isInitializing } = useAuthStore();
  const { mutate: login, isPending } = useLogin();
  const [form] = Form.useForm<LoginFormValues>();

  // Redirect to intended page (or dashboard) after successful login
  const from =
    (location.state as { from?: { pathname?: string } })?.from?.pathname ??
    ROUTES.DASHBOARD;

  // Already logged in → skip the login screen
  useEffect(() => {
    if (!isInitializing && isAuthenticated) {
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, isInitializing, navigate, from]);

  function handleFinish(values: LoginFormValues) {
    const { remember: _, ...req } = values;   // strip UI-only field
    login(req, {
      onSuccess: () => navigate(from, { replace: true }),
    });
  }

  return (
    <Space direction="vertical" size={20} style={{ width: '100%' }}>
      {/* Header */}
      <div style={{ textAlign: 'center' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Đăng nhập hệ thống
        </Typography.Title>
        <Text type="secondary">Nhập thông tin tài khoản để tiếp tục</Text>
      </div>

      {/* Form */}
      <Form<LoginFormValues>
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        autoComplete="on"
        size="large"
        initialValues={{ remember: true }}
        requiredMark={false}
      >
        <Form.Item
          name="username"
          label="Tên đăng nhập"
          rules={[{ required: true, message: 'Vui lòng nhập tên đăng nhập' }]}
        >
          <Input
            prefix={<UserOutlined style={{ color: '#bfbfbf' }} />}
            placeholder="Tên đăng nhập"
            autoComplete="username"
            autoFocus
          />
        </Form.Item>

        <Form.Item
          name="password"
          label="Mật khẩu"
          rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}
          style={{ marginBottom: 8 }}
        >
          <Input.Password
            prefix={<LockOutlined style={{ color: '#bfbfbf' }} />}
            placeholder="Mật khẩu"
            autoComplete="current-password"
            onPressEnter={() => form.submit()}
          />
        </Form.Item>

        {/* Remember me */}
        <Form.Item name="remember" valuePropName="checked" style={{ marginBottom: 20 }}>
          <Checkbox>Ghi nhớ đăng nhập</Checkbox>
        </Form.Item>

        <Form.Item style={{ marginBottom: 0 }}>
          <Button
            type="primary"
            htmlType="submit"
            block
            loading={isPending}
            icon={<SafetyCertificateOutlined />}
          >
            {isPending ? 'Đang xác thực...' : 'Đăng nhập'}
          </Button>
        </Form.Item>
      </Form>

      {/* Footer hint */}
      <Divider style={{ margin: '4px 0' }} />
      <Text type="secondary" style={{ fontSize: 12, textAlign: 'center', display: 'block' }}>
        Liên hệ{' '}
        <Link href="mailto:admin@savingbank.vn">quản trị viên</Link>
        {' '}nếu quên mật khẩu
      </Text>
    </Space>
  );
}
