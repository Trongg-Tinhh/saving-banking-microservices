import {
  Typography,
  Card,
  Button,
  Descriptions,
  Tag,
  Spin,
  Alert,
  Space,
  Statistic,
  Row,
  Col,
  Divider,
} from 'antd';
import {
  ArrowLeftOutlined,
  BankOutlined,
  UserOutlined,
  WalletOutlined,
  PlusCircleOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { dayjs } from '@/utils/formatDate';
import { ROUTES, buildPath } from '@/constants/routes';
import { useAuthStore } from '@/stores/authStore';
import { useAccountBalance } from '@/hooks/useAccounts';
import { useQuery } from '@tanstack/react-query';
import type { ApiResponse } from '@/types/api.types';
import api from '@/services/api';
import { formatVND } from '@/utils/formatCurrency';

const { Title, Text } = Typography;

// ── Fetch full account info ───────────────────────────────────────

function useAccountDetail(accountNo: string | undefined) {
  return useQuery({
    queryKey: ['account-detail', accountNo ?? ''],
    queryFn:  async () => {
      const { data } = await api.get<ApiResponse<Record<string, unknown>>>(
        `/api/v1/accounts/${accountNo}`,
      );
      return data.data!;
    },
    enabled: Boolean(accountNo),
    staleTime: 30_000,
  });
}

// ── Status helpers ────────────────────────────────────────────────

const STATUS_COLOR: Record<string, string> = {
  ACTIVE:  'success',
  BLOCKED: 'error',
  CLOSED:  'default',
};

const ACCOUNT_TYPE_COLOR: Record<string, string> = {
  PAYMENT: 'blue',
  SAVING:  'green',
  LOAN:    'orange',
};

// ── Page ──────────────────────────────────────────────────────────

export default function AccountDetailPage() {
  const { accountNo } = useParams<{ accountNo: string }>();
  const navigate = useNavigate();
  const { hasAnyRole } = useAuthStore();
  const isStaff = hasAnyRole('ADMIN', 'TELLER', 'MANAGER');

  const { data: account,  isLoading: loadingAccount,  isError } = useAccountDetail(accountNo);
  const { data: balance,  isLoading: loadingBalance             } = useAccountBalance(accountNo);

  // ── Loading / error ───────────────────────────────────────────

  if (loadingAccount) {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Spin size="large" />
        <br />
        <Text type="secondary" style={{ marginTop: 12, display: 'block' }}>
          Đang tải thông tin tài khoản...
        </Text>
      </div>
    );
  }

  if (isError || !account) {
    return (
      <div style={{ maxWidth: 600, margin: '40px auto' }}>
        <Alert
          type="error"
          showIcon
          message="Không tìm thấy tài khoản"
          description={`Số tài khoản "${accountNo}" không tồn tại.`}
          action={<Button onClick={() => navigate(-1)}>Quay lại</Button>}
        />
      </div>
    );
  }

  const cif         = account['cif'] as string;
  const accountType = account['accountType'] as string;
  const currency    = account['currency'] as string;
  const status      = account['status'] as string;
  const openDate    = account['openDate'] as string;
  const branchCode  = account['branchCode'] as string | null;

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(-1)}
          style={{ marginBottom: 16 }}
        >
          Quay lại
        </Button>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: '50%',
              background: '#f0f5ff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 24,
              color: '#1677ff',
            }}
          >
            <BankOutlined />
          </div>
          <div>
            <Title level={4} style={{ margin: 0 }}>
              <Text copyable={{ text: accountNo }}>{accountNo}</Text>
            </Title>
            <Space size={8}>
              <Tag color={ACCOUNT_TYPE_COLOR[accountType] ?? 'blue'}>{accountType}</Tag>
              <Tag color={STATUS_COLOR[status] ?? 'default'}>{status}</Tag>
              <Tag>{currency}</Tag>
            </Space>
          </div>
        </div>
      </div>

      {/* Balance summary */}
      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={8}>
          <Card
            style={{ borderRadius: 12, background: 'linear-gradient(135deg, #1677ff 0%, #0958d9 100%)', border: 'none' }}
          >
            {loadingBalance ? (
              <Spin />
            ) : (
              <Statistic
                title={<Text style={{ color: 'rgba(255,255,255,0.8)', fontSize: 13 }}>Số dư khả dụng</Text>}
                value={balance?.availableBalance ?? 0}
                formatter={(v) => formatVND(Number(v))}
                valueStyle={{ color: '#fff', fontSize: 20, fontWeight: 700 }}
              />
            )}
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card style={{ borderRadius: 12 }}>
            {loadingBalance ? (
              <Spin />
            ) : (
              <Statistic
                title={<Text type="secondary" style={{ fontSize: 13 }}>Số dư sổ cái</Text>}
                value={balance?.ledgerBalance ?? 0}
                formatter={(v) => formatVND(Number(v))}
                valueStyle={{ fontSize: 18 }}
              />
            )}
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card style={{ borderRadius: 12 }}>
            {loadingBalance ? (
              <Spin />
            ) : (
              <Statistic
                title={<Text type="secondary" style={{ fontSize: 13 }}>Số dư giữ</Text>}
                value={balance?.holdAmount ?? 0}
                formatter={(v) => formatVND(Number(v))}
                valueStyle={{ fontSize: 18, color: '#fa8c16' }}
              />
            )}
          </Card>
        </Col>
      </Row>

      {/* Account info */}
      <Card style={{ borderRadius: 12, marginBottom: 20 }}>
        <Title level={5} style={{ marginTop: 0 }}>
          <BankOutlined style={{ marginRight: 8 }} />
          Thông tin tài khoản
        </Title>
        <Divider style={{ marginTop: 8, marginBottom: 16 }} />

        <Descriptions bordered column={{ xs: 1, sm: 2 }} size="small">
          <Descriptions.Item label="Số tài khoản">
            <Text strong copyable style={{ fontFamily: 'monospace' }}>{accountNo}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="CIF khách hàng">
            <Button
              type="link"
              size="small"
              icon={<UserOutlined />}
              style={{ padding: 0 }}
              onClick={() => navigate(buildPath(ROUTES.CUSTOMER_DETAIL, { cif }))}
            >
              {cif}
            </Button>
          </Descriptions.Item>
          <Descriptions.Item label="Loại tài khoản">
            <Tag color={ACCOUNT_TYPE_COLOR[accountType] ?? 'blue'}>{accountType}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Loại tiền tệ">
            <Tag>{currency}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <Tag color={STATUS_COLOR[status] ?? 'default'}>{status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Ngày mở">
            {openDate ? dayjs(openDate).format('DD/MM/YYYY') : '—'}
          </Descriptions.Item>
          {branchCode && (
            <Descriptions.Item label="Chi nhánh">
              {branchCode}
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>

      {/* Actions */}
      {isStaff && status === 'ACTIVE' && (
        <Card style={{ borderRadius: 12 }}>
          <Title level={5} style={{ marginTop: 0 }}>
            <WalletOutlined style={{ marginRight: 8 }} />
            Thao tác
          </Title>
          <Divider style={{ marginTop: 8, marginBottom: 16 }} />
          <Space wrap>
            <Button
              type="primary"
              icon={<PlusCircleOutlined />}
              onClick={() => navigate(`${ROUTES.CONTRACT_OPEN}?cif=${cif}`)}
            >
              Mở sổ tiết kiệm
            </Button>
            <Button
              icon={<UserOutlined />}
              onClick={() => navigate(buildPath(ROUTES.CUSTOMER_DETAIL, { cif }))}
            >
              Xem thông tin KH
            </Button>
          </Space>
        </Card>
      )}
    </div>
  );
}
