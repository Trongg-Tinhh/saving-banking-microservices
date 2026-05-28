import { useState, useMemo } from 'react';
import {
  Typography,
  Card,
  Table,
  Button,
  Input,
  Select,
  Space,
  Tag,
  Row,
  Col,
  Alert,
  Tooltip,
  Badge,
} from 'antd';
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import { useContracts } from '@/hooks/useContracts';
import { useAuthStore } from '@/stores/authStore';
import { buildPath, ROUTES } from '@/constants/routes';
import { formatVND } from '@/utils/formatCurrency';
import { formatDate } from '@/utils/formatDate';
import { getStatusConfig, isContractActive } from '@/utils/contractStatus';
import type { SavingContract, ContractStatus } from '@/types';
import type { ContractListFilter } from '@/services/contractService';

const { Title, Text } = Typography;

// ─── Status filter options ────────────────────────────────────────

const STATUS_OPTIONS: { value: ContractStatus; label: string }[] = [
  { value: 'ACTIVE',       label: 'Đang hoạt động' },
  { value: 'PENDING',      label: 'Chờ xử lý' },
  { value: 'MATURED',      label: 'Đã đáo hạn' },
  { value: 'CLOSED',       label: 'Đã tất toán' },
  { value: 'EARLY_CLOSED', label: 'Tất toán trước hạn' },
  { value: 'CANCELLED',    label: 'Đã huỷ' },
  { value: 'FAILED',       label: 'Thất bại' },
];

// ─── Page ─────────────────────────────────────────────────────────

export default function ContractListPage() {
  const navigate   = useNavigate();
  const { user, isCustomer, hasAnyRole } = useAuthStore();
  const isStaff    = hasAnyRole('ADMIN', 'TELLER', 'MANAGER');

  // Local filter state (uncontrolled until Search is clicked) — staff only
  const [cifInput, setCifInput]         = useState('');
  const [statusFilter, setStatusFilter] = useState<ContractStatus | undefined>();

  // Applied filters — CUSTOMER always scopes to own CIF; staff can filter freely
  const initialFilters = useMemo<ContractListFilter>(() => ({
    page: 0,
    size: 20,
    // Backend ignores this for CUSTOMER (uses JWT CIF), but set it anyway for clarity
    cif: isCustomer() && user?.cif ? user.cif : undefined,
  }), []);                         // eslint-disable-line react-hooks/exhaustive-deps

  const [filters, setFilters] = useState<ContractListFilter>(initialFilters);

  const { data: page, isLoading, isError, isFetching, refetch } = useContracts(filters);
  const contracts = page?.content ?? [];
  const total     = page?.totalElements ?? 0;

  const handleSearch = () => {
    setFilters({
      page: 0,
      size: 20,
      cif:    cifInput.trim() || undefined,
      status: statusFilter,
    });
  };

  const handleReset = () => {
    setCifInput('');
    setStatusFilter(undefined);
    setFilters(initialFilters);
  };

  const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
    setFilters((f) => ({
      ...f,
      page: (pagination.current ?? 1) - 1,
      size: pagination.pageSize ?? 20,
    }));
  };

  // ─── Columns ──────────────────────────────────────────────────────

  const columns: ColumnsType<SavingContract> = [
    {
      title: 'Số hợp đồng',
      dataIndex: 'contractNo',
      key: 'contractNo',
      render: (v: string) => <Text code style={{ fontSize: 13 }}>{v}</Text>,
      width: 170,
    },
    // CIF column only visible to staff (customers see only their own contracts)
    ...(isStaff ? [{
      title: 'CIF',
      dataIndex: 'cif',
      key: 'cif',
      width: 110,
    } as ColumnsType<SavingContract>[number]] : []),
    {
      title: 'Sản phẩm',
      dataIndex: 'productName',
      key: 'productName',
      render: (name: string | undefined, row) => name ?? row.productCode,
      ellipsis: true,
    },
    {
      title: 'Số tiền gốc',
      dataIndex: 'principalAmount',
      key: 'principalAmount',
      align: 'right',
      width: 150,
      render: (v: number) => <Text strong>{formatVND(v)}</Text>,
    },
    {
      title: 'Lãi suất',
      dataIndex: 'interestRate',
      key: 'interestRate',
      align: 'center',
      width: 90,
      render: (v: number) => `${v.toFixed(2)}%`,
    },
    {
      title: 'Ngày mở',
      dataIndex: 'openDate',
      key: 'openDate',
      width: 110,
      render: (v: string) => formatDate(v),
    },
    {
      title: 'Ngày đáo hạn',
      dataIndex: 'maturityDate',
      key: 'maturityDate',
      width: 120,
      render: (v: string) => formatDate(v),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 150,
      render: (status: ContractStatus) => {
        const { label, color } = getStatusConfig(status);
        return <Tag color={color}>{label}</Tag>;
      },
    },
    {
      title: '',
      key: 'actions',
      width: 60,
      align: 'center',
      render: (_: unknown, row: SavingContract) => (
        <Tooltip title="Xem chi tiết">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(buildPath(ROUTES.CONTRACT_DETAIL, { contractNo: row.contractNo }))}
          />
        </Tooltip>
      ),
    },
  ];

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>
            {isCustomer() ? 'Sổ tiết kiệm của tôi' : 'Hợp đồng tiết kiệm'}
          </Title>
          <Text type="secondary">
            {isLoading ? 'Đang tải...' : `${total.toLocaleString('vi-VN')} hợp đồng`}
          </Text>
        </div>
        <Space>
          <Button
            icon={<ReloadOutlined spin={isFetching} />}
            onClick={() => refetch()}
            disabled={isFetching}
          >
            Làm mới
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => navigate(ROUTES.CONTRACT_OPEN)}
          >
            Mở sổ tiết kiệm
          </Button>
        </Space>
      </div>

      {/* Error */}
      {isError && (
        <Alert
          type="error"
          message="Không thể tải danh sách hợp đồng"
          showIcon
          closable
          style={{ marginBottom: 16, borderRadius: 8 }}
          action={<Button size="small" onClick={() => refetch()}>Thử lại</Button>}
        />
      )}

      {/* Filter bar — CIF search only for staff */}
      <Card style={{ borderRadius: 12, marginBottom: 16 }}>
        <Row gutter={[12, 12]} align="middle">
          {isStaff && (
            <Col xs={24} sm={12} md={7}>
              <Input
                prefix={<SearchOutlined />}
                placeholder="Tìm theo CIF khách hàng"
                value={cifInput}
                onChange={(e) => setCifInput(e.target.value)}
                onPressEnter={handleSearch}
                allowClear
              />
            </Col>
          )}
          <Col xs={24} sm={12} md={isStaff ? 7 : 12}>
            <Select
              style={{ width: '100%' }}
              placeholder="Lọc theo trạng thái"
              allowClear
              value={statusFilter}
              onChange={(v) => setStatusFilter(v)}
              options={STATUS_OPTIONS}
            />
          </Col>
          <Col xs={24} sm={24} md={isStaff ? 10 : 12}>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                Tìm kiếm
              </Button>
              <Button onClick={handleReset}>Xóa lọc</Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Summary badges */}
      <Space style={{ marginBottom: 12 }} wrap>
        {(['ACTIVE', 'PENDING', 'MATURED', 'CLOSED', 'EARLY_CLOSED'] as ContractStatus[]).map((status) => {
          const count = contracts.filter((c) => c.status === status).length;
          if (!count) return null;
          const { color, label } = getStatusConfig(status);
          return (
            <Badge key={status} count={count} color={color} overflowCount={999}>
              <Tag color={color} style={{ paddingRight: 20 }}>{label}</Tag>
            </Badge>
          );
        })}
      </Space>

      {/* Table */}
      <Card style={{ borderRadius: 12 }}>
        <Table<SavingContract>
          dataSource={contracts}
          columns={columns}
          rowKey="contractNo"
          loading={isLoading || isFetching}
          size="middle"
          pagination={{
            current: (filters.page ?? 0) + 1,
            pageSize: filters.size ?? 20,
            total,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
            showTotal: (tot, range) => `${range[0]}-${range[1]} / ${tot} hợp đồng`,
          }}
          onChange={(pagination) => handleTableChange(pagination)}
          onRow={(record) => ({
            onClick: () => navigate(buildPath(ROUTES.CONTRACT_DETAIL, { contractNo: record.contractNo })),
            style: { cursor: 'pointer' },
          })}
          rowClassName={(record) =>
            isContractActive(record.status) ? '' : 'ant-table-row-dim'
          }
          locale={{ emptyText: 'Chưa có hợp đồng nào' }}
        />
      </Card>
    </div>
  );
}
