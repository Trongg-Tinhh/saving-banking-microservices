import { useState } from 'react';
import {
  Typography,
  Card,
  Table,
  Button,
  Input,
  Select,
  DatePicker,
  Space,
  Tag,
  Row,
  Col,
  Alert,
  Badge,
  Tooltip,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useTransactions } from '@/hooks/useTransactions';
import type { TransactionListFilter } from '@/services/transactionService';
import { formatVND } from '@/utils/formatCurrency';
import { formatDateTime } from '@/utils/formatDate';
import type { Transaction, TransactionType, TransactionStatus } from '@/types';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

// ─── Label / color maps ───────────────────────────────────────────

const TX_TYPE_LABEL: Record<string, string> = {
  OPEN_SAVING:      'Mở sổ',
  CLOSE_SAVING:     'Tất toán',
  INTEREST_PAYMENT: 'Trả lãi',
  DEBIT:            'Ghi nợ',
  CREDIT:           'Ghi có',
  INTEREST:         'Trả lãi',
};

const TX_TYPE_COLOR: Record<string, string> = {
  OPEN_SAVING:      'green',
  CLOSE_SAVING:     'red',
  INTEREST_PAYMENT: 'blue',
  DEBIT:            'orange',
  CREDIT:           'cyan',
  INTEREST:         'blue',
};

const TX_STATUS_LABEL: Record<string, string> = {
  PENDING:   'Chờ xử lý',
  COMPLETED: 'Hoàn thành',
  SUCCESS:   'Thành công',
  FAILED:    'Thất bại',
};

// ─── Columns ──────────────────────────────────────────────────────

const columns: ColumnsType<Transaction> = [
  {
    title: 'Mã giao dịch',
    dataIndex: 'transactionId',
    key: 'transactionId',
    width: 200,
    render: (v: string) => (
      <Tooltip title={v}>
        <Text code style={{ fontSize: 12 }}>{v ? v.slice(0, 12) + '…' : '—'}</Text>
      </Tooltip>
    ),
  },
  {
    title: 'Loại',
    dataIndex: 'transactionType',
    key: 'transactionType',
    width: 130,
    render: (t: string) => (
      <Tag color={TX_TYPE_COLOR[t] ?? 'default'}>
        {TX_TYPE_LABEL[t] ?? t}
      </Tag>
    ),
  },
  {
    title: 'Số hợp đồng',
    dataIndex: 'contractNo',
    key: 'contractNo',
    render: (v: string) =>
      v ? <Text code style={{ fontSize: 12 }}>{v}</Text> : <Text type="secondary">—</Text>,
  },
  {
    title: 'Số TK',
    dataIndex: 'accountNo',
    key: 'accountNo',
    width: 120,
    render: (v: string) => v ?? '—',
  },
  {
    title: 'CIF',
    dataIndex: 'cif',
    key: 'cif',
    width: 100,
  },
  {
    title: 'Số tiền',
    dataIndex: 'amount',
    key: 'amount',
    align: 'right',
    width: 150,
    render: (v: number, row: Transaction) => (
      <Text strong>{formatVND(v)} {row.currency}</Text>
    ),
  },
  {
    title: 'Trạng thái',
    dataIndex: 'status',
    key: 'status',
    width: 120,
    render: (s: string) => (
      <Badge
        status={
          s === 'SUCCESS' || s === 'COMPLETED' ? 'success'
          : s === 'FAILED' ? 'error'
          : 'processing'
        }
        text={TX_STATUS_LABEL[s] ?? s}
      />
    ),
  },
  {
    title: 'Thời gian',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 160,
    render: (v: string) => formatDateTime(v),
  },
];

// ─── Page ─────────────────────────────────────────────────────────

export default function TransactionPage() {
  const [contractNoInput, setContractNoInput] = useState('');
  const [cifInput, setCifInput]               = useState('');
  const [typeFilter, setTypeFilter]           = useState<TransactionType | undefined>();
  const [statusFilter, setStatusFilter]       = useState<TransactionStatus | undefined>();
  const [dateRange, setDateRange]             = useState<[string, string] | undefined>();

  const [filters, setFilters] = useState<TransactionListFilter>({ page: 0, size: 20 });

  const { data: page, isLoading, isError, isFetching, refetch } = useTransactions(filters);
  const transactions = page?.content ?? [];
  const total        = page?.totalElements ?? 0;

  const handleSearch = () => {
    setFilters({
      page:       0,
      size:       20,
      contractNo: contractNoInput.trim() || undefined,
      cif:        cifInput.trim() || undefined,
      txType:     typeFilter,
      status:     statusFilter,
      fromDate:   dateRange?.[0],
      toDate:     dateRange?.[1],
    });
  };

  const handleReset = () => {
    setContractNoInput('');
    setCifInput('');
    setTypeFilter(undefined);
    setStatusFilter(undefined);
    setDateRange(undefined);
    setFilters({ page: 0, size: 20 });
  };

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>Lịch sử giao dịch</Title>
          <Text type="secondary">
            {isLoading ? 'Đang tải...' : `${total.toLocaleString('vi-VN')} giao dịch`}
          </Text>
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
          type="error"
          message="Không thể tải lịch sử giao dịch"
          showIcon
          closable
          style={{ marginBottom: 16, borderRadius: 8 }}
          action={<Button size="small" onClick={() => refetch()}>Thử lại</Button>}
        />
      )}

      {/* Filter bar */}
      <Card style={{ borderRadius: 12, marginBottom: 16 }}>
        <Row gutter={[12, 12]}>
          <Col xs={24} sm={12} md={6}>
            <Input
              prefix={<SearchOutlined />}
              placeholder="Số hợp đồng"
              value={contractNoInput}
              onChange={(e) => setContractNoInput(e.target.value)}
              onPressEnter={handleSearch}
              allowClear
            />
          </Col>
          <Col xs={24} sm={12} md={5}>
            <Input
              prefix={<SearchOutlined />}
              placeholder="CIF khách hàng"
              value={cifInput}
              onChange={(e) => setCifInput(e.target.value)}
              onPressEnter={handleSearch}
              allowClear
            />
          </Col>
          <Col xs={24} sm={8} md={4}>
            <Select
              style={{ width: '100%' }}
              placeholder="Loại GD"
              allowClear
              value={typeFilter}
              onChange={(v) => setTypeFilter(v)}
              options={Object.entries(TX_TYPE_LABEL).map(([k, v]) => ({ value: k, label: v }))}
            />
          </Col>
          <Col xs={24} sm={8} md={4}>
            <Select
              style={{ width: '100%' }}
              placeholder="Trạng thái"
              allowClear
              value={statusFilter}
              onChange={(v) => setStatusFilter(v)}
              options={Object.entries(TX_STATUS_LABEL).map(([k, v]) => ({ value: k, label: v }))}
            />
          </Col>
          <Col xs={24} sm={24} md={5}>
            <RangePicker
              style={{ width: '100%' }}
              format="DD/MM/YYYY"
              placeholder={['Từ ngày', 'Đến ngày']}
              onChange={(_, dateStrings) => {
                const [from, to] = dateStrings;
                setDateRange(from && to ? [from, to] : undefined);
              }}
            />
          </Col>
        </Row>
        <Row gutter={[12, 0]} style={{ marginTop: 12 }}>
          <Col>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                Tìm kiếm
              </Button>
              <Button onClick={handleReset}>Xóa lọc</Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Table */}
      <Card style={{ borderRadius: 12 }}>
        <Table<Transaction>
          dataSource={transactions}
          columns={columns}
          rowKey="transactionId"
          loading={isLoading || isFetching}
          size="middle"
          scroll={{ x: 1000 }}
          pagination={{
            current: (filters.page ?? 0) + 1,
            pageSize: filters.size ?? 20,
            total,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
            showTotal: (tot, range) => `${range[0]}-${range[1]} / ${tot} giao dịch`,
          }}
          onChange={(pagination) => {
            setFilters((f) => ({
              ...f,
              page: (pagination.current ?? 1) - 1,
              size: pagination.pageSize ?? 20,
            }));
          }}
          locale={{
            emptyText: (
              <div style={{ padding: '32px 0', textAlign: 'center' }}>
                <SwapOutlined style={{ fontSize: 40, color: '#d9d9d9', display: 'block', marginBottom: 8 }} />
                <Text type="secondary">Không tìm thấy giao dịch nào</Text>
              </div>
            ),
          }}
        />
      </Card>
    </div>
  );
}
