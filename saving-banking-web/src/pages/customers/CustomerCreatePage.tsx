import { useState } from 'react';
import {
  Typography,
  Card,
  Button,
  Form,
  Input,
  Select,
  DatePicker,
  Steps,
  Row,
  Col,
  Divider,
  Alert,
  Space,
  Result,
  Tag,
} from 'antd';
import {
  ArrowLeftOutlined,
  UserAddOutlined,
  IdcardOutlined,
  LockOutlined,
  CheckCircleOutlined,
  PhoneOutlined,
  MailOutlined,
  EnvironmentOutlined,
  EyeInvisibleOutlined,
  EyeTwoTone,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { dayjs } from '@/utils/formatDate';
import { ROUTES, buildPath } from '@/constants/routes';
import { useCreateCustomer } from '@/hooks/useCustomers';
import { useCreateAuthUser } from '@/hooks/useAuthAdmin';
import type { Customer, CreateAuthUserResponse } from '@/types';

const { Title, Text } = Typography;

// ── Step 1 form values ────────────────────────────────────────────
interface CustomerFormValues {
  fullName:     string;
  dateOfBirth?: ReturnType<typeof dayjs>;
  gender?:      'MALE' | 'FEMALE' | 'OTHER';
  nationality:  string;
  idType:       'NATIONAL_ID' | 'PASSPORT' | 'DRIVER_LICENSE' | 'MILITARY_ID';
  idNumber:     string;
  phoneNumber?: string;
  email?:       string;
  address?:     string;
  district?:    string;
  city?:        string;
}

// ── Step 2 form values ────────────────────────────────────────────
interface AccountFormValues {
  username:        string;
  password:        string;
  confirmPassword: string;
}

const ID_TYPE_OPTIONS = [
  { value: 'NATIONAL_ID',    label: 'CCCD / CMND' },
  { value: 'PASSPORT',       label: 'Hộ chiếu' },
  { value: 'DRIVER_LICENSE', label: 'Bằng lái xe' },
  { value: 'MILITARY_ID',    label: 'Thẻ quân nhân' },
];

// ── Page ──────────────────────────────────────────────────────────

export default function CustomerCreatePage() {
  const navigate = useNavigate();

  const [currentStep, setCurrentStep]           = useState(0);
  const [createdCustomer, setCreatedCustomer]   = useState<Customer | null>(null);
  const [createdUser, setCreatedUser]           = useState<CreateAuthUserResponse | null>(null);

  const [form1] = Form.useForm<CustomerFormValues>();
  const [form2] = Form.useForm<AccountFormValues>();

  const createCustomer = useCreateCustomer();
  const createAuthUser = useCreateAuthUser();

  // ── Step 1: tạo khách hàng ────────────────────────────────────

  const handleStep1 = async () => {
    const values = await form1.validateFields();

    const customer = await createCustomer.mutateAsync({
      fullName:    values.fullName,
      dateOfBirth: values.dateOfBirth?.format('YYYY-MM-DD'),
      gender:      values.gender,
      nationality: values.nationality,
      idType:      values.idType,
      idNumber:    values.idNumber,
      primaryContact: {
        phoneNumber: values.phoneNumber || undefined,
        email:       values.email       || undefined,
        address:     values.address     || undefined,
        district:    values.district    || undefined,
        city:        values.city        || undefined,
        isPrimary:   true,
      },
    });

    setCreatedCustomer(customer);
    setCurrentStep(1);
  };

  // ── Step 2: tạo tài khoản đăng nhập ──────────────────────────

  const handleStep2 = async () => {
    const values = await form2.validateFields();

    const user = await createAuthUser.mutateAsync({
      username: values.username,
      password: values.password,
      cif:      createdCustomer!.cif,
    });

    setCreatedUser(user);
    setCurrentStep(2);
  };

  // ── Success ───────────────────────────────────────────────────

  if (currentStep === 2 && createdCustomer && createdUser) {
    return (
      <div style={{ maxWidth: 560, margin: '0 auto' }}>
        <Result
          status="success"
          icon={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
          title="Tạo khách hàng thành công!"
          subTitle={
            <Space direction="vertical" size={6} style={{ textAlign: 'left', width: '100%' }}>
              <div>
                <Text type="secondary">CIF: </Text>
                <Text strong copyable style={{ fontSize: 15, fontFamily: 'monospace' }}>
                  {createdCustomer.cif}
                </Text>
              </div>
              <div>
                <Text type="secondary">Họ tên: </Text>
                <Text strong>{createdCustomer.fullName}</Text>
              </div>
              <div>
                <Text type="secondary">Tài khoản đăng nhập: </Text>
                <Text strong copyable style={{ fontFamily: 'monospace' }}>
                  {createdUser.username}
                </Text>
              </div>
              <div>
                <Text type="secondary">Trạng thái KYC: </Text>
                <Tag color="orange">PENDING — chờ xác minh</Tag>
              </div>
            </Space>
          }
          extra={[
            <Button
              key="detail"
              type="primary"
              onClick={() => navigate(buildPath(ROUTES.CUSTOMER_DETAIL, { cif: createdCustomer.cif }))}
            >
              Xem chi tiết KH
            </Button>,
            <Button
              key="new"
              onClick={() => {
                setCreatedCustomer(null);
                setCreatedUser(null);
                setCurrentStep(0);
                form1.resetFields();
                form2.resetFields();
              }}
            >
              Tạo khách hàng khác
            </Button>,
            <Button key="list" onClick={() => navigate(ROUTES.CUSTOMERS)}>
              Về danh sách KH
            </Button>,
          ]}
        />
      </div>
    );
  }

  // ── Wizard ────────────────────────────────────────────────────

  return (
    <div style={{ maxWidth: 700, margin: '0 auto' }}>

      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(ROUTES.CUSTOMERS)}
          style={{ marginBottom: 16 }}
        >
          Danh sách khách hàng
        </Button>
        <Title level={4} style={{ margin: 0 }}>
          <UserAddOutlined style={{ marginRight: 8, color: '#1677ff' }} />
          Tạo khách hàng mới
        </Title>
        <Text type="secondary">
          Điền thông tin cá nhân và tạo tài khoản đăng nhập cho khách hàng.
        </Text>
      </div>

      {/* Steps indicator */}
      <Steps
        current={currentStep}
        style={{ marginBottom: 32 }}
        items={[
          { title: 'Thông tin cá nhân', icon: <IdcardOutlined /> },
          { title: 'Tài khoản đăng nhập', icon: <LockOutlined /> },
        ]}
      />

      {/* ── STEP 1: Customer info ─────────────────────────────── */}
      {currentStep === 0 && (
        <Card style={{ borderRadius: 12 }}>
          <Form
            form={form1}
            layout="vertical"
            size="large"
            initialValues={{ nationality: 'VN', idType: 'NATIONAL_ID' }}
          >
            {/* Thông tin cơ bản */}
            <Divider plain style={{ marginTop: 0 }}>
              <Space><IdcardOutlined /> Thông tin cơ bản</Space>
            </Divider>

            <Form.Item
              name="fullName"
              label="Họ và tên"
              rules={[
                { required: true, message: 'Nhập họ và tên' },
                { max: 200, message: 'Tối đa 200 ký tự' },
              ]}
            >
              <Input placeholder="Nguyễn Văn An" />
            </Form.Item>

            <Row gutter={16}>
              <Col xs={24} sm={12}>
                <Form.Item name="dateOfBirth" label="Ngày sinh">
                  <DatePicker
                    style={{ width: '100%' }}
                    format="DD/MM/YYYY"
                    placeholder="Chọn ngày sinh"
                    disabledDate={(d) => d.isAfter(dayjs())}
                  />
                </Form.Item>
              </Col>
              <Col xs={24} sm={12}>
                <Form.Item name="gender" label="Giới tính">
                  <Select
                    placeholder="Chọn giới tính"
                    options={[
                      { value: 'MALE',   label: 'Nam' },
                      { value: 'FEMALE', label: 'Nữ' },
                      { value: 'OTHER',  label: 'Khác' },
                    ]}
                  />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col xs={24} sm={10}>
                <Form.Item
                  name="idType"
                  label="Loại giấy tờ"
                  rules={[{ required: true, message: 'Chọn loại giấy tờ' }]}
                >
                  <Select options={ID_TYPE_OPTIONS} />
                </Form.Item>
              </Col>
              <Col xs={24} sm={14}>
                <Form.Item
                  name="idNumber"
                  label="Số giấy tờ"
                  rules={[
                    { required: true, message: 'Nhập số giấy tờ' },
                    { max: 50, message: 'Tối đa 50 ký tự' },
                  ]}
                >
                  <Input placeholder="079099012345" maxLength={50} />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item name="nationality" label="Quốc tịch">
              <Select
                showSearch
                options={[
                  { value: 'VN', label: '🇻🇳 Việt Nam' },
                  { value: 'US', label: '🇺🇸 Mỹ' },
                  { value: 'KR', label: '🇰🇷 Hàn Quốc' },
                  { value: 'JP', label: '🇯🇵 Nhật Bản' },
                  { value: 'CN', label: '🇨🇳 Trung Quốc' },
                ]}
              />
            </Form.Item>

            {/* Thông tin liên hệ */}
            <Divider plain>
              <Space><PhoneOutlined /> Thông tin liên hệ</Space>
            </Divider>

            <Row gutter={16}>
              <Col xs={24} sm={12}>
                <Form.Item
                  name="phoneNumber"
                  label="Số điện thoại"
                  rules={[
                    {
                      pattern: /^(0|\+84)[0-9]{8,10}$/,
                      message: 'Số điện thoại không hợp lệ (VD: 0912345678)',
                    },
                  ]}
                >
                  <Input prefix={<PhoneOutlined />} placeholder="0912345678" />
                </Form.Item>
              </Col>
              <Col xs={24} sm={12}>
                <Form.Item
                  name="email"
                  label="Email"
                  rules={[{ type: 'email', message: 'Email không hợp lệ' }]}
                >
                  <Input prefix={<MailOutlined />} placeholder="example@email.com" />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item name="address" label="Địa chỉ">
              <Input prefix={<EnvironmentOutlined />} placeholder="123 Nguyễn Huệ, P. Bến Nghé" maxLength={500} />
            </Form.Item>

            <Row gutter={16}>
              <Col xs={24} sm={12}>
                <Form.Item name="district" label="Quận / Huyện">
                  <Input placeholder="Quận 1" maxLength={100} />
                </Form.Item>
              </Col>
              <Col xs={24} sm={12}>
                <Form.Item name="city" label="Tỉnh / Thành phố">
                  <Input placeholder="Hồ Chí Minh" maxLength={100} />
                </Form.Item>
              </Col>
            </Row>

            {/* Error */}
            {createCustomer.isError && (
              <Alert
                type="error"
                showIcon
                message={
                  (createCustomer.error as { response?: { data?: { message?: string } } })
                    ?.response?.data?.message ?? 'Tạo khách hàng thất bại. Vui lòng kiểm tra lại.'
                }
                style={{ marginBottom: 16, borderRadius: 8 }}
              />
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                type="primary"
                size="large"
                loading={createCustomer.isPending}
                onClick={handleStep1}
                icon={<IdcardOutlined />}
              >
                Tiếp theo: Tạo tài khoản
              </Button>
            </div>
          </Form>
        </Card>
      )}

      {/* ── STEP 2: Login account ─────────────────────────────── */}
      {currentStep === 1 && createdCustomer && (
        <Card style={{ borderRadius: 12 }}>
          {/* CIF summary */}
          <Alert
            type="success"
            showIcon
            message={
              <span>
                Đã tạo hồ sơ khách hàng thành công —{' '}
                <Text strong copyable style={{ fontFamily: 'monospace' }}>
                  {createdCustomer.cif}
                </Text>{' '}
                · {createdCustomer.fullName}
              </span>
            }
            style={{ marginBottom: 24, borderRadius: 8 }}
          />

          <Form
            form={form2}
            layout="vertical"
            size="large"
          >
            <Divider plain style={{ marginTop: 0 }}>
              <Space><LockOutlined /> Tài khoản đăng nhập</Space>
            </Divider>

            <Form.Item
              name="username"
              label="Tên đăng nhập"
              tooltip="Chỉ chữ thường, số, dấu chấm, gạch ngang, gạch dưới"
              rules={[
                { required: true, message: 'Nhập tên đăng nhập' },
                { min: 3, message: 'Tối thiểu 3 ký tự' },
                { max: 100, message: 'Tối đa 100 ký tự' },
                {
                  pattern: /^[a-z0-9._-]+$/,
                  message: 'Chỉ được dùng chữ thường, số, dấu chấm, gạch ngang, gạch dưới',
                },
              ]}
            >
              <Input
                prefix={<UserAddOutlined />}
                placeholder="vd: nguyenvanan, mailinh92"
              />
            </Form.Item>

            <Row gutter={16}>
              <Col xs={24} sm={12}>
                <Form.Item
                  name="password"
                  label="Mật khẩu"
                  rules={[
                    { required: true, message: 'Nhập mật khẩu' },
                    { min: 6, message: 'Tối thiểu 6 ký tự' },
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="Tối thiểu 6 ký tự"
                    iconRender={(visible) =>
                      visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />
                    }
                  />
                </Form.Item>
              </Col>
              <Col xs={24} sm={12}>
                <Form.Item
                  name="confirmPassword"
                  label="Xác nhận mật khẩu"
                  dependencies={['password']}
                  rules={[
                    { required: true, message: 'Xác nhận mật khẩu' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('password') === value) {
                          return Promise.resolve();
                        }
                        return Promise.reject(new Error('Mật khẩu không khớp'));
                      },
                    }),
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="Nhập lại mật khẩu"
                    iconRender={(visible) =>
                      visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />
                    }
                  />
                </Form.Item>
              </Col>
            </Row>

            <Alert
              type="info"
              showIcon
              message="Lưu ý"
              description="Sau khi tạo, khách hàng có thể đăng nhập nhưng chưa mở được sổ tiết kiệm. Vui lòng thực hiện xác minh KYC trong trang chi tiết khách hàng."
              style={{ marginBottom: 20, borderRadius: 8 }}
            />

            {/* Error */}
            {createAuthUser.isError && (
              <Alert
                type="error"
                showIcon
                message={
                  (createAuthUser.error as { response?: { data?: { message?: string } } })
                    ?.response?.data?.message ?? 'Tạo tài khoản đăng nhập thất bại.'
                }
                style={{ marginBottom: 16, borderRadius: 8 }}
              />
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <Button
                onClick={() => setCurrentStep(0)}
                disabled={createAuthUser.isPending}
              >
                Quay lại
              </Button>
              <Button
                type="primary"
                size="large"
                loading={createAuthUser.isPending}
                onClick={handleStep2}
                icon={<CheckCircleOutlined />}
              >
                Hoàn tất
              </Button>
            </div>
          </Form>
        </Card>
      )}
    </div>
  );
}
