import { useEffect } from 'react';
import {
  Typography,
  Card,
  Form,
  Input,
  InputNumber,
  Select,
  Button,
  Space,
  Divider,
  Alert,
  Row,
  Col,
  Skeleton,
} from 'antd';
import {
  ArrowLeftOutlined,
  SaveOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { useProduct } from '@/hooks/useProducts';
import { useCreateProduct, useUpdateProduct } from '@/hooks/useProducts';
import { ROUTES, buildPath } from '@/constants/routes';
import { formatVND } from '@/utils/formatCurrency';
import type { InterestPaymentMethod, CreateProductRequest, UpdateProductRequest } from '@/types';

const { Title, Text } = Typography;

const PAYMENT_METHOD_OPTIONS: { value: InterestPaymentMethod; label: string; description: string }[] = [
  { value: 'END_OF_TERM', label: 'Cuối kỳ',      description: 'Trả toàn bộ lãi khi đáo hạn' },
  { value: 'MONTHLY',     label: 'Hàng tháng',   description: 'Trả lãi vào đầu mỗi tháng' },
  { value: 'QUARTERLY',   label: 'Hàng quý',     description: 'Trả lãi vào đầu mỗi quý' },
];

type FormValues = {
  productCode:           string;
  productName:           string;
  currency:              string;
  minAmount:             number;
  maxAmount:             number;
  interestPaymentMethod: InterestPaymentMethod;
  description?:          string;
};

// ─── Page ──────────────────────────────────────────────────────────

export default function ProductFormPage() {
  const { productCode } = useParams<{ productCode: string }>();
  const isEditMode = Boolean(productCode);
  const navigate   = useNavigate();
  const [form]     = Form.useForm<FormValues>();

  // Load existing data in edit mode
  const { data: product, isLoading } = useProduct(isEditMode ? productCode : undefined);

  // Mutations
  const createProduct = useCreateProduct();
  const updateProduct = useUpdateProduct(productCode);

  const isPending = createProduct.isPending || updateProduct.isPending;

  // Pre-fill form when editing
  useEffect(() => {
    if (isEditMode && product) {
      form.setFieldsValue({
        productCode:           product.productCode,
        productName:           product.productName,
        currency:              product.currency,
        minAmount:             product.minAmount,
        maxAmount:             product.maxAmount,
        interestPaymentMethod: product.interestPaymentMethod,
        description:           product.description ?? '',
      });
    }
  }, [product, isEditMode, form]);

  // ── Submit ────────────────────────────────────────────────────

  const handleSubmit = async () => {
    const values = await form.validateFields();

    if (isEditMode) {
      const req: UpdateProductRequest = {
        productName:  values.productName,
        minAmount:    values.minAmount,
        maxAmount:    values.maxAmount,
        description:  values.description || undefined,
      };
      updateProduct.mutate(req);
    } else {
      const req: CreateProductRequest = {
        productCode:           values.productCode.toUpperCase(),
        productName:           values.productName,
        currency:              values.currency,
        minAmount:             values.minAmount,
        maxAmount:             values.maxAmount,
        interestPaymentMethod: values.interestPaymentMethod,
        description:           values.description || undefined,
      };
      createProduct.mutate(req);
    }
  };

  const backTo = isEditMode && productCode
    ? buildPath(ROUTES.PRODUCT_DETAIL, { productCode })
    : ROUTES.PRODUCTS;

  // ── Render ────────────────────────────────────────────────────

  return (
    <div style={{ maxWidth: 680, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(backTo)}
          style={{ marginBottom: 16 }}
        >
          {isEditMode ? 'Chi tiết sản phẩm' : 'Danh sách sản phẩm'}
        </Button>
        <Title level={4} style={{ margin: 0 }}>
          {isEditMode ? 'Chỉnh sửa sản phẩm' : 'Tạo sản phẩm mới'}
        </Title>
        <Text type="secondary">
          {isEditMode
            ? 'Cập nhật tên, hạn mức gửi và mô tả sản phẩm.'
            : 'Điền thông tin để tạo sản phẩm tiết kiệm mới.'}
        </Text>
      </div>

      {isEditMode && isLoading ? (
        <Card style={{ borderRadius: 12 }}>
          <Skeleton active paragraph={{ rows: 8 }} />
        </Card>
      ) : (
        <Card style={{ borderRadius: 12 }}>
          <Form
            form={form}
            layout="vertical"
            size="large"
            initialValues={{ currency: 'VND', interestPaymentMethod: 'END_OF_TERM' }}
          >

            {/* ── Mã & Tên sản phẩm ─────────────────────────── */}
            <Divider plain style={{ marginTop: 0 }}>Thông tin cơ bản</Divider>

            <Row gutter={16}>
              <Col xs={24} sm={10}>
                <Form.Item
                  name="productCode"
                  label="Mã sản phẩm"
                  rules={[
                    { required: true, message: 'Nhập mã sản phẩm' },
                    {
                      pattern: /^[A-Z0-9_]+$/,
                      message: 'Chỉ chữ IN HOA, số và dấu gạch dưới (_)',
                    },
                    { max: 50, message: 'Tối đa 50 ký tự' },
                  ]}
                  tooltip="Ví dụ: TERM_SAVING_MONTHLY. Không thể thay đổi sau khi tạo."
                  normalize={(v: string) => v?.toUpperCase()}
                >
                  <Input
                    placeholder="TERM_SAVING_12M"
                    disabled={isEditMode}
                    style={isEditMode ? { color: '#595959' } : undefined}
                  />
                </Form.Item>
              </Col>
              <Col xs={24} sm={14}>
                <Form.Item
                  name="productName"
                  label="Tên sản phẩm"
                  rules={[
                    { required: true, message: 'Nhập tên sản phẩm' },
                    { max: 200, message: 'Tối đa 200 ký tự' },
                  ]}
                >
                  <Input placeholder="Tiết kiệm có kỳ hạn 12 tháng" />
                </Form.Item>
              </Col>
            </Row>

            {/* ── Tiền tệ & Phương thức trả lãi ─────────────── */}
            <Row gutter={16}>
              <Col xs={24} sm={10}>
                <Form.Item
                  name="currency"
                  label="Tiền tệ"
                  rules={[{ required: true }]}
                  tooltip="Không thể thay đổi sau khi tạo."
                >
                  <Select disabled={isEditMode} options={[
                    { value: 'VND', label: '🇻🇳 VND — Việt Nam Đồng' },
                    { value: 'USD', label: '🇺🇸 USD — US Dollar' },
                    { value: 'EUR', label: '🇪🇺 EUR — Euro' },
                  ]} />
                </Form.Item>
              </Col>
              <Col xs={24} sm={14}>
                <Form.Item
                  name="interestPaymentMethod"
                  label="Phương thức trả lãi"
                  rules={[{ required: true, message: 'Chọn phương thức trả lãi' }]}
                  tooltip="Không thể thay đổi sau khi tạo."
                >
                  <Select
                    disabled={isEditMode}
                    options={PAYMENT_METHOD_OPTIONS.map((o) => ({
                      value: o.value,
                      label: o.label,
                    }))}
                  />
                </Form.Item>
              </Col>
            </Row>

            {/* ── Hạn mức gửi ──────────────────────────────── */}
            <Divider plain>Hạn mức gửi tiền</Divider>

            <Row gutter={16}>
              <Col xs={24} sm={12}>
                <Form.Item
                  name="minAmount"
                  label="Số tiền tối thiểu"
                  rules={[
                    { required: true, message: 'Nhập số tiền tối thiểu' },
                    { type: 'number', min: 1, message: 'Phải lớn hơn 0' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        const max = getFieldValue('maxAmount');
                        if (!value || !max || value < max) return Promise.resolve();
                        return Promise.reject(new Error('Phải nhỏ hơn số tiền tối đa'));
                      },
                    }),
                  ]}
                >
                  <InputNumber
                    style={{ width: '100%' }}
                    min={0}
                    step={1_000_000}
                    formatter={(v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                    parser={(v) => (Number(v?.replace(/\./g, '') ?? 0) as unknown) as 1}
                    addonAfter="₫"
                    placeholder="1.000.000"
                  />
                </Form.Item>
              </Col>
              <Col xs={24} sm={12}>
                <Form.Item
                  name="maxAmount"
                  label="Số tiền tối đa"
                  rules={[
                    { required: true, message: 'Nhập số tiền tối đa' },
                    { type: 'number', min: 1, message: 'Phải lớn hơn 0' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        const min = getFieldValue('minAmount');
                        if (!value || !min || value > min) return Promise.resolve();
                        return Promise.reject(new Error('Phải lớn hơn số tiền tối thiểu'));
                      },
                    }),
                  ]}
                >
                  <InputNumber
                    style={{ width: '100%' }}
                    min={0}
                    step={100_000_000}
                    formatter={(v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                    parser={(v) => (Number(v?.replace(/\./g, '') ?? 0) as unknown) as 1}
                    addonAfter="₫"
                    placeholder="10.000.000.000"
                  />
                </Form.Item>
              </Col>
            </Row>

            {/* Hiển thị khoảng hạn mức */}
            <Form.Item shouldUpdate noStyle>
              {({ getFieldValue }) => {
                const min = getFieldValue('minAmount');
                const max = getFieldValue('maxAmount');
                if (!min || !max || min >= max) return null;
                return (
                  <Alert
                    type="info"
                    showIcon
                    message={`Khách hàng có thể gửi từ ${formatVND(min)} đến ${formatVND(max)}`}
                    style={{ marginBottom: 16, borderRadius: 8 }}
                  />
                );
              }}
            </Form.Item>

            {/* ── Mô tả ────────────────────────────────────── */}
            <Divider plain>Mô tả (tuỳ chọn)</Divider>

            <Form.Item name="description" label="Mô tả sản phẩm">
              <Input.TextArea
                rows={3}
                maxLength={500}
                showCount
                placeholder="Mô tả ngắn về sản phẩm tiết kiệm này..."
              />
            </Form.Item>

            {/* ── Lưu ý khi tạo ────────────────────────────── */}
            {!isEditMode && (
              <Alert
                type="warning"
                showIcon
                message="Lưu ý sau khi tạo sản phẩm"
                description="Mã sản phẩm, tiền tệ và phương thức trả lãi không thể thay đổi sau khi tạo. Bạn có thể thêm kỳ hạn và lãi suất trong trang chi tiết."
                style={{ marginBottom: 16, borderRadius: 8 }}
              />
            )}

            {/* ── Actions ──────────────────────────────────── */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 8 }}>
              <Space>
                <Button onClick={() => navigate(backTo)} disabled={isPending}>
                  Huỷ
                </Button>
                <Button
                  type="primary"
                  icon={isEditMode ? <SaveOutlined /> : <PlusOutlined />}
                  loading={isPending}
                  onClick={handleSubmit}
                >
                  {isEditMode ? 'Lưu thay đổi' : 'Tạo sản phẩm'}
                </Button>
              </Space>
            </div>

          </Form>
        </Card>
      )}
    </div>
  );
}
