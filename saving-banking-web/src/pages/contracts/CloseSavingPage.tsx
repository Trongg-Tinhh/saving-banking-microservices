import { useParams, useNavigate } from 'react-router-dom';
import {
  Card,
  Typography,
  Button,
  Form,
  Select,
  Alert,
  Space,
  Descriptions,
  Skeleton,
  Statistic,
  Row,
  Col,
  Checkbox,
  Tag,
  Spin,
} from 'antd';
import {
  ArrowLeftOutlined,
  ExclamationCircleOutlined,
  WarningOutlined,
  BankOutlined,
} from '@ant-design/icons';
import { buildPath, ROUTES } from '@/constants/routes';
import { useContract, useCloseSaving } from '@/hooks/useContracts';
import { useAccountsByCif } from '@/hooks/useAccounts';
import { useAuthStore } from '@/stores/authStore';
import { formatVND } from '@/utils/formatCurrency';
import { formatDate, dayjs } from '@/utils/formatDate';
import { getStatusConfig } from '@/utils/contractStatus';
import type { CloseType } from '@/types';

const { Title, Text } = Typography;

interface CloseForm {
  receivingAccountNo: string;
  confirmedByUser: boolean;
}

export default function CloseSavingPage() {
  const { contractNo } = useParams<{ contractNo: string }>();
  const navigate = useNavigate();
  const [form] = Form.useForm<CloseForm>();

  const { hasAnyRole } = useAuthStore();
  const isStaff = hasAnyRole('ADMIN', 'TELLER', 'MANAGER');

  const { data: contract, isLoading } = useContract(contractNo);
  const closeMutation = useCloseSaving(contractNo);

  // CIF để lấy danh sách tài khoản nhận tiền:
  // - CUSTOMER: chỉ xem tài khoản của chính mình (= contract.cif, vì chỉ thấy hợp đồng của mình)
  // - Staff: xem tài khoản của khách hàng sở hữu hợp đồng (= contract.cif)
  const cifForAccounts = contract?.cif;

  const { data: accounts = [], isLoading: accountsLoading } = useAccountsByCif(cifForAccounts);

  // Determine if this is an early withdrawal
  const isEarly: boolean = contract
    ? dayjs(contract.maturityDate).isAfter(dayjs())
    : false;

  const closeType: CloseType = isEarly ? 'EARLY_WITHDRAWAL' : 'MATURITY';
  const statusConfig = contract ? getStatusConfig(contract.status) : null;

  const handleSubmit = async (values: CloseForm) => {
    await closeMutation.mutateAsync({
      receivingAccountNo: values.receivingAccountNo,
      closeType,
      confirmedByUser:    values.confirmedByUser,
    });
  };

  const accountOptions = accounts.map((a) => ({
    value: a.accountNo,
    label: (
      <Space>
        <BankOutlined />
        <Text code style={{ fontSize: 12 }}>{a.accountNo}</Text>
        <Text type="secondary">({a.currency})</Text>
      </Space>
    ),
  }));

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
        <Title level={4} style={{ margin: 0 }}>Tất toán hợp đồng</Title>
        <Text type="secondary">Số hợp đồng: <Text code>{contractNo}</Text></Text>
      </div>

      {/* Early withdrawal warning */}
      {isEarly && (
        <Alert
          type="warning"
          icon={<ExclamationCircleOutlined />}
          showIcon
          message={
            <Text strong>Tất toán trước hạn — Ngày đáo hạn: {contract ? formatDate(contract.maturityDate) : '—'}</Text>
          }
          description="Rút vốn trước hạn có thể bị phạt lãi suất theo chính sách sản phẩm. Lãi thực nhận sẽ thấp hơn lãi hợp đồng."
          style={{ marginBottom: 16, borderRadius: 8 }}
        />
      )}

      {!isEarly && contract && (
        <Alert
          type="success"
          showIcon
          message="Hợp đồng đến hạn — không bị phạt"
          style={{ marginBottom: 16, borderRadius: 8 }}
        />
      )}

      {/* Contract summary */}
      <Card style={{ borderRadius: 12, marginBottom: 16 }}>
        <Title level={5} style={{ marginTop: 0 }}>Thông tin hợp đồng</Title>
        {isLoading ? (
          <Skeleton active paragraph={{ rows: 3 }} />
        ) : contract ? (
          <>
            <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
              <Col xs={12} sm={8}>
                <Statistic
                  title="Số tiền gốc"
                  value={contract.principalAmount}
                  formatter={(v) => formatVND(Number(v))}
                  valueStyle={{ fontSize: 16 }}
                />
              </Col>
              <Col xs={12} sm={8}>
                <Statistic
                  title="Lãi suất / năm"
                  value={contract.interestRate.toFixed(2)}
                  suffix="%"
                  valueStyle={{ fontSize: 16 }}
                />
              </Col>
              {contract.expectedInterest !== undefined && (
                <Col xs={12} sm={8}>
                  <Statistic
                    title={isEarly ? 'Lãi dự kiến (nếu đúng hạn)' : 'Lãi dự kiến'}
                    value={contract.expectedInterest}
                    formatter={(v) => formatVND(Number(v))}
                    valueStyle={{ fontSize: 16, color: '#52c41a' }}
                  />
                </Col>
              )}
            </Row>

            <Descriptions bordered size="small" column={{ xs: 1, sm: 2 }}>
              <Descriptions.Item label="Khách hàng (CIF)">{contract.cif}</Descriptions.Item>
              <Descriptions.Item label="Sản phẩm">
                {contract.productName ?? contract.productCode}
              </Descriptions.Item>
              <Descriptions.Item label="Kỳ hạn">
                {contract.termLabel ?? contract.termId}
              </Descriptions.Item>
              <Descriptions.Item label="Ngày mở">
                {formatDate(contract.openDate)}
              </Descriptions.Item>
              <Descriptions.Item label="Ngày đáo hạn">
                <Space>
                  {formatDate(contract.maturityDate)}
                  {isEarly && <Tag color="orange" icon={<WarningOutlined />}>Chưa đến hạn</Tag>}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                {statusConfig && <Tag color={statusConfig.color}>{statusConfig.label}</Tag>}
              </Descriptions.Item>
            </Descriptions>
          </>
        ) : null}
      </Card>

      {/* Close form */}
      <Card style={{ borderRadius: 12 }}>
        <Title level={5} style={{ marginTop: 0 }}>Thông tin tất toán</Title>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          size="large"
          disabled={closeMutation.isPending || isLoading}
        >
          <Form.Item
            name="receivingAccountNo"
            label={
              <Space>
                Tài khoản nhận tiền
                {isStaff && (
                  <Tag color="blue" style={{ fontSize: 11 }}>
                    Tài khoản của CIF {contract?.cif}
                  </Tag>
                )}
              </Space>
            }
            rules={[{ required: true, message: 'Vui lòng chọn tài khoản nhận tiền' }]}
            extra="Gốc và lãi (sau khi trừ phạt nếu có) sẽ được chuyển về tài khoản này"
          >
            <Select
              placeholder={
                accountsLoading
                  ? 'Đang tải danh sách tài khoản...'
                  : accounts.length === 0
                  ? 'Không có tài khoản ACTIVE'
                  : 'Chọn tài khoản nhận tiền'
              }
              options={accountOptions}
              loading={accountsLoading}
              disabled={accountsLoading || !cifForAccounts}
              notFoundContent={
                accountsLoading ? <Spin size="small" /> : 'Không có tài khoản ACTIVE'
              }
              showSearch
              filterOption={(input, option) =>
                String(option?.value ?? '').toLowerCase().includes(input.toLowerCase())
              }
            />
          </Form.Item>

          <Form.Item
            name="confirmedByUser"
            valuePropName="checked"
            rules={[
              {
                validator: (_, value) =>
                  value ? Promise.resolve() : Promise.reject(new Error('Vui lòng xác nhận để tiếp tục')),
              },
            ]}
          >
            <Checkbox>
              <Text>
                Tôi xác nhận muốn{' '}
                <Text strong style={{ color: isEarly ? '#fa8c16' : '#52c41a' }}>
                  {isEarly ? 'tất toán trước hạn' : 'tất toán đúng hạn'}
                </Text>{' '}
                hợp đồng <Text code>{contractNo}</Text>
                {isEarly && ' và chấp nhận các điều khoản phạt lãi suất áp dụng'}
              </Text>
            </Checkbox>
          </Form.Item>

          {closeMutation.isError && (
            <Alert
              type="error"
              message="Không thể tất toán hợp đồng. Vui lòng thử lại hoặc liên hệ hỗ trợ."
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
              danger={isEarly}
              htmlType="submit"
              loading={closeMutation.isPending}
              disabled={accounts.length === 0 && !accountsLoading}
              icon={<ExclamationCircleOutlined />}
            >
              {isEarly ? 'Xác nhận tất toán trước hạn' : 'Xác nhận tất toán'}
            </Button>
          </Space>
        </Form>
      </Card>
    </div>
  );
}
