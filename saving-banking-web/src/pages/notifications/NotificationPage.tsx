import { useState } from 'react';
import {
  Card,
  Typography,
  Button,
  List,
  Avatar,
  Badge,
  Space,
  Tag,
  Alert,
  Skeleton,
  Pagination,
} from 'antd';
import {
  BellOutlined,
  CheckOutlined,
  MailOutlined,
  MobileOutlined,
  NotificationOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { useNotifications, useMarkRead, useMarkAllRead } from '@/hooks/useNotifications';
import { formatDateTime } from '@/utils/formatDate';
import type { Notification, NotificationChannel, NotificationStatus } from '@/types';

const { Title, Text } = Typography;

// ─── Label / icon maps ────────────────────────────────────────────

const CHANNEL_ICON: Record<NotificationChannel, React.ReactNode> = {
  SMS:   <MobileOutlined />,
  EMAIL: <MailOutlined />,
  PUSH:  <NotificationOutlined />,
};

const CHANNEL_COLOR: Record<NotificationChannel, string> = {
  SMS:   '#1677ff',
  EMAIL: '#52c41a',
  PUSH:  '#fa8c16',
};

const STATUS_COLOR: Record<NotificationStatus, string> = {
  SENT:    'success',
  PENDING: 'processing',
  FAILED:  'error',
};

const STATUS_LABEL: Record<NotificationStatus, string> = {
  SENT:    'Đã gửi',
  PENDING: 'Đang gửi',
  FAILED:  'Thất bại',
};

// ─── Page ─────────────────────────────────────────────────────────

export default function NotificationPage() {
  const [page, setPage] = useState(0);

  const {
    data,
    isLoading,
    isError,
    isFetching,
    refetch,
  } = useNotifications(page);

  const markRead    = useMarkRead();
  const markAllRead = useMarkAllRead();

  const notifications = data?.content ?? [];
  const total         = data?.totalElements ?? 0;
  const unreadCount   = notifications.filter((n) => !n.isRead).length;

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <Space>
            <Title level={4} style={{ margin: 0 }}>Thông báo</Title>
            {unreadCount > 0 && (
              <Badge count={unreadCount} style={{ backgroundColor: '#1677ff' }} />
            )}
          </Space>
          <Text type="secondary">
            {isLoading ? 'Đang tải...' : `${total.toLocaleString('vi-VN')} thông báo`}
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
            icon={<CheckOutlined />}
            disabled={unreadCount === 0 || markAllRead.isPending}
            loading={markAllRead.isPending}
            onClick={() => markAllRead.mutate()}
          >
            Đọc tất cả
          </Button>
        </Space>
      </div>

      {/* Error */}
      {isError && (
        <Alert
          type="error"
          message="Không thể tải thông báo"
          showIcon
          closable
          style={{ marginBottom: 16, borderRadius: 8 }}
          action={<Button size="small" onClick={() => refetch()}>Thử lại</Button>}
        />
      )}

      {/* List */}
      <Card style={{ borderRadius: 12 }}>
        {isLoading ? (
          <div>
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} style={{ padding: '16px 0', borderBottom: '1px solid #f0f0f0' }}>
                <Skeleton avatar active paragraph={{ rows: 2 }} />
              </div>
            ))}
          </div>
        ) : notifications.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '48px 0' }}>
            <BellOutlined style={{ fontSize: 48, color: '#d9d9d9', display: 'block', marginBottom: 12 }} />
            <Text type="secondary">Chưa có thông báo nào</Text>
          </div>
        ) : (
          <List<Notification>
            itemLayout="horizontal"
            dataSource={notifications}
            renderItem={(item) => (
              <List.Item
                style={{
                  background: item.isRead ? 'transparent' : '#f0f7ff',
                  padding: '14px 16px',
                  borderRadius: 8,
                  marginBottom: 6,
                  cursor: 'default',
                  transition: 'background 0.2s',
                }}
                actions={[
                  !item.isRead && (
                    <Button
                      type="link"
                      size="small"
                      key="read"
                      loading={markRead.isPending}
                      onClick={() => markRead.mutate(item.notificationId)}
                    >
                      Đánh dấu đã đọc
                    </Button>
                  ),
                ].filter(Boolean)}
              >
                <List.Item.Meta
                  avatar={
                    <Badge dot={!item.isRead} color={CHANNEL_COLOR[item.channel]}>
                      <Avatar
                        icon={CHANNEL_ICON[item.channel]}
                        style={{ background: CHANNEL_COLOR[item.channel] }}
                      />
                    </Badge>
                  }
                  title={
                    <Space size={6} wrap>
                      <Text strong={!item.isRead} style={{ fontSize: 14 }}>
                        {item.contentSummary}
                      </Text>
                      {!item.isRead && (
                        <Tag color="blue" style={{ fontSize: 11, lineHeight: '18px' }}>Mới</Tag>
                      )}
                      <Tag color={STATUS_COLOR[item.status]} style={{ fontSize: 11, lineHeight: '18px' }}>
                        {STATUS_LABEL[item.status]}
                      </Tag>
                    </Space>
                  }
                  description={
                    <Space direction="vertical" size={2}>
                      <Space size={4} wrap>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          Kênh: <Text code style={{ fontSize: 11 }}>{item.channel}</Text>
                        </Text>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          Sự kiện: <Text code style={{ fontSize: 11 }}>{item.eventType}</Text>
                        </Text>
                        {item.recipient && (
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            Đến: {item.recipient}
                          </Text>
                        )}
                      </Space>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {item.sentAt ? formatDateTime(item.sentAt) : `Tạo: ${formatDateTime(item.createdAt)}`}
                      </Text>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        )}

        {/* Pagination */}
        {total > 20 && (
          <div style={{ textAlign: 'center', marginTop: 20 }}>
            <Pagination
              current={page + 1}
              pageSize={20}
              total={total}
              onChange={(p) => setPage(p - 1)}
              showTotal={(tot) => `${tot} thông báo`}
            />
          </div>
        )}
      </Card>
    </div>
  );
}
