import { useParams, useNavigate } from 'react-router-dom';
import {
  Card,
  Typography,
  Button,
  Form,
  Input,
  Alert,
  Space,
  Descriptions,
  Skeleton,
  Select,
  Row,
  Col,
  Statistic,
  Tag,
} from 'antd';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import { buildPath, ROUTES } from '@/constants/routes';
import { useContract, useProcessMaturity } from '@/hooks/useContracts';
import { useProductTerms } from '@/hooks/useProducts';
import { formatVND } from '@/utils/formatCurrency';
import { formatDate } from '@/utils/formatDate';
import type { MaturityInstructionType } from '@/types';

const { Title, Text } = Typography;

// ─── Maturity instruction options ─────────────────────────────────

const INSTRUCTION_OPTIONS: {
  value: MaturityInstructionType;
  label: string;
  description: string;
}[] = [
  {
    value: 'RENEW_PRINCIPAL_AND_INTEREST',
    label: 'Tái tục gốc + lãi',
    description: 'Toàn bộ gốc và lãi được gộp thành khoản tiết kiệm mới',
  },
  {
    value: 'RENEW_PRINCIPAL',
    label: 'Tái tục gốc, rút lãi',
    description: 'Gốc tiếp tục gửi; lãi chuyển về tài khoản thanh toán',
  },
  {
    value: 'TRANSFER_PRINCIPAL_AND_INTEREST',
    label: 'Tất toán — chuyển về tài khoản',
    description: 'Toàn bộ gốc và lãi chuyển về tài khoản chỉ định',
  },
];

// ─── Form shape ───────────────────────────────────────────────────

interface MaturityForm {
  instructionType: MaturityInstructionType;
  newTermId?: string;
  receivingAccountNo?: string;
}

// ─── Page ─────────────────────────────────────────────────────────

export default function MaturityPage() {
  const { contractNo } = useParams<{ contractNo: string }>();
  const navigate = useNavigate();
  const [form] = Form.useForm<MaturityForm>();

  const { data: contract, isLoading } = useContract(contractNo);
  const { data: terms = [], isLoading: loadingTerms } = useProductTerms(contract?.productCode);
  const maturityMutation = useProcessMaturity(contractNo);

  const instructionType = Form.useWatch('instructionType', form);

  const handleSubmit = async (values: MaturityForm) => {
    await maturityMutation.mutateAsync({
      instructionType:    values.instructionType,
      newTermId:             values.newTermId ?? null,
      receivingAccountNo:    values.receivingAccountNo ?? null,
    });
  };

  return (
    <div style={{ maxWidth: 720, margin: '0 auto' }}>
      {/* Back */}
      <Button
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate(buildPath(ROUTES.CONTRACT_DETAIL, { contractNo: contractNo! }))}
        style={{ marginBottom: 16 }}
      >
        Chi tiết hợp đồng
      </Button>

      <div style={{ marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0 }}>Xử lý đáo hạn</Title>
        <Text type="secondary">Số hợp đồng: <Text code>{contractNo}</Text></Text>
      </div>

      <Alert
        type="info"
        showIcon
        message="Hợp đồng đến hạn"
        description="Chọn chỉ thị đáo hạn để tự động xử lý. Thao tác này không thể hoàn tác."
        style={{ marginBottom: 16, borderRadius: 8 }}
      />

      {/* Contract summary */}
      <Card style={{ borderRadius: 12, marginBottom: 16 }}>
        <Title level={5} style={{ marginTop: 0 }}>Thông tin hợp đồng</Title>
        {isLoading ? (
          <Skeleton active paragraph={{ rows: 3 }} />
        ) : contract ? (
          <>
            <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
              <Col xs={12} sm={6}>
                <Statistic
                  title="Số tiền gốc"
                  value={contract.principalAmount}
                  formatter={(v) => formatVND(Number(v))}
                  valueStyle={{ fontSize: 16 }}
                />
              </Col>
              <Col xs={12} sm={6}>
                <Statistic
                  title="Lãi suất / năm"
                  value={contract.interestRate.toFixed(2)}
                  suffix="%"
                  valueStyle={{ fontSize: 16 }}
                />
              </Col>
              {contract.expectedInterest !== undefined && (
                <Col xs={12} sm={6}>
                  <Statistic
                    title="Lãi dự kiến"
                    value={contract.expectedInterest}
                    formatter={(v) => formatVND(Number(v))}
                    valueStyle={{ fontSize: 16, color: '#52c41a' }}
                  />
                </Col>
              )}
              <Col xs={12} sm={6}>
                <Statistic
                  title="Ngày đáo hạn"
                  value={formatDate(contract.maturityDate)}
                  valueStyle={{ fontSize: 16 }}
                />
              </Col>
            </Row>

            <Descriptions bordered size="small" column={{ xs: 1, sm: 2 }}>
              <Descriptions.Item label="Sản phẩm">
                {contract.productName ?? contract.productCode}
              </Descriptions.Item>
              <Descriptions.Item label="Kỳ hạn hiện tại">
                {contract.termLabel ?? contract.termId}
                {contract.termDays && (
                  <Text type="secondary"> ({contract.termDays} ngày)</Text>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Tag color="blue">Đã đáo hạn</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Tài khoản nguồn">
                <Text code>{contract.sourceAccountNo}</Text>
              </Descriptions.Item>
            </Descriptions>
          </>
        ) : null}
      </Card>

      {/* Maturity instruction form */}
      <Card style={{ borderRadius: 12 }}>
        <Title level={5} style={{ marginTop: 0 }}>Chỉ thị đáo hạn</Title>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          size="large"
          disabled={maturityMutation.isPending || isLoading}
        >
          <Form.Item
            name="instructionType"
            label="Hướng xử lý"
            rules={[{ required: true, message: 'Chọn chỉ thị đáo hạn' }]}
          >
            <Select placeholder="Chọn hướng xử lý...">
              {INSTRUCTION_OPTIONS.map((opt) => (
                <Select.Option key={opt.value} value={opt.value}>
                  <div>
                    <Text strong>{opt.label}</Text>
                    <br />
                    <Text type="secondary" style={{ fontSize: 12 }}>{opt.description}</Text>
                  </div>
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          {/* New term selection (for renewal types) */}
          {(instructionType === 'RENEW_PRINCIPAL' ||
            instructionType === 'RENEW_PRINCIPAL_AND_INTEREST') && (
            <Form.Item
              name="newTermId"
              label="Kỳ hạn tái tục (bỏ trống = giữ nguyên)"
            >
              <Select
                allowClear
                placeholder="Giữ nguyên kỳ hạn hiện tại"
                loading={loadingTerms}
                options={terms
                  .filter((t) => t.isActive)
                  .map((t) => ({
                    value: t.termId,
                    label: `${t.termLabel}${t.annualRate != null ? ` — ${t.annualRate.toFixed(2)}%/năm` : ''}`,
                  }))}
              />
            </Form.Item>
          )}

          {/* Receiving account (for transfer type) */}
          {instructionType === 'TRANSFER_PRINCIPAL_AND_INTEREST' && (
            <Form.Item
              name="receivingAccountNo"
              label="Tài khoản nhận tiền"
              rules={[{ required: true, message: 'Nhập số tài khoản nhận tiền' }]}
              extra="Gốc và lãi sẽ được chuyển về tài khoản này"
              initialValue={contract?.sourceAccountNo}
            >
              <Input placeholder="Nhập số tài khoản..." />
            </Form.Item>
          )}

          {maturityMutation.isError && (
            <Alert
              type="error"
              message="Không thể xử lý đáo hạn. Vui lòng thử lại."
              showIcon
              style={{ marginBottom: 16, borderRadius: 8 }}
            />
          )}

          <Space>
            <Button
              onClick={() => navigate(buildPath(ROUTES.CONTRACT_DETAIL, { contractNo: contractNo! }))}
            >
              Huỷ
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={maturityMutation.isPending}
              icon={<CheckCircleOutlined />}
            >
              Xác nhận đáo hạn
            </Button>
          </Space>
        </Form>
      </Card>
    </div>
  );
}
