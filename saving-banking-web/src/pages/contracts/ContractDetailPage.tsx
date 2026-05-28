import {
  Typography,
  Card,
  Button,
  Tag,
  Tabs,
  Descriptions,
  Space,
  Skeleton,
  Alert,
  Timeline,
  Tooltip,
  Statistic,
  Row,
  Col,
} from 'antd';
import {
  ArrowLeftOutlined,
  CloseCircleOutlined,
  CheckCircleOutlined,
  CalendarOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { useContract, useContractHistory } from '@/hooks/useContracts';
import { buildPath, ROUTES } from '@/constants/routes';
import { formatVND } from '@/utils/formatCurrency';
import { formatDate, formatDateTime, dayjs } from '@/utils/formatDate';
import { getStatusConfig } from '@/utils/contractStatus';
import type { ContractStatus, ContractStatusHistory } from '@/types';

const { Title, Text } = Typography;

// ─── Status timeline color ────────────────────────────────────────

function historyDotColor(status: ContractStatus): string {
  const map: Partial<Record<ContractStatus, string>> = {
    ACTIVE:       '#52c41a',
    MATURED:      '#1677ff',
    CLOSED:       '#8c8c8c',
    EARLY_CLOSED: '#fa8c16',
    CANCELLED:    '#ff4d4f',
    FAILED:       '#ff4d4f',
    PENDING:      '#faad14',
  };
  return map[status] ?? '#d9d9d9';
}

// ─── Page ─────────────────────────────────────────────────────────

export default function ContractDetailPage() {
  const { contractNo } = useParams<{ contractNo: string }>();
  const navigate = useNavigate();

  const { data: contract, isLoading, isError } = useContract(contractNo);
  const { data: history = [], isLoading: historyLoading } = useContractHistory(contractNo);

  if (isError) {
    return (
      <div>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(ROUTES.CONTRACTS)} style={{ marginBottom: 16 }}>
          Quay lại
        </Button>
        <Alert
          type="error"
          message={`Không tìm thấy hợp đồng "${contractNo}"`}
          showIcon
          action={<Button onClick={() => navigate(ROUTES.CONTRACTS)}>Về danh sách</Button>}
        />
      </div>
    );
  }

  const canClose    = contract?.status === 'ACTIVE';
  const canMaturity = contract?.status === 'MATURED';
  const isEarlyClose = contract?.maturityDate && dayjs(contract.maturityDate).isAfter(dayjs());
  const statusConfig = contract ? getStatusConfig(contract.status) : null;

  // ─── Info Tab ──────────────────────────────────────────────────

  const infoTab = isLoading ? (
    <Skeleton active paragraph={{ rows: 8 }} />
  ) : contract ? (
    <>
      {/* Summary stats */}
      <Row gutter={[12, 12]} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={6}>
          <Card style={{ borderRadius: 8, background: '#f0f7ff', border: '1px solid #bae0ff' }}>
            <Statistic
              title={<Text style={{ fontSize: 12 }}>Số tiền gốc</Text>}
              value={contract.principalAmount}
              formatter={(v) => formatVND(Number(v))}
              valueStyle={{ fontSize: 16, color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card style={{ borderRadius: 8, background: '#f6ffed', border: '1px solid #b7eb8f' }}>
            <Statistic
              title={<Text style={{ fontSize: 12 }}>Lãi suất / năm</Text>}
              value={contract.interestRate.toFixed(2)}
              suffix="%"
              valueStyle={{ fontSize: 16, color: '#52c41a' }}
            />
          </Card>
        </Col>
        {contract.expectedInterest !== undefined && (
          <Col xs={12} sm={6}>
            <Card style={{ borderRadius: 8, background: '#fff7e6', border: '1px solid #ffd591' }}>
              <Statistic
                title={<Text style={{ fontSize: 12 }}>Lãi dự kiến</Text>}
                value={contract.expectedInterest}
                formatter={(v) => formatVND(Number(v))}
                valueStyle={{ fontSize: 16, color: '#fa8c16' }}
              />
            </Card>
          </Col>
        )}
        {contract.daysRemaining !== undefined && contract.daysRemaining > 0 && (
          <Col xs={12} sm={6}>
            <Card style={{ borderRadius: 8, background: '#fff2f0', border: '1px solid #ffccc7' }}>
              <Statistic
                title={<Text style={{ fontSize: 12 }}>Ngày còn lại</Text>}
                value={contract.daysRemaining}
                suffix="ngày"
                valueStyle={{ fontSize: 16, color: '#ff4d4f' }}
              />
            </Card>
          </Col>
        )}
      </Row>

      <Descriptions bordered column={{ xs: 1, sm: 2 }} size="small">
        <Descriptions.Item label="Số hợp đồng">
          <Text code>{contract.contractNo}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="Mã CIF">
          <Text strong>{contract.cif}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="Sản phẩm">
          {contract.productName ?? contract.productCode}
        </Descriptions.Item>
        <Descriptions.Item label="Kỳ hạn">
          {contract.termLabel ?? contract.termId}
          {contract.termDays && <Text type="secondary"> ({contract.termDays} ngày)</Text>}
        </Descriptions.Item>
        <Descriptions.Item label="Tiền tệ">
          <Tag>{contract.currency}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Hình thức trả lãi">
          {contract.interestPaymentMethod}
        </Descriptions.Item>
        <Descriptions.Item label="Ngày mở">
          {formatDate(contract.openDate)}
        </Descriptions.Item>
        <Descriptions.Item label="Ngày đáo hạn">
          <Text strong>{formatDate(contract.maturityDate)}</Text>
          {isEarlyClose && (
            <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
              (chưa đến hạn)
            </Text>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="Tài khoản nguồn">
          <Text code>{contract.sourceAccountNo}</Text>
        </Descriptions.Item>
        {contract.branchCode && (
          <Descriptions.Item label="Chi nhánh">{contract.branchCode}</Descriptions.Item>
        )}
        {contract.openedBy && (
          <Descriptions.Item label="Người mở">{contract.openedBy}</Descriptions.Item>
        )}
        {contract.maturityInstruction && (
          <Descriptions.Item label="Chỉ thị đáo hạn" span={2}>
            <Tag color="blue">{contract.maturityInstruction.instructionType}</Tag>
            {contract.maturityInstruction.receivingAccountNo && (
              <Text type="secondary" style={{ marginLeft: 8 }}>
                → {contract.maturityInstruction.receivingAccountNo}
              </Text>
            )}
          </Descriptions.Item>
        )}
      </Descriptions>
    </>
  ) : null;

  // ─── History Tab ───────────────────────────────────────────────

  const historyTab = historyLoading ? (
    <Skeleton active paragraph={{ rows: 4 }} />
  ) : history.length === 0 ? (
    <Alert type="info" message="Chưa có lịch sử trạng thái" showIcon />
  ) : (
    <Timeline
      mode="left"
      items={history.map((h: ContractStatusHistory) => ({
        color: historyDotColor(h.toStatus),
        label: formatDateTime(h.changedAt),
        children: (
          <div>
            <Space size={4}>
              {h.fromStatus && (
                <>
                  <Tag color={getStatusConfig(h.fromStatus).color}>
                    {getStatusConfig(h.fromStatus).label}
                  </Tag>
                  <Text type="secondary">→</Text>
                </>
              )}
              <Tag color={getStatusConfig(h.toStatus).color}>
                {getStatusConfig(h.toStatus).label}
              </Tag>
            </Space>
            {h.reason && (
              <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>
                {h.reason}
              </Text>
            )}
            {h.changedBy && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                Bởi: {h.changedBy}
              </Text>
            )}
          </div>
        ),
      }))}
    />
  );

  // ─── Render ───────────────────────────────────────────────────

  return (
    <div>
      {/* Back */}
      <Button
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate(ROUTES.CONTRACTS)}
        style={{ marginBottom: 16 }}
      >
        Danh sách hợp đồng
      </Button>

      {/* Header card */}
      <Card style={{ borderRadius: 12, marginBottom: 20 }}>
        {isLoading ? (
          <Skeleton active paragraph={{ rows: 2 }} />
        ) : contract ? (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
            <div>
              <Space align="center">
                <Title level={4} style={{ margin: 0 }}>Hợp đồng {contract.contractNo}</Title>
                {statusConfig && (
                  <Tag color={statusConfig.color} style={{ fontSize: 13 }}>
                    {statusConfig.label}
                  </Tag>
                )}
              </Space>
              <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
                CIF: {contract.cif} · {contract.productName ?? contract.productCode}
              </Text>
            </div>

            <Space wrap>
              {canClose && (
                <Tooltip title={isEarlyClose ? 'Tất toán trước hạn — sẽ áp dụng phạt' : 'Tất toán'}>
                  <Button
                    danger
                    icon={<CloseCircleOutlined />}
                    onClick={() => navigate(buildPath(ROUTES.CONTRACT_CLOSE, { contractNo: contractNo! }))}
                  >
                    Tất toán
                  </Button>
                </Tooltip>
              )}
              {canMaturity && (
                <Button
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  onClick={() => navigate(buildPath(ROUTES.CONTRACT_MATURITY, { contractNo: contractNo! }))}
                >
                  Xử lý đáo hạn
                </Button>
              )}
            </Space>
          </div>
        ) : null}
      </Card>

      {/* Tabs */}
      <Card style={{ borderRadius: 12 }}>
        <Tabs
          defaultActiveKey="info"
          items={[
            {
              key: 'info',
              label: (
                <Space size={6}>
                  <CalendarOutlined />
                  Thông tin hợp đồng
                </Space>
              ),
              children: infoTab,
            },
            {
              key: 'history',
              label: (
                <Space size={6}>
                  <HistoryOutlined />
                  Lịch sử trạng thái
                </Space>
              ),
              children: historyTab,
            },
          ]}
        />
      </Card>
    </div>
  );
}
