import {
  Row,
  Col,
  Card,
  Statistic,
  Typography,
  Space,
  Table,
  Tag,
  Button,
  Skeleton,
  Alert,
  Badge,
} from 'antd';
import {
  FileTextOutlined,
  DollarOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  SwapOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import { useDashboard } from '@/hooks/useDashboard';
import { useAuthStore } from '@/stores/authStore';
import { buildPath, ROUTES } from '@/constants/routes';
import { formatVND, compactAmount } from '@/utils/formatCurrency';
import { formatDate, formatDateTime } from '@/utils/formatDate';
import type { NearMaturityItem, RecentTransaction, TransactionType } from '@/types';

const { Title, Text } = Typography;

// ─── Transaction type labels ──────────────────────────────────────

const TX_LABELS: Record<TransactionType, string> = {
  OPEN_SAVING:      'Mở sổ',
  CLOSE_SAVING:     'Tất toán',
  INTEREST_PAYMENT: 'Trả lãi',
};

const TX_COLORS: Record<TransactionType, string> = {
  OPEN_SAVING:      'green',
  CLOSE_SAVING:     'red',
  INTEREST_PAYMENT: 'blue',
};

// ─── Near-maturity table ──────────────────────────────────────────

const nearMaturityCols: ColumnsType<NearMaturityItem> = [
  {
    title: 'Hợp đồng',
    dataIndex: 'contractNo',
    key: 'contractNo',
    render: (v: string) => <Text code style={{ fontSize: 12 }}>{v}</Text>,
  },
  {
    title: 'Ngày đáo hạn',
    dataIndex: 'maturityDate',
    key: 'maturityDate',
    render: (v: string) => formatDate(v),
  },
  {
    title: 'Còn lại',
    dataIndex: 'daysRemaining',
    key: 'daysRemaining',
    align: 'center',
    render: (v: number) => (
      <Badge
        count={`${v} ngày`}
        style={{
          backgroundColor: v <= 7 ? '#ff4d4f' : v <= 14 ? '#fa8c16' : '#1677ff',
          fontSize: 12,
        }}
      />
    ),
  },
  {
    title: 'Số tiền gốc',
    dataIndex: 'principal',
    key: 'principal',
    align: 'right',
    render: (v: number, row: NearMaturityItem) => `${formatVND(v)} ${row.currency}`,
  },
];

// ─── Page ─────────────────────────────────────────────────────────

export default function DashboardPage() {
  const navigate  = useNavigate();
  const { user }  = useAuthStore();

  const { data, isLoading, isError, isFetching, refetch } = useDashboard();

  const summary     = data?.summary;
  const nearMaturity = data?.nearMaturity ?? [];
  const recentTx    = data?.recentTransactions ?? [];

  return (
    <div>
      {/* Greeting */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>
            Xin chào, {user?.fullName ?? user?.username ?? 'Người dùng'}!
          </Title>
          <Text type="secondary">Tổng quan hệ thống tiết kiệm ngân hàng</Text>
        </div>
        <Button
          icon={<ReloadOutlined spin={isFetching} />}
          onClick={() => refetch()}
          disabled={isFetching}
        >
          Làm mới
        </Button>
      </div>

      {/* Error */}
      {isError && (
        <Alert
          type="warning"
          message="Không thể tải dữ liệu tổng quan. Một số thông tin có thể không hiển thị."
          showIcon
          closable
          style={{ marginBottom: 20, borderRadius: 8 }}
        />
      )}

      {/* Summary stat cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {/* Total contracts */}
        <Col xs={24} sm={12} lg={6}>
          <Card style={{ borderRadius: 12 }} styles={{ body: { padding: 20 } }}>
            <Space align="start">
              <div style={{ width: 48, height: 48, borderRadius: 10, background: '#e6f4ff', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                <FileTextOutlined style={{ fontSize: 22, color: '#1677ff' }} />
              </div>
              <div>
                {isLoading ? (
                  <Skeleton active paragraph={{ rows: 1 }} title={false} />
                ) : (
                  <Statistic
                    title={<Text style={{ fontSize: 13 }}>Tổng hợp đồng</Text>}
                    value={summary?.totalContracts ?? 0}
                    valueStyle={{ fontSize: 24, fontWeight: 600 }}
                  />
                )}
              </div>
            </Space>
          </Card>
        </Col>

        {/* Total principal */}
        <Col xs={24} sm={12} lg={6}>
          <Card style={{ borderRadius: 12 }} styles={{ body: { padding: 20 } }}>
            <Space align="start">
              <div style={{ width: 48, height: 48, borderRadius: 10, background: '#f6ffed', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                <DollarOutlined style={{ fontSize: 22, color: '#52c41a' }} />
              </div>
              <div>
                {isLoading ? (
                  <Skeleton active paragraph={{ rows: 1 }} title={false} />
                ) : (
                  <Statistic
                    title={<Text style={{ fontSize: 13 }}>Tổng số dư gốc</Text>}
                    value={summary?.totalPrincipal ?? 0}
                    formatter={(v) => compactAmount(Number(v))}
                    valueStyle={{ fontSize: 24, fontWeight: 600, color: '#52c41a' }}
                  />
                )}
              </div>
            </Space>
          </Card>
        </Col>

        {/* Active contracts */}
        <Col xs={24} sm={12} lg={6}>
          <Card style={{ borderRadius: 12 }} styles={{ body: { padding: 20 } }}>
            <Space align="start">
              <div style={{ width: 48, height: 48, borderRadius: 10, background: '#fff7e6', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                <ClockCircleOutlined style={{ fontSize: 22, color: '#fa8c16' }} />
              </div>
              <div>
                {isLoading ? (
                  <Skeleton active paragraph={{ rows: 1 }} title={false} />
                ) : (
                  <Statistic
                    title={<Text style={{ fontSize: 13 }}>Đang hoạt động</Text>}
                    value={summary?.activeContracts ?? 0}
                    valueStyle={{ fontSize: 24, fontWeight: 600, color: '#fa8c16' }}
                  />
                )}
              </div>
            </Space>
          </Card>
        </Col>

        {/* Near maturity */}
        <Col xs={24} sm={12} lg={6}>
          <Card style={{ borderRadius: 12 }} styles={{ body: { padding: 20 } }}>
            <Space align="start">
              <div style={{ width: 48, height: 48, borderRadius: 10, background: '#fff2f0', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                <WarningOutlined style={{ fontSize: 22, color: '#ff4d4f' }} />
              </div>
              <div>
                {isLoading ? (
                  <Skeleton active paragraph={{ rows: 1 }} title={false} />
                ) : (
                  <Statistic
                    title={<Text style={{ fontSize: 13 }}>Sắp đáo hạn (30 ngày)</Text>}
                    value={nearMaturity.length}
                    valueStyle={{ fontSize: 24, fontWeight: 600, color: '#ff4d4f' }}
                  />
                )}
              </div>
            </Space>
          </Card>
        </Col>
      </Row>

      {/* Lower section */}
      <Row gutter={[16, 16]}>
        {/* Near maturity table */}
        <Col xs={24} lg={14}>
          <Card
            title={
              <Space>
                <ClockCircleOutlined />
                <span>Hợp đồng sắp đáo hạn</span>
                {nearMaturity.length > 0 && (
                  <Tag color="orange">{nearMaturity.length}</Tag>
                )}
              </Space>
            }
            extra={
              <Button
                type="link"
                size="small"
                onClick={() => navigate(ROUTES.CONTRACTS)}
              >
                Xem tất cả
              </Button>
            }
            style={{ borderRadius: 12 }}
          >
            {isLoading ? (
              <Skeleton active paragraph={{ rows: 4 }} />
            ) : nearMaturity.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '32px 0' }}>
                <ClockCircleOutlined style={{ fontSize: 36, color: '#52c41a', display: 'block', marginBottom: 8 }} />
                <Text type="secondary">Không có hợp đồng nào sắp đáo hạn trong 30 ngày tới</Text>
              </div>
            ) : (
              <Table<NearMaturityItem>
                dataSource={nearMaturity}
                columns={nearMaturityCols}
                rowKey="contractNo"
                pagination={false}
                size="small"
                onRow={(record) => ({
                  onClick: () => navigate(buildPath(ROUTES.CONTRACT_DETAIL, { contractNo: record.contractNo })),
                  style: { cursor: 'pointer' },
                })}
              />
            )}
          </Card>
        </Col>

        {/* Recent transactions */}
        <Col xs={24} lg={10}>
          <Card
            title={
              <Space>
                <SwapOutlined />
                <span>Giao dịch gần đây</span>
              </Space>
            }
            extra={
              <Button
                type="link"
                size="small"
                onClick={() => navigate(ROUTES.TRANSACTIONS)}
              >
                Xem tất cả
              </Button>
            }
            style={{ borderRadius: 12 }}
          >
            {isLoading ? (
              <Skeleton active paragraph={{ rows: 5 }} />
            ) : recentTx.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '32px 0' }}>
                <SwapOutlined style={{ fontSize: 36, color: '#d9d9d9', display: 'block', marginBottom: 8 }} />
                <Text type="secondary">Chưa có giao dịch nào</Text>
              </div>
            ) : (
              <div>
                {recentTx.map((tx: RecentTransaction) => (
                  <div
                    key={tx.txId}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '10px 0',
                      borderBottom: '1px solid #f0f0f0',
                      cursor: 'pointer',
                    }}
                    onClick={() => navigate(
                      buildPath(ROUTES.CONTRACT_DETAIL, { contractNo: tx.contractNo })
                    )}
                  >
                    <Space direction="vertical" size={2}>
                      <Space size={6}>
                        <Tag color={TX_COLORS[tx.txType]} style={{ margin: 0 }}>
                          {TX_LABELS[tx.txType]}
                        </Tag>
                        <Text code style={{ fontSize: 11 }}>{tx.contractNo}</Text>
                      </Space>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {formatDateTime(tx.createdAt)}
                      </Text>
                    </Space>
                    <Text strong style={{ color: tx.txType === 'CLOSE_SAVING' ? '#ff4d4f' : '#52c41a' }}>
                      {formatVND(tx.amount)} {tx.currency}
                    </Text>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {/* Interest summary */}
      {summary?.totalInterestExpected !== undefined && summary.totalInterestExpected > 0 && (
        <Card style={{ borderRadius: 12, marginTop: 16 }}>
          <Statistic
            title={<Text style={{ fontSize: 14 }}>Tổng lãi dự kiến (hợp đồng đang hoạt động)</Text>}
            value={summary.totalInterestExpected}
            formatter={(v) => formatVND(Number(v))}
            valueStyle={{ fontSize: 20, color: '#52c41a' }}
          />
        </Card>
      )}
    </div>
  );
}
