import { useEffect } from 'react';
import {
  Typography,
  Card,
  Button,
  Input,
  Form,
  Space,
  Divider,
  Spin,
} from 'antd';
import { SearchOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { ROUTES, buildPath } from '@/constants/routes';
import { useAuthStore } from '@/stores/authStore';

const { Title, Text } = Typography;

export default function CustomerSearchPage() {
  const navigate = useNavigate();
  const { isCustomer, user } = useAuthStore();
  const [form] = Form.useForm<{ cif: string }>();

  // CUSTOMER → redirect thẳng đến trang thông tin của chính họ
  useEffect(() => {
    if (isCustomer() && user?.cif) {
      navigate(buildPath(ROUTES.CUSTOMER_DETAIL, { cif: user.cif }), { replace: true });
    }
  }, [isCustomer, user?.cif, navigate]);

  // Hiển thị loading khi đang redirect cho CUSTOMER
  if (isCustomer() && user?.cif) {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Spin size="large" />
      </div>
    );
  }

  const handleSearch = (values: { cif: string }) => {
    const cif = values.cif?.trim();
    if (cif) navigate(buildPath(ROUTES.CUSTOMER_DETAIL, { cif }));
  };

  return (
    <div style={{ maxWidth: 560, margin: '60px auto' }}>
      <div style={{ textAlign: 'center', marginBottom: 32 }}>
        <div
          style={{
            width: 72, height: 72, borderRadius: '50%',
            background: '#e6f4ff', display: 'flex',
            alignItems: 'center', justifyContent: 'center',
            fontSize: 32, color: '#1677ff', margin: '0 auto 16px',
          }}
        >
          <UserOutlined />
        </div>
        <Title level={3} style={{ margin: 0 }}>Tra cứu khách hàng</Title>
        <Text type="secondary">Nhập mã CIF để xem và cập nhật thông tin</Text>
      </div>

      <Card style={{ borderRadius: 12 }}>
        <Form form={form} layout="vertical" size="large" onFinish={handleSearch}>
          <Form.Item
            name="cif"
            label="Mã khách hàng (CIF)"
            rules={[
              { required: true, message: 'Nhập mã CIF' },
              { min: 3, message: 'CIF phải có ít nhất 3 ký tự' },
            ]}
          >
            <Input prefix={<UserOutlined />} placeholder="VD: CIF0001" autoFocus allowClear />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />} block>
              Tra cứu
            </Button>
          </Form.Item>
        </Form>

        <Divider plain>Truy cập nhanh</Divider>
        <Space wrap>
          {['CIF0001', 'CIF0002', 'CIF0003', 'CIF0004'].map((cif) => (
            <Button
              key={cif}
              size="small"
              onClick={() => navigate(buildPath(ROUTES.CUSTOMER_DETAIL, { cif }))}
            >
              {cif}
            </Button>
          ))}
        </Space>
      </Card>
    </div>
  );
}
