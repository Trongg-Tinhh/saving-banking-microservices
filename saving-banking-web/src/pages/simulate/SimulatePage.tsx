import { useEffect } from 'react';
import {
  Typography,
  Card,
  Form,
  Select,
  InputNumber,
  DatePicker,
  Button,
  Row,
  Col,
  Statistic,
  Table,
  Tag,
  Divider,
  Space,
  Alert,
  Skeleton,
  Empty,
} from 'antd';
import {
  CalculatorOutlined,
  CalendarOutlined,
  DollarOutlined,
  PercentageOutlined,
  ClearOutlined,
} from '@ant-design/icons';
import { useSearchParams } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import { dayjs, formatDate } from '@/utils/formatDate';
import type { Dayjs } from 'dayjs';
import { useProducts, useProductTerms } from '@/hooks/useProducts';
import { useSimulate } from '@/hooks/useInterest';
import { formatVND } from '@/utils/formatCurrency';
import type { InterestScheduleItem, InterestPaymentMethod, SavingTerm } from '@/types';

const { Title, Text } = Typography;

// ─── Constants ────────────────────────────────────────────────────

const PAYMENT_LABELS: Record<InterestPaymentMethod, string> = {
  END_OF_TERM: 'Cuối kỳ',
  MONTHLY:     'Hàng tháng',
  QUARTERLY:   'Hàng quý',
};

// ─── Schedule table columns ───────────────────────────────────────

const SCHEDULE_COLUMNS: ColumnsType<InterestScheduleItem> = [
  {
    title: 'Kỳ số',
    dataIndex: 'periodNo',
    key: 'periodNo',
    width: 70,
    align: 'center',
    render: (v: number) => <Tag color="blue">{v}</Tag>,
  },
  {
    title: 'Từ ngày',
    dataIndex: 'fromDate',
    key: 'fromDate',
    render: (v: string) => formatDate(v),
  },
  {
    title: 'Đến ngày',
    dataIndex: 'toDate',
    key: 'toDate',
    render: (v: string) => formatDate(v),
  },
  {
    title: 'Số ngày',
    dataIndex: 'days',
    key: 'days',
    align: 'center',
    render: (v: number) => `${v} ngày`,
  },
  {
    title: 'Tiền lãi',
    dataIndex: 'interest',
    key: 'interest',
    align: 'right',
    render: (v: number) => (
      <Text strong style={{ color: '#52c41a' }}>{formatVND(v)}</Text>
    ),
  },
];

// ─── Form shape ───────────────────────────────────────────────────

interface SimulateForm {
  productCode: string;
  termId: string;
  principal: number;
  startDate: Dayjs;
}

// ─── Page ─────────────────────────────────────────────────────────

export default function SimulatePage() {
  const [searchParams] = useSearchParams();
  const [form] = Form.useForm<SimulateForm>();

  const preFilledProduct = searchParams.get('productCode') ?? undefined;

  // Watched form values
  const selectedProduct = Form.useWatch('productCode', form);
  const selectedTermId  = Form.useWatch('termId', form);

  // Queries
  const { data: products = [], isLoading: loadingProducts } = useProducts(true); // only active
  const { data: terms = [],    isLoading: loadingTerms }    = useProductTerms(selectedProduct);

  // Simulation mutation
  const simulate = useSimulate();
  const result   = simulate.data;

  // Pre-fill product from URL param
  useEffect(() => {
    if (preFilledProduct) {
      form.setFieldValue('productCode', preFilledProduct);
    }
  }, [preFilledProduct, form]);

  // Reset term when product changes
  useEffect(() => {
    form.setFieldValue('termId', undefined);
  }, [selectedProduct, form]);

  // Auto-fill annualRate label (stored internally for display)
  const selectedTerm: SavingTerm | undefined = terms.find((t) => t.termId === selectedTermId);

  // ─── Submit ─────────────────────────────────────────────────────

  const handleSubmit = async (values: SimulateForm) => {
    if (!selectedTerm) {
      form.setFields([{ name: 'termId', errors: ['Chọn kỳ hạn trước khi tính toán'] }]);
      return;
    }
    if (selectedTerm.annualRate == null) {
      form.setFields([{ name: 'termId', errors: ['Kỳ hạn này chưa có lãi suất'] }]);
      return;
    }

    const startDate = values.startDate.format('YYYY-MM-DD');
    const product   = products.find((p) => p.productCode === values.productCode);
    const paymentMethod: InterestPaymentMethod = product?.interestPaymentMethod ?? 'END_OF_TERM';

    try {
      await simulate.mutateAsync({
        principal:     values.principal,
        annualRate:    selectedTerm.annualRate,
        termDays:      selectedTerm.termDays ?? 0,
        paymentMethod,
        startDate,
      });
    } catch {
      // lỗi đã được xử lý bởi onError trong useSimulate
    }
  };

  const handleCalculate = () => {
    form.submit();
  };

  const handleReset = () => {
    form.resetFields();
    simulate.reset();
  };

  // ─── Render ──────────────────────────────────────────────────────

  const hasSchedule =
    result &&
    result.schedule.length > 0 &&
    (products.find((p) => p.productCode === selectedProduct)?.interestPaymentMethod !== 'END_OF_TERM');

  return (
    <div>
      {/* Page title */}
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ margin: 0 }}>Mô phỏng lãi suất</Title>
        <Text type="secondary">
          Tính toán lãi và lịch trả lãi dự kiến cho một khoản tiết kiệm
        </Text>
      </div>

      <Row gutter={[20, 20]} align="top">
        {/* ── Left: Form ── */}
        <Col xs={24} lg={9}>
          <Card
            title={
              <Space>
                <CalculatorOutlined />
                <span>Thông tin mô phỏng</span>
              </Space>
            }
            style={{ borderRadius: 12, position: 'sticky', top: 80 }}
          >
            <Form
              form={form}
              layout="vertical"
              onFinish={handleSubmit}
              onFinishFailed={() => {
                // validation errors already shown under each field
              }}
              initialValues={{
                startDate: dayjs(),
                principal: 50_000_000,
              }}
              requiredMark={false}
            >
              {/* Product */}
              <Form.Item
                name="productCode"
                label="Sản phẩm tiết kiệm"
                rules={[{ required: true, message: 'Chọn sản phẩm' }]}
              >
                <Select
                  placeholder="Chọn sản phẩm..."
                  loading={loadingProducts}
                  showSearch
                  optionFilterProp="label"
                  options={products.map((p) => ({
                    value: p.productCode,
                    label: `${p.productName} (${p.productCode})`,
                  }))}
                  notFoundContent={loadingProducts ? <Skeleton active paragraph={{ rows: 2 }} /> : 'Không có sản phẩm'}
                />
              </Form.Item>

              {/* Term */}
              <Form.Item
                name="termId"
                label={
                  <Space size={4}>
                    <CalendarOutlined />
                    <span>Kỳ hạn</span>
                  </Space>
                }
                rules={[{ required: true, message: 'Chọn kỳ hạn' }]}
              >
                <Select
                  placeholder={selectedProduct ? 'Chọn kỳ hạn...' : 'Chọn sản phẩm trước'}
                  disabled={!selectedProduct}
                  loading={loadingTerms}
                  options={terms
                    .filter((t) => t.isActive)
                    .map((t) => ({
                      value: t.termId,
                      label: `${t.termLabel}${t.annualRate != null ? ` — ${t.annualRate.toFixed(2)}%/năm` : ''}`,
                    }))}
                  notFoundContent={loadingTerms ? <Skeleton active paragraph={{ rows: 1 }} /> : 'Không có kỳ hạn'}
                />
              </Form.Item>

              {/* Rate display (read-only) */}
              {selectedTerm && (
                <div style={{
                  marginBottom: 16,
                  padding: '10px 14px',
                  background: '#f0f7ff',
                  borderRadius: 8,
                  border: '1px solid #bae0ff',
                }}>
                  <Space size="large" wrap>
                    <span>
                      <PercentageOutlined style={{ color: '#1677ff', marginRight: 4 }} />
                      <Text type="secondary">Lãi suất: </Text>
                      <Text strong style={{ color: '#1677ff' }}>
                        {selectedTerm.annualRate?.toFixed(2) ?? '—'}%/năm
                      </Text>
                    </span>
                    <span>
                      <CalendarOutlined style={{ color: '#52c41a', marginRight: 4 }} />
                      <Text type="secondary">Thời gian: </Text>
                      <Text strong>{selectedTerm.termDays} ngày</Text>
                    </span>
                  </Space>
                </div>
              )}

              {/* Principal */}
              <Form.Item
                name="principal"
                label={
                  <Space size={4}>
                    <DollarOutlined />
                    <span>Số tiền gửi (VND)</span>
                  </Space>
                }
                rules={[
                  { required: true, message: 'Nhập số tiền gửi' },
                  {
                    validator: (_, value) =>
                      value >= 1_000_000
                        ? Promise.resolve()
                        : Promise.reject(new Error('Tối thiểu 1.000.000 ₫')),
                  },
                ]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={1_000_000}
                  step={1_000_000}
                  formatter={(v) =>
                    `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
                  }
                  parser={(v) => (Number(v?.replace(/\./g, '') ?? 0) as unknown) as 1000000}
                  addonAfter="₫"
                  placeholder="50.000.000"
                />
              </Form.Item>

              {/* Start date */}
              <Form.Item
                name="startDate"
                label="Ngày bắt đầu gửi"
                rules={[{ required: true, message: 'Chọn ngày bắt đầu' }]}
              >
                <DatePicker
                  style={{ width: '100%' }}
                  format="DD/MM/YYYY"
                  placeholder="Chọn ngày..."
                  disabledDate={(d) => d.isBefore(dayjs().subtract(1, 'day'))}
                />
              </Form.Item>

              {/* Buttons */}
              <Form.Item style={{ marginBottom: 0 }}>
                <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                  <Button
                    icon={<ClearOutlined />}
                    onClick={handleReset}
                    disabled={simulate.isPending}
                  >
                    Xóa
                  </Button>
                  <Button
                    type="primary"
                    htmlType="submit"
                    onClick={handleCalculate}
                    icon={<CalculatorOutlined />}
                    loading={simulate.isPending}
                    disabled={simulate.isPending}
                    style={{ flex: 1, marginLeft: 8 }}
                  >
                    Tính toán
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        {/* ── Right: Results ── */}
        <Col xs={24} lg={15}>
          {simulate.isError && (
            <Alert
              type="error"
              message="Lỗi tính toán"
              description="Dịch vụ lãi suất hiện không khả dụng. Vui lòng thử lại sau."
              showIcon
              style={{ marginBottom: 16, borderRadius: 8 }}
            />
          )}

          {!result && !simulate.isPending && (
            <Card style={{ borderRadius: 12 }}>
              <Empty
                image={<CalculatorOutlined style={{ fontSize: 56, color: '#d9d9d9' }} />}
                description={
                  <Text type="secondary">
                    Nhập thông tin bên trái và nhấn{' '}
                    <Text strong>Tính toán</Text> để xem kết quả mô phỏng.
                  </Text>
                }
              />
            </Card>
          )}

          {simulate.isPending && (
            <Card style={{ borderRadius: 12 }}>
              <Skeleton active paragraph={{ rows: 6 }} />
            </Card>
          )}

          {result && !simulate.isPending && (
            <>
              {/* Summary cards */}
              <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                <Col xs={12} sm={6}>
                  <Card style={{ borderRadius: 10, textAlign: 'center', background: '#f0f7ff', border: '1px solid #bae0ff' }}>
                    <Statistic
                      title={<Text style={{ fontSize: 12 }}>Gốc gửi</Text>}
                      value={result.principal}
                      formatter={(v) => formatVND(Number(v))}
                      valueStyle={{ fontSize: 16, color: '#1677ff' }}
                    />
                  </Card>
                </Col>
                <Col xs={12} sm={6}>
                  <Card style={{ borderRadius: 10, textAlign: 'center', background: '#f6ffed', border: '1px solid #b7eb8f' }}>
                    <Statistic
                      title={<Text style={{ fontSize: 12 }}>Tổng lãi</Text>}
                      value={result.totalInterest}
                      formatter={(v) => formatVND(Number(v))}
                      valueStyle={{ fontSize: 16, color: '#52c41a' }}
                    />
                  </Card>
                </Col>
                <Col xs={12} sm={6}>
                  <Card style={{ borderRadius: 10, textAlign: 'center', background: '#fff7e6', border: '1px solid #ffd591' }}>
                    <Statistic
                      title={<Text style={{ fontSize: 12 }}>Tổng nhận</Text>}
                      value={result.totalPayout}
                      formatter={(v) => formatVND(Number(v))}
                      valueStyle={{ fontSize: 16, color: '#fa8c16' }}
                    />
                  </Card>
                </Col>
                <Col xs={12} sm={6}>
                  <Card style={{ borderRadius: 10, textAlign: 'center', background: '#fff2f0', border: '1px solid #ffccc7' }}>
                    <Statistic
                      title={<Text style={{ fontSize: 12 }}>Lãi suất/năm</Text>}
                      value={result.annualRate?.toFixed(2) ?? '0'}
                      suffix="%"
                      valueStyle={{ fontSize: 16, color: '#ff4d4f' }}
                    />
                  </Card>
                </Col>
              </Row>

              {/* Maturity info */}
              <Card style={{ borderRadius: 12, marginBottom: 16 }}>
                <Row gutter={16}>
                  <Col xs={24} sm={8}>
                    <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>Ngày đáo hạn</Text>
                    <Text strong style={{ fontSize: 16 }}>
                      {formatDate(result.maturityDate)}
                    </Text>
                  </Col>
                  <Col xs={24} sm={8}>
                    <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>Kỳ hạn</Text>
                    <Text strong style={{ fontSize: 16 }}>{result.termDays} ngày</Text>
                  </Col>
                  <Col xs={24} sm={8}>
                    <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>Hình thức trả lãi</Text>
                    <Tag color="blue" style={{ marginTop: 2 }}>
                      {PAYMENT_LABELS[
                        (products.find((p) => p.productCode === selectedProduct)?.interestPaymentMethod) ?? 'END_OF_TERM'
                      ]}
                    </Tag>
                  </Col>
                </Row>
              </Card>

              {/* Schedule table (only for periodic payments) */}
              {hasSchedule ? (
                <Card
                  title={
                    <Space>
                      <CalendarOutlined />
                      <span>Lịch trả lãi ({result.schedule.length} kỳ)</span>
                    </Space>
                  }
                  style={{ borderRadius: 12 }}
                >
                  <Table<InterestScheduleItem>
                    dataSource={result.schedule}
                    columns={SCHEDULE_COLUMNS}
                    rowKey="periodNo"
                    pagination={result.schedule.length > 12 ? { pageSize: 12, showSizeChanger: false } : false}
                    size="small"
                    summary={(data) => {
                      const total = data.reduce((sum, r) => sum + r.interest, 0);
                      return (
                        <Table.Summary fixed>
                          <Table.Summary.Row>
                            <Table.Summary.Cell index={0} colSpan={4} align="right">
                              <Text strong>Tổng lãi</Text>
                            </Table.Summary.Cell>
                            <Table.Summary.Cell index={1} align="right">
                              <Text strong style={{ color: '#52c41a' }}>
                                {formatVND(total)}
                              </Text>
                            </Table.Summary.Cell>
                          </Table.Summary.Row>
                        </Table.Summary>
                      );
                    }}
                  />
                </Card>
              ) : result ? (
                <Card style={{ borderRadius: 12 }}>
                  <Divider plain>
                    <Text type="secondary" style={{ fontSize: 13 }}>
                      Lãi suất được nhận một lần vào ngày đáo hạn{' '}
                      <Text strong>{formatDate(result.maturityDate)}</Text>
                    </Text>
                  </Divider>
                  <div style={{ textAlign: 'center', padding: '12px 0' }}>
                    <Text type="secondary">Hình thức cuối kỳ không có lịch trả lãi định kỳ.</Text>
                  </div>
                </Card>
              ) : null}
            </>
          )}
        </Col>
      </Row>
    </div>
  );
}
