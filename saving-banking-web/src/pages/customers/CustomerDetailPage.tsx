import { useState } from 'react';
import {
  Typography,
  Card,
  Button,
  Form,
  Input,
  Select,
  DatePicker,
  Tabs,
  Descriptions,
  Tag,
  Alert,
  Space,
  Spin,
  Table,
  Row,
  Col,
  Divider,
  Tooltip,
  Modal,
  message,
} from 'antd';
import {
  ArrowLeftOutlined,
  EditOutlined,
  SaveOutlined,
  CloseOutlined,
  UserOutlined,
  PhoneOutlined,
  MailOutlined,
  EnvironmentOutlined,
  BankOutlined,
  PlusCircleOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import type { Dayjs } from 'dayjs';
import { dayjs } from '@/utils/formatDate';
import { ROUTES, buildPath } from '@/constants/routes';
import { useAuthStore } from '@/stores/authStore';
import {
  useCustomer,
  useCustomerContacts,
  useUpdateCustomer,
  useUpdateContact,
  useAddContact,
} from '@/hooks/useCustomers';
import { useAccountsByCif } from '@/hooks/useAccounts';
import type {
  UpdateCustomerRequest,
  UpdateContactRequest,
  CreateContactRequest,
  Gender,
  CustomerStatus,
  CustomerContact,
} from '@/types';

const { Title, Text } = Typography;

// ── Helpers ───────────────────────────────────────────────────────

const KYC_STATUS_CONFIG = {
  VERIFIED:     { color: 'success', icon: <CheckCircleOutlined />,       label: 'Đã xác minh' },
  PENDING:      { color: 'warning', icon: <ClockCircleOutlined />,       label: 'Chờ xác minh' },
  REJECTED:     { color: 'error',   icon: <ExclamationCircleOutlined />, label: 'Từ chối' },
  NOT_VERIFIED: { color: 'default', icon: <ClockCircleOutlined />,       label: 'Chưa xác minh' },
} as const;

const CUST_STATUS_COLOR: Record<string, string> = {
  ACTIVE: 'success', INACTIVE: 'default', BLOCKED: 'error',
};
const GENDER_LABELS: Record<string, string> = {
  MALE: 'Nam', FEMALE: 'Nữ', OTHER: 'Khác',
};

// ── Form shapes ───────────────────────────────────────────────────

interface InfoEditForm {
  fullName:    string;
  dateOfBirth: Dayjs | null;
  gender:      Gender;
  nationality: string;
  status?:     CustomerStatus;   // only shown for staff
}

interface ContactEditForm {
  phoneNumber: string;
  email:       string;
  address:     string;
  district:    string;
  city:        string;
}

// ── Page ──────────────────────────────────────────────────────────

export default function CustomerDetailPage() {
  const { cif } = useParams<{ cif: string }>();
  const navigate = useNavigate();
  const { user, isCustomer, hasAnyRole } = useAuthStore();

  const isStaff      = hasAnyRole('ADMIN', 'TELLER', 'MANAGER');
  const isOwnProfile = isCustomer() && cif === user?.cif;
  const canEdit      = isStaff || isOwnProfile;

  // ── State ─────────────────────────────────────────────────────
  const [editingInfo,    setEditingInfo]    = useState(false);
  const [editingContact, setEditingContact] = useState<string | null>(null); // contactId being edited
  const [showAddContact, setShowAddContact] = useState(false);
  const [infoForm]    = Form.useForm<InfoEditForm>();
  const [contactForm] = Form.useForm<ContactEditForm>();
  const [addForm]     = Form.useForm<ContactEditForm & { isPrimary: boolean }>();

  // ── Data ──────────────────────────────────────────────────────
  const { data: customer, isLoading: loadingCustomer, isError } = useCustomer(cif);
  const { data: contacts = [], isLoading: loadingContacts      } = useCustomerContacts(cif);
  const { data: accounts = [], isLoading: loadingAccounts      } = useAccountsByCif(cif);
  const updateCustomer = useUpdateCustomer(cif!);
  const updateContact  = useUpdateContact(cif!);
  const addContact     = useAddContact(cif!);

  // ── Info handlers ─────────────────────────────────────────────

  const startEditInfo = () => {
    if (!customer) return;
    infoForm.setFieldsValue({
      fullName:    customer.fullName,
      dateOfBirth: customer.dateOfBirth ? dayjs(customer.dateOfBirth) : null,
      gender:      customer.gender as Gender,
      nationality: customer.nationality ?? '',
      status:      customer.status as CustomerStatus,
    });
    setEditingInfo(true);
  };

  const handleSaveInfo = async (values: InfoEditForm) => {
    const req: UpdateCustomerRequest = {
      fullName:    values.fullName?.trim() || undefined,
      dateOfBirth: values.dateOfBirth?.format('YYYY-MM-DD'),
      gender:      values.gender,
      nationality: values.nationality?.trim() || undefined,
      // CUSTOMER cannot change own status — only staff
      status:      isStaff ? values.status : undefined,
    };
    try {
      await updateCustomer.mutateAsync(req);
      setEditingInfo(false);
      message.success('Cập nhật thông tin thành công');
    } catch {
      // error shown via Alert
    }
  };

  // ── Contact handlers ──────────────────────────────────────────

  const startEditContact = (c: CustomerContact) => {
    contactForm.setFieldsValue({
      phoneNumber: c.phoneNumber ?? '',
      email:       c.email       ?? '',
      address:     c.address     ?? '',
      district:    c.district    ?? '',
      city:        c.city        ?? '',
    });
    setEditingContact(c.contactId);
  };

  const handleSaveContact = async (values: ContactEditForm) => {
    if (!editingContact) return;
    const req: UpdateContactRequest = {
      phoneNumber: values.phoneNumber?.trim() || undefined,
      email:       values.email?.trim()       || undefined,
      address:     values.address?.trim()     || undefined,
      district:    values.district?.trim()    || undefined,
      city:        values.city?.trim()        || undefined,
    };
    try {
      await updateContact.mutateAsync({ contactId: editingContact, req });
      setEditingContact(null);
      message.success('Cập nhật liên hệ thành công');
    } catch {
      // error shown via Alert
    }
  };

  const handleAddContact = async (values: ContactEditForm & { isPrimary: boolean }) => {
    const req: CreateContactRequest = {
      phoneNumber: values.phoneNumber?.trim() || undefined,
      email:       values.email?.trim()       || undefined,
      address:     values.address?.trim()     || undefined,
      district:    values.district?.trim()    || undefined,
      city:        values.city?.trim()        || undefined,
      isPrimary:   values.isPrimary ?? false,
    };
    try {
      await addContact.mutateAsync(req);
      setShowAddContact(false);
      addForm.resetFields();
      message.success('Thêm liên hệ thành công');
    } catch {
      // error via Alert
    }
  };

  // ── Loading / Error ───────────────────────────────────────────

  if (loadingCustomer) {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Spin size="large" />
        <br />
        <Text type="secondary" style={{ marginTop: 12, display: 'block' }}>
          Đang tải thông tin...
        </Text>
      </div>
    );
  }

  if (isError || !customer) {
    return (
      <div style={{ maxWidth: 600, margin: '40px auto' }}>
        <Alert
          type="error" showIcon
          message="Không tìm thấy khách hàng"
          description={`CIF "${cif}" không tồn tại trong hệ thống.`}
          action={<Button onClick={() => navigate(-1)}>Quay lại</Button>}
        />
      </div>
    );
  }

  const kycCfg = customer.kycStatus
    ? (KYC_STATUS_CONFIG as Record<string, typeof KYC_STATUS_CONFIG['VERIFIED']>)[customer.kycStatus]
    : null;

  // ── Contact form fields (reusable) ────────────────────────────

  const renderContactFields = (size: 'middle' | 'large' = 'large') => (
    <Row gutter={12}>
      <Col xs={24} sm={12}>
        <Form.Item name="phoneNumber" label={<><PhoneOutlined /> Số điện thoại</>}
          rules={[{ pattern: /^(0|\+84)[0-9]{8,10}$/, message: 'Số điện thoại không hợp lệ' }]}>
          <Input placeholder="VD: 0901234567" />
        </Form.Item>
      </Col>
      <Col xs={24} sm={12}>
        <Form.Item name="email" label={<><MailOutlined /> Email</>}
          rules={[{ type: 'email', message: 'Email không hợp lệ' }]}>
          <Input placeholder="VD: name@email.com" />
        </Form.Item>
      </Col>
      <Col xs={24}>
        <Form.Item name="address" label={<><EnvironmentOutlined /> Địa chỉ</>}>
          <Input placeholder="Số nhà, tên đường..." />
        </Form.Item>
      </Col>
      <Col xs={24} sm={12}>
        <Form.Item name="district" label="Quận / Huyện">
          <Input placeholder="VD: Quận 1" />
        </Form.Item>
      </Col>
      <Col xs={24} sm={12}>
        <Form.Item name="city" label="Tỉnh / Thành phố">
          <Input placeholder="VD: Hồ Chí Minh" />
        </Form.Item>
      </Col>
    </Row>
  );

  // ── Tab: Thông tin KH ─────────────────────────────────────────

  const renderInfoTab = () => (
    <div>
      {/* View mode */}
      {!editingInfo && (
        <>
          {canEdit && (
            <div style={{ textAlign: 'right', marginBottom: 16 }}>
              <Button icon={<EditOutlined />} onClick={startEditInfo}>
                Chỉnh sửa thông tin
              </Button>
            </div>
          )}
          <Descriptions bordered column={{ xs: 1, sm: 2 }} size="small">
            <Descriptions.Item label="CIF">
              <Text strong copyable>{customer.cif}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <Tag color={CUST_STATUS_COLOR[customer.status] ?? 'default'}>
                {customer.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Họ tên" span={2}>
              <Text strong style={{ fontSize: 15 }}>{customer.fullName}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Ngày sinh">
              {customer.dateOfBirth ? dayjs(customer.dateOfBirth).format('DD/MM/YYYY') : '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Giới tính">
              {customer.gender ? GENDER_LABELS[customer.gender] ?? customer.gender : '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Quốc tịch">
              {customer.nationality ?? '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Loại giấy tờ">
              {customer.idType ?? '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Số giấy tờ" span={2}>
              <Text copyable={!!customer.idNumber}>{customer.idNumber ?? '—'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái KYC">
              {kycCfg ? (
                <Tag icon={kycCfg.icon} color={kycCfg.color}>{kycCfg.label}</Tag>
              ) : '—'}
            </Descriptions.Item>
            <Descriptions.Item label="Ngày tạo">
              {customer.createdAt
                ? dayjs(customer.createdAt).format('DD/MM/YYYY HH:mm')
                : '—'}
            </Descriptions.Item>
          </Descriptions>
        </>
      )}

      {/* Edit mode */}
      {editingInfo && (
        <Form form={infoForm} layout="vertical" size="large" onFinish={handleSaveInfo}>
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item name="fullName" label="Họ và tên"
                rules={[{ required: true, message: 'Nhập họ tên' }]}>
                <Input prefix={<UserOutlined />} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="dateOfBirth" label="Ngày sinh">
                <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY"
                  disabledDate={(d) => d.isAfter(dayjs())} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="gender" label="Giới tính">
                <Select options={[
                  { value: 'MALE', label: 'Nam' },
                  { value: 'FEMALE', label: 'Nữ' },
                  { value: 'OTHER', label: 'Khác' },
                ]} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="nationality" label="Quốc tịch">
                <Input placeholder="VD: VN" maxLength={10} />
              </Form.Item>
            </Col>
            {/* Only staff can change status */}
            {isStaff && (
              <Col xs={24} md={12}>
                <Form.Item name="status" label="Trạng thái">
                  <Select options={[
                    { value: 'ACTIVE',   label: 'ACTIVE — Đang hoạt động' },
                    { value: 'INACTIVE', label: 'INACTIVE — Không hoạt động' },
                    { value: 'BLOCKED',  label: 'BLOCKED — Bị khoá' },
                  ]} />
                </Form.Item>
              </Col>
            )}
          </Row>

          {updateCustomer.isError && (
            <Alert type="error" showIcon
              message="Cập nhật thất bại. Vui lòng thử lại."
              style={{ marginBottom: 16, borderRadius: 8 }} />
          )}

          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />}
              loading={updateCustomer.isPending}>
              Lưu thay đổi
            </Button>
            <Button icon={<CloseOutlined />}
              onClick={() => { setEditingInfo(false); infoForm.resetFields(); }}>
              Huỷ
            </Button>
          </Space>
        </Form>
      )}
    </div>
  );

  // ── Tab: Liên hệ ──────────────────────────────────────────────

  const renderContactsTab = () => {
    if (loadingContacts) return <Spin />;

    return (
      <div>
        {/* Add contact button */}
        {canEdit && (
          <div style={{ textAlign: 'right', marginBottom: 16 }}>
            <Button
              icon={<PlusOutlined />}
              onClick={() => setShowAddContact(true)}
              disabled={contacts.length >= 5}
            >
              Thêm liên hệ {contacts.length >= 5 ? '(Đã đủ 5)' : ''}
            </Button>
          </div>
        )}

        {contacts.length === 0 && (
          <Alert type="info" showIcon
            message="Chưa có thông tin liên hệ"
            description={canEdit ? 'Nhấn "Thêm liên hệ" để bổ sung.' : undefined}
            style={{ borderRadius: 8, marginBottom: 12 }} />
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {contacts.map((c) => (
            <Card
              key={c.contactId}
              size="small"
              style={{ borderRadius: 8 }}
              extra={canEdit && editingContact !== c.contactId && (
                <Button size="small" icon={<EditOutlined />}
                  onClick={() => startEditContact(c)}>
                  Chỉnh sửa
                </Button>
              )}
              title={
                <Space>
                  <UserOutlined />
                  {c.isPrimary
                    ? <Tag color="blue">Liên hệ chính</Tag>
                    : <Text type="secondary">Liên hệ phụ</Text>}
                </Space>
              }
            >
              {/* View mode */}
              {editingContact !== c.contactId && (
                <Descriptions size="small" column={{ xs: 1, sm: 2 }}>
                  <Descriptions.Item label={<><PhoneOutlined /> Điện thoại</>}>
                    {c.phoneNumber
                      ? <Text copyable>{c.phoneNumber}</Text>
                      : <Text type="secondary">—</Text>}
                  </Descriptions.Item>
                  <Descriptions.Item label={<><MailOutlined /> Email</>}>
                    {c.email
                      ? <Text copyable>{c.email}</Text>
                      : <Text type="secondary">—</Text>}
                  </Descriptions.Item>
                  <Descriptions.Item label={<><EnvironmentOutlined /> Địa chỉ</>} span={2}>
                    {[c.address, c.district, c.city].filter(Boolean).join(', ') || '—'}
                  </Descriptions.Item>
                </Descriptions>
              )}

              {/* Inline edit form */}
              {editingContact === c.contactId && (
                <Form
                  form={contactForm}
                  layout="vertical"
                  size="middle"
                  onFinish={handleSaveContact}
                  style={{ marginTop: 8 }}
                >
                  {renderContactFields('middle')}

                  {updateContact.isError && (
                    <Alert type="error" showIcon
                      message="Cập nhật thất bại"
                      style={{ marginBottom: 12, borderRadius: 8 }} />
                  )}

                  <Space>
                    <Button type="primary" htmlType="submit" size="small"
                      icon={<SaveOutlined />} loading={updateContact.isPending}>
                      Lưu
                    </Button>
                    <Button size="small" icon={<CloseOutlined />}
                      onClick={() => { setEditingContact(null); contactForm.resetFields(); }}>
                      Huỷ
                    </Button>
                  </Space>
                </Form>
              )}
            </Card>
          ))}
        </div>

        {/* Add Contact Modal */}
        <Modal
          open={showAddContact}
          title={<><PlusOutlined /> Thêm thông tin liên hệ</>}
          onCancel={() => { setShowAddContact(false); addForm.resetFields(); }}
          footer={null}
          destroyOnClose
        >
          <Form
            form={addForm}
            layout="vertical"
            size="large"
            onFinish={handleAddContact}
            initialValues={{ isPrimary: false }}
            style={{ marginTop: 8 }}
          >
            {renderContactFields()}

            <Form.Item name="isPrimary" label="Đặt làm liên hệ chính?">
              <Select options={[
                { value: false, label: 'Không — liên hệ phụ' },
                { value: true,  label: 'Có — thay liên hệ chính hiện tại' },
              ]} />
            </Form.Item>

            {addContact.isError && (
              <Alert type="error" showIcon
                message="Thêm liên hệ thất bại"
                style={{ marginBottom: 12, borderRadius: 8 }} />
            )}

            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => { setShowAddContact(false); addForm.resetFields(); }}>
                Huỷ
              </Button>
              <Button type="primary" htmlType="submit" icon={<SaveOutlined />}
                loading={addContact.isPending}>
                Thêm liên hệ
              </Button>
            </Space>
          </Form>
        </Modal>
      </div>
    );
  };

  // ── Tab: Tài khoản ────────────────────────────────────────────

  const renderAccountsTab = () => {
    const columns = [
      {
        title:     'Số tài khoản',
        dataIndex: 'accountNo',
        render:    (v: string) => (
          <Text strong copyable style={{ fontFamily: 'monospace' }}>{v}</Text>
        ),
      },
      {
        title:     'Tiền tệ',
        dataIndex: 'currency',
        render:    (v: string) => <Tag>{v}</Tag>,
      },
      {
        title:     'Trạng thái',
        dataIndex: 'status',
        render:    (v: string) => (
          <Tag color={v === 'ACTIVE' ? 'success' : 'default'}>{v}</Tag>
        ),
      },
      {
        title:  'Hành động',
        render: (_: unknown, row: { accountNo: string }) => (
          <Space size="small">
            <Button size="small" icon={<BankOutlined />}
              onClick={() => navigate(buildPath(ROUTES.ACCOUNT_DETAIL, { accountNo: row.accountNo }))}>
              Chi tiết
            </Button>
            {isStaff && (
              <Tooltip title="Mở sổ tiết kiệm từ tài khoản này">
                <Button size="small" type="primary" ghost icon={<PlusCircleOutlined />}
                  onClick={() => navigate(`${ROUTES.CONTRACT_OPEN}?cif=${cif}`)}>
                  Mở sổ TK
                </Button>
              </Tooltip>
            )}
          </Space>
        ),
      },
    ];

    return (
      <div>
        <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text type="secondary">CIF {cif} — {accounts.length} tài khoản hoạt động</Text>
          {isStaff && (
            <Button type="primary" icon={<PlusCircleOutlined />}
              onClick={() => navigate(`${ROUTES.ACCOUNTS_CREATE}?cif=${cif}`)}>
              Tạo tài khoản
            </Button>
          )}
        </div>
        <Table
          rowKey="accountNo" dataSource={accounts} columns={columns}
          loading={loadingAccounts} pagination={false} size="small"
          locale={{ emptyText: 'Không có tài khoản' }}
        />
      </div>
    );
  };

  // ── Tab items ─────────────────────────────────────────────────

  const tabItems = [
    {
      key:      'info',
      label:    <Space><UserOutlined /> Thông tin</Space>,
      children: renderInfoTab(),
    },
    {
      key:      'contacts',
      label:    (
        <Space>
          <PhoneOutlined />
          Liên hệ
          {contacts.length > 0 && (
            <Tag style={{ marginLeft: 4, fontSize: 11 }}>{contacts.length}</Tag>
          )}
        </Space>
      ),
      children: renderContactsTab(),
    },
    // Tab tài khoản: chỉ hiện cho staff hoặc chủ sở hữu xem tài khoản của mình
    ...(isStaff || isOwnProfile ? [{
      key:      'accounts',
      label:    <Space><BankOutlined /> Tài khoản</Space>,
      children: renderAccountsTab(),
    }] : []),
  ];

  // ── Render ────────────────────────────────────────────────────

  return (
    <div style={{ maxWidth: 900, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        {/* Quay lại: staff về trang tra cứu, customer về dashboard */}
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => isOwnProfile
            ? navigate(ROUTES.DASHBOARD)
            : navigate(-1)
          }
          style={{ marginBottom: 16 }}
        >
          {isOwnProfile ? 'Về trang chính' : 'Quay lại'}
        </Button>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <div style={{
            width: 56, height: 56, borderRadius: '50%',
            background: isOwnProfile ? '#f6ffed' : '#e6f4ff',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 24, color: isOwnProfile ? '#52c41a' : '#1677ff',
          }}>
            <UserOutlined />
          </div>
          <div>
            <Title level={4} style={{ margin: 0 }}>
              {isOwnProfile ? 'Thông tin cá nhân của tôi' : customer.fullName}
            </Title>
            <Space size={8} style={{ flexWrap: 'wrap' }}>
              {!isOwnProfile && (
                <Text type="secondary" copyable={{ text: customer.cif }}>
                  CIF: {customer.cif}
                </Text>
              )}
              {isOwnProfile && (
                <Text type="secondary">CIF: {customer.cif}</Text>
              )}
              <Tag color={CUST_STATUS_COLOR[customer.status] ?? 'default'}>
                {customer.status}
              </Tag>
              {kycCfg && (
                <Tag icon={kycCfg.icon} color={kycCfg.color}>
                  KYC: {kycCfg.label}
                </Tag>
              )}
            </Space>
          </div>
        </div>
      </div>

      {/* Own profile info banner */}
      {isOwnProfile && (
        <Alert
          type="info"
          showIcon
          message="Bạn đang xem thông tin cá nhân của mình. Nhấn vào tab để chỉnh sửa."
          style={{ marginBottom: 16, borderRadius: 8 }}
          closable
        />
      )}

      {/* Tabs */}
      <Card style={{ borderRadius: 12 }}>
        <Tabs items={tabItems} defaultActiveKey="info" size="large" />
      </Card>
    </div>
  );
}
