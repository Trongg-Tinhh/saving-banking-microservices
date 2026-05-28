import { useState } from 'react';
import {
  Typography,
  Card,
  Button,
  Tag,
  Tabs,
  Table,
  Descriptions,
  Space,
  Badge,
  Skeleton,
  Alert,
  Tooltip,
  Modal,
  Form,
  Input,
  InputNumber,
  Switch,
  DatePicker,
  Select,
} from 'antd';
import {
  ArrowLeftOutlined,
  CalculatorOutlined,
  CheckCircleOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  PlusOutlined,
  PoweroffOutlined,
  StopOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  useProduct,
  useProductTerms,
  useAllProductTerms,
  useProductRates,
  useEarlyWithdrawalPolicy,
  useToggleProductStatus,
  useCreateTerm,
  useUpdateTerm,
  useAddRateConfig,
  useUpsertEarlyWithdrawalPolicy,
} from '@/hooks/useProducts';
import { ROUTES, buildPath } from '@/constants/routes';
import { useAuthStore } from '@/stores/authStore';
import { formatVND } from '@/utils/formatCurrency';
import { formatDate } from '@/utils/formatDate';
import type {
  SavingTerm,
  InterestRate,
  InterestPaymentMethod,
  CreateTermRequest,
  CreateRateConfigRequest,
  UpsertEarlyWithdrawalPolicyRequest,
} from '@/types';

const { Title, Text } = Typography;

const PAYMENT_METHOD_LABELS: Record<InterestPaymentMethod, string> = {
  END_OF_TERM: 'Cuối kỳ',
  MONTHLY:     'Hàng tháng',
  QUARTERLY:   'Hàng quý',
};

// ══════════════════════════════════════════════════════════════════
// Modal: Thêm Kỳ Hạn
// ══════════════════════════════════════════════════════════════════

function AddTermModal({
  open,
  productCode,
  onClose,
}: {
  open: boolean;
  productCode: string;
  onClose: () => void;
}) {
  const [form]      = Form.useForm<CreateTermRequest>();
  const createTerm  = useCreateTerm(productCode);

  const handleOk = async () => {
    const values = await form.validateFields();
    // Auto-calculate termDays if not filled
    if (!values.termDays) values.termDays = values.termMonths * 30;
    createTerm.mutate(values, {
      onSuccess: () => { form.resetFields(); onClose(); },
    });
  };

  return (
    <Modal
      title="Thêm kỳ hạn mới"
      open={open}
      onCancel={() => { form.resetFields(); onClose(); }}
      onOk={handleOk}
      confirmLoading={createTerm.isPending}
      okText="Thêm kỳ hạn"
      destroyOnClose
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="termId"
          label="Mã kỳ hạn"
          rules={[
            { required: true, message: 'Nhập mã kỳ hạn' },
            { max: 50, message: 'Tối đa 50 ký tự' },
          ]}
          tooltip="Ví dụ: TERM_MTH_6M. Mã duy nhất, không đổi được."
          normalize={(v: string) => v?.toUpperCase()}
        >
          <Input placeholder="TERM_MTH_18M" />
        </Form.Item>

        <Form.Item
          name="termLabel"
          label="Tên hiển thị"
          rules={[{ required: true, message: 'Nhập tên kỳ hạn' }]}
        >
          <Input placeholder="18 tháng" />
        </Form.Item>

        <Form.Item
          name="termMonths"
          label="Số tháng"
          rules={[{ required: true, message: 'Nhập số tháng' }, { type: 'number', min: 1 }]}
        >
          <InputNumber style={{ width: '100%' }} min={1} placeholder="18" addonAfter="tháng" />
        </Form.Item>

        <Form.Item
          name="termDays"
          label="Số ngày (tuỳ chọn — mặc định = tháng × 30)"
          tooltip="Để trống sẽ tự tính theo tháng × 30"
        >
          <InputNumber style={{ width: '100%' }} min={1} placeholder="540" addonAfter="ngày" />
        </Form.Item>
      </Form>
    </Modal>
  );
}

// ══════════════════════════════════════════════════════════════════
// Modal: Thêm Lãi Suất
// ══════════════════════════════════════════════════════════════════

function AddRateModal({
  open,
  productCode,
  terms,
  onClose,
}: {
  open: boolean;
  productCode: string;
  terms: SavingTerm[];
  onClose: () => void;
}) {
  const [form]    = Form.useForm<CreateRateConfigRequest & { effectiveFromDay: dayjs.Dayjs; effectiveToDay?: dayjs.Dayjs }>();
  const addRate   = useAddRateConfig(productCode);

  const handleOk = async () => {
    const values = await form.validateFields();
    const req: CreateRateConfigRequest = {
      termId:        values.termId,
      annualRate:    values.annualRate,
      effectiveFrom: values.effectiveFromDay.format('YYYY-MM-DD'),
      effectiveTo:   values.effectiveToDay ? values.effectiveToDay.format('YYYY-MM-DD') : null,
    };
    addRate.mutate(req, {
      onSuccess: () => { form.resetFields(); onClose(); },
    });
  };

  return (
    <Modal
      title="Thêm lãi suất mới"
      open={open}
      onCancel={() => { form.resetFields(); onClose(); }}
      onOk={handleOk}
      confirmLoading={addRate.isPending}
      okText="Lưu lãi suất"
      destroyOnClose
    >
      <Alert
        type="info"
        showIcon
        message="Lịch sử lãi suất là bất biến"
        description="Lãi suất đã ghi không thể xóa. Thêm bản ghi mới với ngày hiệu lực để thay thế lãi suất hiện tại."
        style={{ marginBottom: 16, borderRadius: 8 }}
      />
      <Form form={form} layout="vertical" initialValues={{ effectiveFromDay: dayjs() }}>
        <Form.Item
          name="termId"
          label="Kỳ hạn"
          rules={[{ required: true, message: 'Chọn kỳ hạn' }]}
        >
          <Select
            placeholder="Chọn kỳ hạn..."
            options={terms.map((t) => ({
              value: t.termId,
              label: `${t.termLabel} (${t.termId})`,
            }))}
          />
        </Form.Item>

        <Form.Item
          name="annualRate"
          label="Lãi suất / năm (%)"
          rules={[
            { required: true, message: 'Nhập lãi suất' },
            { type: 'number', min: 0, message: 'Lãi suất phải >= 0' },
          ]}
        >
          <InputNumber
            style={{ width: '100%' }}
            min={0}
            max={100}
            step={0.1}
            precision={2}
            addonAfter="%/năm"
            placeholder="6.50"
          />
        </Form.Item>

        <Form.Item
          name="effectiveFromDay"
          label="Hiệu lực từ"
          rules={[{ required: true, message: 'Chọn ngày hiệu lực' }]}
        >
          <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
        </Form.Item>

        <Form.Item
          name="effectiveToDay"
          label="Hiệu lực đến (để trống = lãi suất hiện tại, không có ngày kết thúc)"
        >
          <DatePicker
            style={{ width: '100%' }}
            format="DD/MM/YYYY"
            placeholder="Không giới hạn"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}

// ══════════════════════════════════════════════════════════════════
// Modal: Chính sách Rút sớm
// ══════════════════════════════════════════════════════════════════

function EarlyWithdrawalModal({
  open,
  productCode,
  initialValues,
  onClose,
}: {
  open: boolean;
  productCode: string;
  initialValues?: { minDaysHeld: number; penaltyRate: number; useDemandRate: boolean; demandRate: number } | null;
  onClose: () => void;
}) {
  const [form]   = Form.useForm<UpsertEarlyWithdrawalPolicyRequest>();
  const upsert   = useUpsertEarlyWithdrawalPolicy(productCode);
  const useDemand = Form.useWatch('useDemandRate', form);

  const handleOk = async () => {
    const values = await form.validateFields();
    // Backend requires both fields (NotNull); default to 0 when the field is hidden
    const req: UpsertEarlyWithdrawalPolicyRequest = {
      minDaysHeld:   values.minDaysHeld,
      useDemandRate: values.useDemandRate,
      demandRate:    values.demandRate  ?? 0,
      penaltyRate:   values.penaltyRate ?? 0,
    };
    upsert.mutate(req, {
      onSuccess: () => onClose(),
    });
  };

  return (
    <Modal
      title={initialValues ? 'Cập nhật chính sách rút sớm' : 'Tạo chính sách rút sớm'}
      open={open}
      onCancel={onClose}
      onOk={handleOk}
      confirmLoading={upsert.isPending}
      okText="Lưu"
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        style={{ marginTop: 16 }}
        initialValues={initialValues ?? {
          minDaysHeld:   30,
          penaltyRate:   0,
          useDemandRate: true,
          demandRate:    0.5,
        }}
      >
        <Form.Item
          name="minDaysHeld"
          label="Số ngày tối thiểu phải nắm giữ"
          tooltip="Khách hàng chỉ được rút sớm sau khi đã gửi đủ số ngày này"
          rules={[{ required: true }, { type: 'number', min: 0 }]}
        >
          <InputNumber style={{ width: '100%' }} min={0} addonAfter="ngày" />
        </Form.Item>

        <Form.Item
          name="useDemandRate"
          label="Tính lãi theo lãi suất không kỳ hạn khi rút sớm"
          valuePropName="checked"
        >
          <Switch checkedChildren="Có" unCheckedChildren="Không" />
        </Form.Item>

        {/* demandRate — visible only when useDemandRate=true */}
        <Form.Item
          name="demandRate"
          label="Lãi suất không kỳ hạn (%/năm)"
          hidden={!useDemand}
          rules={[
            { required: !!useDemand, message: 'Nhập lãi suất không kỳ hạn' },
            { type: 'number', min: 0, message: 'Phải >= 0' },
          ]}
        >
          <InputNumber style={{ width: '100%' }} min={0} max={100} step={0.1} precision={2} addonAfter="%/năm" />
        </Form.Item>

        {/* penaltyRate — visible only when useDemandRate=false */}
        <Form.Item
          name="penaltyRate"
          label="Lãi suất phạt (%/năm)"
          tooltip="Lãi suất áp dụng thay thế lãi suất hợp đồng khi rút sớm"
          hidden={!!useDemand}
          rules={[
            { required: !useDemand, message: 'Nhập lãi suất phạt' },
            { type: 'number', min: 0, message: 'Phải >= 0' },
          ]}
        >
          <InputNumber style={{ width: '100%' }} min={0} max={100} step={0.1} precision={2} addonAfter="%/năm" />
        </Form.Item>
      </Form>
    </Modal>
  );
}

// ══════════════════════════════════════════════════════════════════
// Main Page
// ══════════════════════════════════════════════════════════════════

export default function ProductDetailPage() {
  const { productCode } = useParams<{ productCode: string }>();
  const navigate        = useNavigate();
  const { isAdmin }     = useAuthStore();

  // Modal state
  const [termModalOpen, setTermModalOpen]   = useState(false);
  const [rateModalOpen, setRateModalOpen]   = useState(false);
  const [policyModalOpen, setPolicyModalOpen] = useState(false);

  // Data
  const { data: product,  isLoading: loadingProduct, isError: errorProduct } = useProduct(productCode);
  // allTerms = active + inactive → used for the terms TABLE (so disabled terms stay visible with "Bật" button)
  const { data: allTerms = [], isLoading: loadingTerms } = useAllProductTerms(productCode);
  // activeTerms = active only → used for the count badge in Descriptions
  const { data: activeTerms = [] }                       = useProductTerms(productCode);
  const { data: rates  = [], isLoading: loadingRates }   = useProductRates(productCode);
  const { data: ewPolicy, isLoading: loadingPolicy }     = useEarlyWithdrawalPolicy(productCode);

  // Mutations
  const toggleStatus = useToggleProductStatus(productCode);
  const updateTerm   = useUpdateTerm(productCode);

  const handleEdit = () => navigate(buildPath(ROUTES.PRODUCT_EDIT, { productCode: productCode! }));

  const handleToggleStatus = () => {
    if (!product) return;
    Modal.confirm({
      title:  product.isActive ? 'Vô hiệu hoá sản phẩm?' : 'Kích hoạt lại sản phẩm?',
      icon:   <ExclamationCircleOutlined style={{ color: product.isActive ? '#ff4d4f' : '#52c41a' }} />,
      content: product.isActive ? (
        <div>
          <p>Sản phẩm <strong>"{product.productName}"</strong> sẽ bị vô hiệu hoá. Khách hàng không thể mở sổ mới.</p>
          <Alert
            type="info" showIcon
            message="Các sổ tiết kiệm hiện tại không bị ảnh hưởng"
            description="Tất cả hợp đồng đang hoạt động tiếp tục bình thường đến khi tất toán."
            style={{ marginTop: 8, borderRadius: 6 }}
          />
        </div>
      ) : (
        <p>Kích hoạt lại <strong>"{product.productName}"</strong>? Khách hàng có thể mở sổ mới.</p>
      ),
      okText: product.isActive ? 'Vô hiệu hoá' : 'Kích hoạt',
      okType: product.isActive ? 'danger' : 'primary',
      cancelText: 'Huỷ',
      onOk: () => toggleStatus.mutate(!product.isActive),
    });
  };

  const handleToggleTerm = (term: SavingTerm) => {
    Modal.confirm({
      title:  term.isActive ? 'Vô hiệu hoá kỳ hạn?' : 'Kích hoạt kỳ hạn?',
      icon:   <ExclamationCircleOutlined style={{ color: term.isActive ? '#ff4d4f' : '#52c41a' }} />,
      content: term.isActive ? (
        <div>
          <p>Kỳ hạn <strong>{term.termLabel}</strong> sẽ không thể dùng để mở sổ mới.</p>
          <Alert type="info" showIcon message="Hợp đồng cũ dùng kỳ hạn này vẫn hoạt động bình thường." style={{ marginTop: 8, borderRadius: 6 }} />
        </div>
      ) : (
        <p>Kích hoạt lại kỳ hạn <strong>{term.termLabel}</strong>?</p>
      ),
      okText:  term.isActive ? 'Vô hiệu hoá' : 'Kích hoạt',
      okType:  term.isActive ? 'danger' : 'primary',
      cancelText: 'Huỷ',
      onOk: () => updateTerm.mutate({ termId: term.termId, req: { isActive: !term.isActive } }),
    });
  };

  // ── Term table columns ───────────────────────────────────────────

  const termColumns: ColumnsType<SavingTerm> = [
    {
      title:     'Kỳ hạn',
      dataIndex: 'termLabel',
      key:       'termLabel',
      render:    (label: string, row) => (
        <Space>
          <Text strong>{label}</Text>
          {!row.isActive && <Tag color="default">Ngừng</Tag>}
        </Space>
      ),
    },
    { title: 'Số tháng', dataIndex: 'termMonths', key: 'termMonths', align: 'center',
      render: (v: number) => v ? `${v} tháng` : '—' },
    { title: 'Số ngày', dataIndex: 'termDays', key: 'termDays', align: 'center',
      render: (v: number) => `${v} ngày` },
    { title: 'Lãi suất / năm', dataIndex: 'annualRate', key: 'annualRate', align: 'right',
      render: (v: number | undefined) =>
        v != null
          ? <Text strong style={{ color: '#1677ff' }}>{v.toFixed(2)}%</Text>
          : <Text type="secondary">Chưa có lãi suất</Text>,
    },
    ...(isAdmin() ? [{
      title: 'Thao tác',
      key:   'actions',
      align: 'center' as const,
      render: (_: unknown, row: SavingTerm) => (
        <Space>
          <Tooltip title={row.isActive ? 'Vô hiệu hoá kỳ hạn này (hợp đồng cũ không đổi)' : 'Kích hoạt lại'}>
            <Button
              size="small"
              danger={row.isActive}
              onClick={() => handleToggleTerm(row)}
              loading={updateTerm.isPending}
            >
              {row.isActive ? 'Tắt' : 'Bật'}
            </Button>
          </Tooltip>
        </Space>
      ),
    }] : []),
  ];

  // ── Rate history columns ─────────────────────────────────────────

  const rateColumns: ColumnsType<InterestRate> = [
    { title: 'Mã kỳ hạn', dataIndex: 'termId', key: 'termId',
      render: (v: string) => <Text code>{v}</Text> },
    { title: 'Lãi suất / năm', dataIndex: 'annualRate', key: 'annualRate', align: 'right',
      render: (v: number) => <Text strong>{v?.toFixed(2)}%</Text> },
    { title: 'Hiệu lực từ', dataIndex: 'effectiveFrom', key: 'effectiveFrom',
      render: (v: string) => formatDate(v) },
    { title: 'Hiệu lực đến', dataIndex: 'effectiveTo', key: 'effectiveTo',
      render: (v: string | null) => v ? formatDate(v) : <Tag color="success">Hiện tại</Tag> },
    { title: 'Trạng thái', dataIndex: 'isActive', key: 'isActive', align: 'center',
      render: (v: boolean) =>
        v ? <Badge status="success" text="Đang áp dụng" />
          : <Badge status="default" text="Đã hết hạn" /> },
  ];

  // ── Error / loading ──────────────────────────────────────────────

  if (errorProduct) {
    return (
      <div>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(ROUTES.PRODUCTS)} style={{ marginBottom: 16 }}>
          Quay lại
        </Button>
        <Alert type="error" message="Không tìm thấy sản phẩm"
          description={`Sản phẩm "${productCode}" không tồn tại hoặc đã bị xóa.`}
          showIcon action={<Button onClick={() => navigate(ROUTES.PRODUCTS)}>Về danh sách</Button>}
        />
      </div>
    );
  }

  // ── Render ────────────────────────────────────────────────────────

  return (
    <div>
      {/* Back */}
      <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(ROUTES.PRODUCTS)} style={{ marginBottom: 16 }}>
        Danh sách sản phẩm
      </Button>

      {/* Header card */}
      <Card style={{ borderRadius: 12, marginBottom: 20 }}>
        {loadingProduct ? <Skeleton active paragraph={{ rows: 3 }} /> : product ? (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
            <div>
              <Space align="center" style={{ marginBottom: 8 }}>
                <Title level={4} style={{ margin: 0 }}>{product.productName}</Title>
                {product.isActive
                  ? <Tag icon={<CheckCircleOutlined />} color="success">Đang hoạt động</Tag>
                  : <Tag icon={<StopOutlined />}        color="default">Ngừng hoạt động</Tag>}
              </Space>
              <Space size="large" wrap>
                <Text type="secondary">Mã: <Text code>{product.productCode}</Text></Text>
                <Text type="secondary">Tiền tệ: <Text strong>{product.currency}</Text></Text>
                <Text type="secondary">Trả lãi:{' '}
                  <Tag color="blue" style={{ margin: 0 }}>
                    {PAYMENT_METHOD_LABELS[product.interestPaymentMethod]}
                  </Tag>
                </Text>
              </Space>
              {product.description && (
                <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>{product.description}</Text>
              )}
            </div>

            <Space wrap>
              <Tooltip title="Tính lãi mô phỏng cho sản phẩm này">
                <Button icon={<CalculatorOutlined />} onClick={() => navigate(`${ROUTES.SIMULATE}?productCode=${productCode}`)}>
                  Mô phỏng lãi
                </Button>
              </Tooltip>

              {isAdmin() && (
                <>
                  <Tooltip title="Chỉnh sửa tên, hạn mức và mô tả">
                    <Button icon={<EditOutlined />} onClick={handleEdit}>
                      Chỉnh sửa
                    </Button>
                  </Tooltip>
                  <Tooltip title={product.isActive ? 'Vô hiệu hoá — sổ cũ vẫn hoạt động' : 'Kích hoạt lại sản phẩm'}>
                    <Button
                      danger={product.isActive}
                      icon={product.isActive ? <StopOutlined /> : <PoweroffOutlined />}
                      loading={toggleStatus.isPending}
                      onClick={handleToggleStatus}
                    >
                      {product.isActive ? 'Vô hiệu hoá' : 'Kích hoạt lại'}
                    </Button>
                  </Tooltip>
                </>
              )}
            </Space>
          </div>
        ) : null}
      </Card>

      {/* Main content */}
      <Card style={{ borderRadius: 12 }}>
        {product && (
          <Descriptions column={{ xs: 1, sm: 2, md: 3 }} style={{ marginBottom: 24 }} size="small">
            <Descriptions.Item label="Số tiền gửi tối thiểu">
              <Text strong>{formatVND(product.minAmount)}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Số tiền gửi tối đa">
              <Text strong>{formatVND(product.maxAmount)}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Số kỳ hạn">
              <Text strong>{activeTerms.length} kỳ hạn đang hoạt động</Text>
              {allTerms.length > activeTerms.length && (
                <Text type="secondary" style={{ fontSize: 12, marginLeft: 6 }}>
                  ({allTerms.length - activeTerms.length} đã tắt)
                </Text>
              )}
            </Descriptions.Item>
          </Descriptions>
        )}

        <Tabs
          defaultActiveKey="terms"
          items={[

            // ── Tab 1: Kỳ hạn & Lãi suất ──────────────────────────
            {
              key:   'terms',
              label: `Kỳ hạn & Lãi suất (${activeTerms.length}${allTerms.length > activeTerms.length ? `/${allTerms.length}` : ''})`,
              children: (
                <div>
                  {isAdmin() && (
                    <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
                      <Button
                        type="primary"
                        icon={<PlusOutlined />}
                        onClick={() => setTermModalOpen(true)}
                      >
                        Thêm kỳ hạn
                      </Button>
                    </div>
                  )}
                  <Table<SavingTerm>
                    dataSource={allTerms}
                    columns={termColumns}
                    rowKey="termId"
                    loading={loadingTerms}
                    pagination={false}
                    size="middle"
                    locale={{ emptyText: 'Chưa có kỳ hạn nào' }}
                  />
                </div>
              ),
            },

            // ── Tab 2: Lịch sử lãi suất ────────────────────────────
            {
              key:   'rates',
              label: 'Lịch sử lãi suất',
              children: (
                <div>
                  {isAdmin() && (
                    <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
                      <Button
                        type="primary"
                        icon={<PlusOutlined />}
                        onClick={() => setRateModalOpen(true)}
                        disabled={allTerms.length === 0}
                      >
                        Thêm lãi suất
                      </Button>
                    </div>
                  )}
                  <Table<InterestRate>
                    dataSource={rates}
                    columns={rateColumns}
                    rowKey={(r) => `${r.termId}-${r.effectiveFrom}`}
                    loading={loadingRates}
                    pagination={{ pageSize: 10, showSizeChanger: false }}
                    size="middle"
                    locale={{ emptyText: 'Chưa có dữ liệu lãi suất' }}
                  />
                </div>
              ),
            },

            // ── Tab 3: Rút sớm ─────────────────────────────────────
            {
              key:   'early-withdrawal',
              label: 'Rút sớm',
              children: loadingPolicy ? (
                <Skeleton active />
              ) : (
                <div>
                  {isAdmin() && (
                    <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
                      <Button
                        type={ewPolicy ? 'default' : 'primary'}
                        icon={ewPolicy ? <EditOutlined /> : <PlusOutlined />}
                        onClick={() => setPolicyModalOpen(true)}
                      >
                        {ewPolicy ? 'Cập nhật chính sách' : 'Tạo chính sách'}
                      </Button>
                    </div>
                  )}

                  {ewPolicy ? (
                    <Descriptions bordered column={1} size="small" style={{ maxWidth: 480 }}>
                      <Descriptions.Item label="Số ngày tối thiểu">
                        <Text strong>{ewPolicy.minDaysHeld} ngày</Text>
                      </Descriptions.Item>
                      <Descriptions.Item label="Dùng lãi suất không kỳ hạn">
                        <Tag color={ewPolicy.useDemandRate ? 'success' : 'default'}>
                          {ewPolicy.useDemandRate ? 'Có' : 'Không'}
                        </Tag>
                      </Descriptions.Item>
                      {ewPolicy.useDemandRate ? (
                        <Descriptions.Item label="Lãi suất không kỳ hạn">
                          <Text strong>{ewPolicy.demandRate.toFixed(2)}%/năm</Text>
                        </Descriptions.Item>
                      ) : (
                        <Descriptions.Item label="Lãi suất phạt">
                          <Text strong style={{ color: '#ff4d4f' }}>
                            {ewPolicy.penaltyRate.toFixed(2)}%/năm
                          </Text>
                        </Descriptions.Item>
                      )}
                    </Descriptions>
                  ) : (
                    <Alert
                      type="info"
                      message="Chưa có chính sách rút trước hạn"
                      description={
                        isAdmin()
                          ? 'Nhấn "Tạo chính sách" để thiết lập điều khoản rút sớm cho sản phẩm này.'
                          : 'Sản phẩm này không có chính sách rút trước hạn.'
                      }
                      showIcon
                      style={{ maxWidth: 480 }}
                    />
                  )}
                </div>
              ),
            },
          ]}
        />
      </Card>

      {/* ── Modals ──────────────────────────────────────────────── */}

      {productCode && (
        <>
          <AddTermModal
            open={termModalOpen}
            productCode={productCode}
            onClose={() => setTermModalOpen(false)}
          />
          <AddRateModal
            open={rateModalOpen}
            productCode={productCode}
            terms={allTerms}
            onClose={() => setRateModalOpen(false)}
          />
          <EarlyWithdrawalModal
            open={policyModalOpen}
            productCode={productCode}
            initialValues={ewPolicy ?? null}
            onClose={() => setPolicyModalOpen(false)}
          />
        </>
      )}
    </div>
  );
}
