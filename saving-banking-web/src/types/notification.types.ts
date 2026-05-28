export type NotificationChannel = 'SMS' | 'EMAIL' | 'PUSH';
export type NotificationStatus = 'SENT' | 'FAILED' | 'PENDING';

export interface Notification {
  notificationId: string;
  cif: string;
  templateCode: string;
  channel: NotificationChannel;
  recipient: string;
  contentSummary: string;
  eventType: string;
  correlationId?: string;
  status: NotificationStatus;
  isRead?: boolean;
  sentAt: string | null;
  createdAt: string;
}
