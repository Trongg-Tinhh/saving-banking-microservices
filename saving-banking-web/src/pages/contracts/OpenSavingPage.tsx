import { useState, useEffect } from 'react';
import {
  Typography,
  Card,
  Button,
  Steps,
  Form,
  Input,
  InputNumber,
  Select,
  DatePicker,
  Space,
  Descriptions,
  Alert,
  Row,
  Col,
  Divider,
  Spin,
  Tooltip,
  Tag,
} from 'antd';
import {
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CheckOutlined,
  SaveOutlined,
  UserOutlined,
  BankOutlined,
  FileTextOutlined,
  LockOutlined,
  PlusCircleOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { dayjs } from '@/utils/formatDate';
import type { Dayjs } from 'dayjs';
import { ROUTES } from '@/constants/routes';
import { useProducts, useProductTerms } from '@/hooks/useProducts';
import { useAccountsByCif, useAccountBalance } from '@/hooks/useAccounts';
import { useOpenSaving } from '@/hooks/useContracts';
import { getApiErrorMessage } from '@/services/api';
import { useAuthStore } from '@/stores/authStore';
import { formatVND } from '@/utils/formatCurrency';
import { formatDate } from '@/utils/formatDate';
import type { MaturityInstructionType } from '@/types';

const { Title, Text } = Typography;

// ─── Maturity instruction labels ──────────────────────────────────

const MATURITY_OPTIONS: { value: MaturityInstructionType; label: string; description: string }[] = [
  {
    value: 'RENEW_PRINCIPAL_AND_INTEREST',
    label: 'Tái tục gốc + lãi',
    description: 'Toàn bộ gốc và lãi được tái tục thành khoản tiết kiệm mới',
  },
  {
    value: 'RENEW_PRINCIPAL',
    label: 'Tái tục gốc, rút lãi',
    description: 'Gốc được tái tục, lãi chuyển về tài khoản nguồn',
  },
  {
    value: 'TRANSFER_PRINCIPAL_AND_INTEREST',
    label: 'Tất toán — chuyển về tài khoản',
    description: 'Toàn bộ gốc và lãi chuyển về tài khoản chỉ định',
  },
];

// ─── Form shapes ──────────────────────────────────────────────────

interface Step1Form {
  cif: string;
  productCode: string;
  termId: string;
}

interface Step2Form {
  principalAmount: number;
  sourceAccountNo: string;
  openDate: Dayjs;
  instructionType: MaturityInstructionType;
  newTermId?: string;
  receivingAccountNo?: string;
}

// ─── Page ─────────────────────────────────────────────────────────

export default function OpenSavingPage() {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(0);
  const [step1Form] = Form.useForm<Step1Form>();
  const [step2Form] = Form.useForm<Step2Form>();

  // Persisted values — capture on each "Next" press so step 3 always has valid data
  const [step1Values, setStep1Values] = useState<Step1Form | null>(null);
  const [step2Values, setStep2Values] = useState<Step2Form | null>(null);

  // Auth
  const { user, isCustomer, hasAnyRole } = useAuthStore();
  const isStaff = hasAnyRole('ADMIN', 'TELLER', 'MANAGER');

  // Auto-fill CIF for CUSTOMER role
  useEffect(() => {
    if (isCustomer() && user?.cif) {
      step1Form.setFieldValue('cif', user.cif);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.cif]);

  // Watched values (only meaningful when the respective form is mounted)
  const cifValue         = Form.useWatch('cif',             step1Form);
  const productCodeValue = Form.useWatch('productCode',     step1Form);
  const instructionType  = Form.useWatch('instructionType', step2Form);
  const sourceAccountNo  = Form.useWatch('sourceAccountNo', step2Form);
  const principalAmount  = Form.useWatch('principalAmount', step2Form);

  // BUG FIX: Form.useWatch returns undefined when the form is unmounted (step hidden).
  // Use persisted step1Values as fallback so Step 2 & 3 queries stay populated.
  const activeCif         = step1Values?.cif         ?? cifValue;
  const activeProductCode = step1Values?.productCode ?? productCodeValue;

  // Balance: use persisted account no (step 3) or watched value (step 2)
  const activeSourceAccountNo = step2Values?.sourceAccountNo ?? sourceAccountNo;

  // Queries
  const { data: products = [],  isLoading: loadingProducts } = useProducts(true);
  const { data: terms    = [],  isLoading: loadingTerms    } = useProductTerms(activeProductCode);
  const { data: accounts = [],  isLoading: loadingAccounts } = useAccountsByCif(activeCif);
  const { data: balance,        isLoading: loadingBalance  } = useAccountBalance(activeSourceAccountNo);

  // Mutation
  const openSaving = useOpenSaving();

  const selectedProduct = products.find((p) => p.productCode === productCodeValue);
  const selectedTerm    = terms.find((t) => t.termId === step1Values?.termId);

  // Balance check helpers
  const activePrincipal     = step2Values?.principalAmount ?? principalAmount ?? 0;
  const availableBalance    = balance?.availableBalance ?? 0;
  const isBalanceSufficient = !activeSourceAccountNo || !balance || availableBalance >= activePrincipal;
  const balanceShortfall    = !isBalanceSufficient ? activePrincipal - availableBalance : 0;

  // ─── Navigation ────────────────────────────────────────────────

  const nextStep = async () => {
    if (currentStep === 0) {
      try {
        const values = await step1Form.validateFields();
        setStep1Values(values);
        step2Form.resetFields(['sourceAccountNo']);
        setCurrentStep(1);
      } catch { /* validation error shown inline */ }

    } else if (currentStep === 1) {
      try {
        const values = await step2Form.validateFields();
        // Persist step 2 values BEFORE unmounting the form
        setStep2Values(values);
        setCurrentStep(2);
      } catch { /* validation error shown inline */ }
    }
  };

  const prevStep = () => {
    if (currentStep === 2) {
      // Restore step2 form values before remounting it
      if (step2Values) {
        setTimeout(() => step2Form.setFieldsValue(step2Values), 0);
      }
    }
    setCurrentStep((s) => s - 1);
  };

  // ─── Submit ────────────────────────────────────────────────────

  const handleSubmit = async () => {
    // Guard: step2Values is always set when reaching step 3
    if (!step1Values || !step2Values) return;

    // openDate is always a valid Dayjs since it was validated before setting step2Values
    const openDateStr = step2Values.openDate.format('YYYY-MM-DD');
    const product     = products.find((p) => p.productCode === step1Values.productCode);

    await openSaving.mutateAsync({
      cif:             step1Values.cif,
      productCode:     step1Values.productCode,
      termId:          step1Values.termId,
      principalAmount: step2Values.principalAmount,
      currency:        product?.currency ?? 'VND',
      sourceAccountNo: step2Values.sourceAccountNo,
      openDate:        openDateStr,
      maturityInstruction: {
        instructionType:   step2Values.instructionType,
        newTermId:         step2Values.newTermId         ?? null,
        receivingAccountNo: step2Values.receivingAccountNo ?? null,
      },
    });
  };

  // ─── Step 1: Customer & Product ───────────────────────────────

  const renderStep1 = () => (
    <Form form={step1Form} layout="vertical" size="large">
      <Divider plain style={{ marginTop: 0 }}>
        <Space><UserOutlined /> Khách hàng</Space>
      </Divider>

      <Form.Item
        name="cif"
        label={
          <Space size={4}>
            Mã khách hàng (CIF)
            {isCustomer() && (
              <Tooltip title="CIF được gán tự động theo tài khoản của bạn">
                <LockOutlined style={{ color: '#8c8c8c', fontSize: 12 }} />
              </Tooltip>
            )}
          </Space>
        }
        rules={[
          { required: true, message: 'Nhập CIF khách hàng' },
          { min: 3, message: 'CIF phải có ít nhất 3 ký tự' },
        ]}
      >
        <Input
          placeholder={isCustomer() ? '' : 'Nhập CIF khách hàng...'}
          prefix={<UserOutlined />}
          disabled={isCustomer()}
          autoFocus={!isCustomer()}
        />
      </Form.Item>

      {cifValue && cifValue.length >= 3 && (
        <Alert
          type={loadingAccounts ? 'info' : accounts.length > 0 ? 'success' : 'warning'}
          showIcon
          message={
            loadingAccounts
              ? 'Đang kiểm tra tài khoản...'
              : accounts.length > 0
              ? `Tìm thấy ${accounts.length} tài khoản hoạt động`
              : 'Không tìm thấy tài khoản hoạt động cho CIF này'
          }
          description={
            !loadingAccounts && accounts.length === 0 && isStaff
              ? (
                <Button
                  type="link"
                  size="small"
                  icon={<PlusCircleOutlined />}
                  style={{ padding: '4px 0' }}
                  onClick={() => navigate(`${ROUTES.ACCOUNTS_CREATE}?cif=${cifValue}`)}
                >
                  Tạo tài khoản cho CIF này
                </Button>
              )
              : undefined
          }
          icon={loadingAccounts ? <Spin size="small" /> : undefined}
          style={{ marginBottom: 16, borderRadius: 8 }}
        />
      )}

      <Divider plain>
        <Space><BankOutlined /> Sản phẩm & Kỳ hạn</Space>
      </Divider>

      <Row gutter={16}>
        <Col span={24}>
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
              onChange={() => step1Form.setFieldValue('termId', undefined)}
            />
          </Form.Item>
        </Col>
        <Col span={24}>
          <Form.Item
            name="termId"
            label="Kỳ hạn"
            rules={[{ required: true, message: 'Chọn kỳ hạn' }]}
          >
            <Select
              placeholder={productCodeValue ? 'Chọn kỳ hạn...' : 'Chọn sản phẩm trước'}
              disabled={!productCodeValue}
              loading={loadingTerms}
              options={terms
                .filter((t) => t.isActive)
                .map((t) => ({
                  value: t.termId,
                  label: `${t.termLabel}${t.annualRate != null ? ` — ${t.annualRate.toFixed(2)}%/năm` : ''}`,
                }))}
            />
          </Form.Item>
        </Col>
      </Row>
    </Form>
  );

  // ─── Step 2: Contract Details ─────────────────────────────────

  const renderStep2 = () => (
    <Form
      form={step2Form}
      layout="vertical"
      size="large"
      initialValues={{ openDate: dayjs() }}
    >
      <Row gutter={16}>
        <Col xs={24} md={12}>
          <Form.Item
            name="principalAmount"
            label="Số tiền gửi (VND)"
            rules={[
              { required: true, message: 'Nhập số tiền gửi' },
              {
                type: 'number',
                min: selectedProduct?.minAmount ?? 1_000_000,
                message: `Tối thiểu ${formatVND(selectedProduct?.minAmount ?? 1_000_000)}`,
              },
              {
                type: 'number',
                max: selectedProduct?.maxAmount ?? 999_999_999_999,
                message: `Tối đa ${formatVND(selectedProduct?.maxAmount ?? 999_999_999_999)}`,
              },
              // Balance check validator
              {
                validator: (_, value) => {
                  if (!value || !balance) return Promise.resolve();
                  if (value > balance.availableBalance) {
                    return Promise.reject(
                      new Error(
                        `Số dư khả dụng chỉ còn ${formatVND(balance.availableBalance)} — không đủ để gửi`
                      )
                    );
                  }
                  return Promise.resolve();
                },
              },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              min={selectedProduct?.minAmount ?? 1_000_000}
              step={1_000_000}
              formatter={(v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
              parser={(v) => (Number(v?.replace(/\./g, '') ?? 0) as unknown) as 1000000}
              addonAfter="₫"
              placeholder="50.000.000"
            />
          </Form.Item>
        </Col>
        <Col xs={24} md={12}>
          <Form.Item
            name="openDate"
            label="Ngày bắt đầu gửi"
            rules={[{ required: true, message: 'Chọn ngày bắt đầu' }]}
          >
            <DatePicker
              style={{ width: '100%' }}
              format="DD/MM/YYYY"
              disabledDate={(d) => d.isBefore(dayjs().subtract(1, 'day'))}
            />
          </Form.Item>
        </Col>
      </Row>

      {/* Source account selector */}
      <Form.Item
        name="sourceAccountNo"
        label="Tài khoản nguồn (trích nợ)"
        rules={[{ required: true, message: 'Chọn tài khoản nguồn' }]}
      >
        <Select
          placeholder="Chọn tài khoản..."
          loading={loadingAccounts}
          options={accounts.map((a) => ({
            value: a.accountNo,
            label: a.label,
          }))}
          notFoundContent={loadingAccounts ? <Spin size="small" /> : 'Không tìm thấy tài khoản'}
        />
      </Form.Item>

      {/* Balance info bar — shown when an account is selected */}
      {sourceAccountNo && (
        <div
          style={{
            marginTop: -12,
            marginBottom: 16,
            padding: '8px 12px',
            background: '#fafafa',
            border: '1px solid #f0f0f0',
            borderRadius: 8,
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            flexWrap: 'wrap',
          }}
        >
          <Space size={4}>
            <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
            <Text type="secondary" style={{ fontSize: 13 }}>Số dư khả dụng:</Text>
          </Space>

          {loadingBalance ? (
            <Spin size="small" />
          ) : balance ? (
            <>
              <Text
                strong
                style={{
                  fontSize: 14,
                  color: balance.availableBalance >= (principalAmount ?? 0) ? '#52c41a' : '#ff4d4f',
                }}
              >
                {formatVND(balance.availableBalance)}
              </Text>

              {(principalAmount ?? 0) > 0 && balance.availableBalance < (principalAmount ?? 0) && (
                <Tag color="error" icon={<ExclamationCircleOutlined />}>
                  Thiếu {formatVND((principalAmount ?? 0) - balance.availableBalance)}
                </Tag>
              )}

              {(principalAmount ?? 0) > 0 && balance.availableBalance >= (principalAmount ?? 0) && (
                <Tag color="success">
                  Còn lại: {formatVND(balance.availableBalance - (principalAmount ?? 0))}
                </Tag>
              )}
            </>
          ) : (
            <Text type="secondary" style={{ fontSize: 13 }}>Đang tải...</Text>
          )}
        </div>
      )}

      <Divider plain>Chỉ thị đáo hạn</Divider>

      <Form.Item
        name="instructionType"
        label="Hướng xử lý khi đáo hạn"
        rules={[{ required: true, message: 'Chọn chỉ thị đáo hạn' }]}
      >
        <Select
          placeholder="Chọn chỉ thị..."
          options={MATURITY_OPTIONS.map((opt) => ({
            value: opt.value,
            label: opt.label,
          }))}
        />
      </Form.Item>

      {/* Description of selected maturity instruction — shown below the dropdown */}
      {instructionType && (() => {
        const selected = MATURITY_OPTIONS.find((o) => o.value === instructionType);
        return selected ? (
          <div
            style={{
              marginTop: -12,
              marginBottom: 16,
              padding: '8px 12px',
              background: '#f6ffed',
              border: '1px solid #b7eb8f',
              borderRadius: 6,
            }}
          >
            <Text type="secondary" style={{ fontSize: 13 }}>
              💡 {selected.description}
            </Text>
          </div>
        ) : null;
      })()}

      {/* Renewal term (for RENEW_* types) */}
      {(instructionType === 'RENEW_PRINCIPAL' || instructionType === 'RENEW_PRINCIPAL_AND_INTEREST') && (
        <Form.Item
          name="newTermId"
          label="Kỳ hạn tái tục (bỏ trống = giữ nguyên kỳ hạn)"
        >
          <Select
            allowClear
            placeholder="Giữ nguyên kỳ hạn hiện tại"
            options={terms
              .filter((t) => t.isActive)
              .map((t) => ({
                value: t.termId,
                label: `${t.termLabel} — ${t.annualRate.toFixed(2)}%/năm`,
              }))}
          />
        </Form.Item>
      )}

      {/* Receiving account (for TRANSFER type) */}
      {instructionType === 'TRANSFER_PRINCIPAL_AND_INTEREST' && (
        <Form.Item
          name="receivingAccountNo"
          label="Tài khoản nhận tiền khi đáo hạn"
          rules={[{ required: true, message: 'Nhập số tài khoản nhận tiền' }]}
        >
          <Input placeholder="Nhập số tài khoản..." />
        </Form.Item>
      )}
    </Form>
  );

  // ─── Step 3: Review ───────────────────────────────────────────

  const renderReview = () => {
    // Always use persisted step2Values — step2Form is unmounted at this point
    if (!step2Values) return null;

    const instruction = MATURITY_OPTIONS.find((o) => o.value === step2Values.instructionType);
    const account     = accounts.find((a) => a.accountNo === step2Values.sourceAccountNo);
    const openDateStr = step2Values.openDate.format('YYYY-MM-DD');

    return (
      <div>
        <Alert
          type="info"
          showIcon
          message="Vui lòng kiểm tra kỹ thông tin trước khi xác nhận"
          style={{ marginBottom: 20, borderRadius: 8 }}
        />

        <Descriptions
          title="Thông tin hợp đồng"
          bordered
          column={{ xs: 1, sm: 2 }}
          size="small"
          style={{ marginBottom: 20 }}
        >
          <Descriptions.Item label="CIF khách hàng">
            <Text strong>{step1Values?.cif}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Sản phẩm">
            {selectedProduct?.productName ?? step1Values?.productCode}
          </Descriptions.Item>
          <Descriptions.Item label="Kỳ hạn">
            {selectedTerm?.termLabel ?? step1Values?.termId}
          </Descriptions.Item>
          <Descriptions.Item label="Lãi suất / năm">
            <Text strong style={{ color: '#1677ff' }}>
              {selectedTerm?.annualRate?.toFixed(2) ?? '—'}%
            </Text>
          </Descriptions.Item>
          <Descriptions.Item label="Số tiền gửi">
            <Text strong style={{ fontSize: 16 }}>
              {formatVND(step2Values.principalAmount)}
            </Text>
          </Descriptions.Item>
          <Descriptions.Item label="Ngày bắt đầu">
            {formatDate(openDateStr)}
          </Descriptions.Item>
          <Descriptions.Item label="Tài khoản nguồn" span={2}>
            <Space wrap>
              <Text>{account?.label ?? step2Values.sourceAccountNo}</Text>

              {/* Balance panel */}
              {loadingBalance ? (
                <Spin size="small" />
              ) : balance ? (
                <Space size={8}>
                  <Space size={4}>
                    <Text type="secondary" style={{ fontSize: 12 }}>Số dư:</Text>
                    <Text
                      strong
                      style={{
                        fontSize: 13,
                        color: isBalanceSufficient ? '#52c41a' : '#ff4d4f',
                      }}
                    >
                      {formatVND(availableBalance)}
                    </Text>
                  </Space>

                  {isBalanceSufficient ? (
                    <Tag color="success">
                      Còn lại sau gửi: {formatVND(availableBalance - activePrincipal)}
                    </Tag>
                  ) : (
                    <Tag color="error" icon={<ExclamationCircleOutlined />}>
                      Thiếu {formatVND(balanceShortfall)}
                    </Tag>
                  )}
                </Space>
              ) : null}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="Chỉ thị đáo hạn" span={2}>
            {instruction?.label ?? '—'}
          </Descriptions.Item>
          {step2Values.receivingAccountNo && (
            <Descriptions.Item label="TK nhận tiền" span={2}>
              {step2Values.receivingAccountNo}
            </Descriptions.Item>
          )}
        </Descriptions>

        {/* Insufficient balance warning */}
        {!isBalanceSufficient && (
          <Alert
            type="error"
            showIcon
            icon={<ExclamationCircleOutlined />}
            message="Số dư tài khoản không đủ"
            description={
              `Tài khoản nguồn chỉ còn ${formatVND(availableBalance)}, ` +
              `cần thêm ${formatVND(balanceShortfall)} để mở sổ.`
            }
            style={{ marginBottom: 16, borderRadius: 8 }}
          />
        )}

        {openSaving.isError && (
          <Alert
            type="error"
            showIcon
            message="Mở sổ thất bại"
            description={getApiErrorMessage(openSaving.error, 'Vui lòng kiểm tra lại thông tin.')}
            style={{ borderRadius: 8 }}
          />
        )}
      </div>
    );
  };

  // ─── Render ───────────────────────────────────────────────────

  const stepItems = [
    { title: 'Khách hàng & Sản phẩm', icon: <UserOutlined /> },
    { title: 'Chi tiết hợp đồng',     icon: <BankOutlined /> },
    { title: 'Xác nhận',              icon: <FileTextOutlined /> },
  ];

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(ROUTES.CONTRACTS)}
          style={{ marginBottom: 16 }}
        >
          Danh sách hợp đồng
        </Button>
        <Title level={4} style={{ margin: 0 }}>Mở sổ tiết kiệm mới</Title>
        <Text type="secondary">
          {isCustomer()
            ? 'Mở sổ tiết kiệm cho tài khoản của bạn'
            : 'Tạo hợp đồng tiết kiệm cho khách hàng'}
        </Text>
      </div>

      {/* Steps bar */}
      <Card style={{ borderRadius: 12, marginBottom: 20 }}>
        <Steps current={currentStep} items={stepItems} size="small" />
      </Card>

      {/* Step content */}
      <Card style={{ borderRadius: 12, marginBottom: 20 }}>
        {currentStep === 0 && renderStep1()}
        {currentStep === 1 && renderStep2()}
        {currentStep === 2 && renderReview()}
      </Card>

      {/* Navigation buttons */}
      <Card style={{ borderRadius: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={currentStep === 0 ? () => navigate(ROUTES.CONTRACTS) : prevStep}
          >
            {currentStep === 0 ? 'Huỷ' : 'Quay lại'}
          </Button>

          {currentStep < 2 ? (
            <Button
              type="primary"
              icon={<ArrowRightOutlined />}
              onClick={nextStep}
            >
              Tiếp theo
            </Button>
          ) : (
            <Button
              type="primary"
              icon={openSaving.isSuccess ? <CheckOutlined /> : <SaveOutlined />}
              loading={openSaving.isPending}
              onClick={handleSubmit}
              disabled={openSaving.isPending || !isBalanceSufficient}
              danger={!isBalanceSufficient}
            >
              {isBalanceSufficient ? 'Xác nhận mở sổ' : 'Số dư không đủ'}
            </Button>
          )}
        </div>
      </Card>
    </div>
  );
}
