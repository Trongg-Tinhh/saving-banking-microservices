import { useState, useEffect } from 'react';
import {
  Typography,
  Card,
  Button,
  Form,
  Input,
  Select,
  DatePicker,
  Alert,
  Space,
  Result,
  Tag,
  Divider,
} from 'antd';
import {
  ArrowLeftOutlined,
  UserOutlined,
  BankOutlined,
  PlusCircleOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import type { Dayjs } from 'dayjs';
import { dayjs } from '@/utils/formatDate';
import { ROUTES } from '@/constants/routes';
import { useAuthStore } from '@/stores/authStore';
import { useCreateAccount } from '@/hooks/useAccounts';
import type { AccountType, CreatedAccountResponse } from '@/types';

const { Title, Text } = Typography;

interface CreateAccountForm {
  cif: string;
  accountType: AccountType;
  currency: string;
  branchCode?: string;
  openDate: Dayjs;
}

const ACCOUNT_TYPE_OPTIONS = [
  { value: 'PAYMENT', label: 'Thanh toán (PAYMENT)', color: 'blue' },
  { value: 'SAVING',  label: 'Tiết kiệm (SAVING)',   color: 'green' },
  { value: 'LOAN',    label: 'Vay (LOAN)',            color: 'orange' },
];

const CURRENCY_OPTIONS = [
  { value: 'VND', label: '🇻🇳 VND — Việt Nam đồng' },
  { value: 'USD', label: '🇺🇸 USD — Đô la Mỹ' },
  { value: 'EUR', label: '🇪🇺 EUR — Euro' },
];

export default function CreateAccountPage() {
  const navigate  = useNavigate();
  const [searchParams] = useSearchParams();
  const { user, isCustomer, hasAnyRole } = useAuthStore();
  const isStaff = hasAnyRole('ADMIN', 'TELLER', 'MANAGER');

  const [form] = Form.useForm<CreateAccountForm>();
  const createAccount = useCreateAccount();
  const [created, setCreated] = useState<CreatedAccountResponse | null>(null);

  // Pre-fill CIF: from URL param (when navigated from OpenSavingPage) or from logged-in user
  useEffect(() => {
    const cifParam = searchParams.get('cif');
    if (cifParam) {
      form.setFieldValue('cif', cifParam);
    } else if (isCustomer() && user?.cif) {
      form.setFieldValue('cif', user.cif);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleFinish = async (values: CreateAccountForm) => {
    try {
      const result = await createAccount.mutateAsync({
        cif:         values.cif.trim(),
        accountType: values.accountType,
        currency:    values.currency,
        branchCode:  values.branchCode?.trim() || undefined,
        openDate:    values.openDate?.format('YYYY-MM-DD'),
      });
      setCreated(result);
      // Don't navigate — show success result with action buttons
    } catch {
      // error shown via Alert below
    }
  };

  // ── Success screen ───────────────────────────────────────────────

  if (created) {
    return (
      <div style={{ maxWidth: 560, margin: '0 auto' }}>
        <Result
          status="success"
          icon={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
          title="Tạo tài khoản thành công!"
          subTitle={
            <Space direction="vertical" size={4}>
              <Text>
                Số tài khoản:{' '}
                <Text strong copyable style={{ fontSize: 16 }}>
                  {created.accountNo}
                </Text>
              </Text>
              <Text type="secondary">
                CIF: {created.cif} &nbsp;|&nbsp;
                Loại: <Tag color="blue">{created.accountType}</Tag> &nbsp;|&nbsp;
                Tiền tệ: <Tag>{created.currency}</Tag>
              </Text>
            </Space>
          }
          extra={[
            <Button
              key="open"
              type="primary"
              onClick={() =>
                navigate(`${ROUTES.CONTRACT_OPEN}?cif=${created.cif}`)
              }
            >
              Mở sổ tiết kiệm ngay
            </Button>,
            <Button key="new" onClick={() => { setCreated(null); form.resetFields(); }}>
              Tạo tài khoản khác
            </Button>,
            <Button key="back" onClick={() => navigate(ROUTES.CONTRACTS)}>
              Về danh sách hợp đồng
            </Button>,
          ]}
        />
      </div>
    );
  }

  // ── Form ─────────────────────────────────────────────────────────

  return (
    <div style={{ maxWidth: 600, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(-1)}
          style={{ marginBottom: 16 }}
        >
          Quay lại
        </Button>
        <Title level={4} style={{ margin: 0 }}>
          <PlusCircleOutlined style={{ marginRight: 8, color: '#1677ff' }} />
          Tạo tài khoản ngân hàng
        </Title>
        <Text type="secondary">
          {isStaff
            ? 'Mở tài khoản cho khách hàng theo CIF'
            : 'Tạo tài khoản thanh toán mới'}
        </Text>
      </div>

      {/* Warning for customer */}
      {isCustomer() && (
        <Alert
          type="info"
          showIcon
          message="Tài khoản sẽ được mở với CIF của bạn"
          style={{ marginBottom: 20, borderRadius: 8 }}
        />
      )}

      <Card style={{ borderRadius: 12 }}>
        <Form
          form={form}
          layout="vertical"
          size="large"
          onFinish={handleFinish}
          initialValues={{
            accountType: 'PAYMENT',
            currency:    'VND',
            openDate:    dayjs(),
          }}
        >
          {/* CIF */}
          <Divider plain style={{ marginTop: 0 }}>
            <Space><UserOutlined /> Thông tin khách hàng</Space>
          </Divider>

          <Form.Item
            name="cif"
            label="Mã khách hàng (CIF)"
            rules={[
              { required: true, message: 'Nhập mã CIF khách hàng' },
              { min: 3,         message: 'CIF phải có ít nhất 3 ký tự' },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder={isCustomer() ? '' : 'Nhập mã CIF...'}
              disabled={isCustomer()}
            />
          </Form.Item>

          {/* Account details */}
          <Divider plain>
            <Space><BankOutlined /> Thông tin tài khoản</Space>
          </Divider>

          <Form.Item
            name="accountType"
            label="Loại tài khoản"
            rules={[{ required: true, message: 'Chọn loại tài khoản' }]}
          >
            <Select
              options={ACCOUNT_TYPE_OPTIONS.map((opt) => ({
                value: opt.value,
                label: (
                  <Space>
                    <Tag color={opt.color}>{opt.value}</Tag>
                    {opt.label.split('(')[0].trim()}
                  </Space>
                ),
              }))}
            />
          </Form.Item>

          <Form.Item
            name="currency"
            label="Loại tiền tệ"
            rules={[{ required: true, message: 'Chọn loại tiền tệ' }]}
          >
            <Select options={CURRENCY_OPTIONS} />
          </Form.Item>

          {isStaff && (
            <Form.Item name="branchCode" label="Mã chi nhánh (tùy chọn)">
              <Input
                prefix={<BankOutlined />}
                placeholder="VD: HN001, HCM002..."
                maxLength={20}
              />
            </Form.Item>
          )}

          <Form.Item
            name="openDate"
            label="Ngày mở tài khoản"
            rules={[{ required: true, message: 'Chọn ngày mở tài khoản' }]}
          >
            <DatePicker
              style={{ width: '100%' }}
              format="DD/MM/YYYY"
              disabledDate={(d) => d.isAfter(dayjs())}
            />
          </Form.Item>

          {/* Error */}
          {createAccount.isError && (
            <Alert
              type="error"
              showIcon
              message="Không thể tạo tài khoản. Vui lòng kiểm tra lại thông tin."
              style={{ marginBottom: 16, borderRadius: 8 }}
            />
          )}

          <Form.Item style={{ marginBottom: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={createAccount.isPending}
              icon={<PlusCircleOutlined />}
              size="large"
            >
              Tạo tài khoản
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
