import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity({ name: 'notifications', schema: 'notification_schema' })
export class NotificationEntity {

  @PrimaryGeneratedColumn('uuid', { name: 'notification_id' })
  notificationId: string;

  @Column({ name: 'cif', type: 'varchar', length: 20 })
  cif: string;

  @Column({ name: 'event_type', type: 'varchar', length: 50 })
  eventType: string;

  @Column({ name: 'contract_no', type: 'varchar', length: 50, nullable: true })
  contractNo: string | null;

  /** EMAIL | SMS | PUSH */
  @Column({ name: 'channel', type: 'varchar', length: 20, default: 'EMAIL' })
  channel: string;

  @Column({ name: 'recipient', type: 'varchar', length: 200, nullable: true })
  recipient: string | null;

  @Column({ name: 'template_code', type: 'varchar', length: 50, nullable: true })
  templateCode: string | null;

  @Column({ name: 'content_summary', type: 'varchar', length: 500, nullable: true })
  contentSummary: string | null;

  /** PENDING | SENT | FAILED */
  @Column({ name: 'status', type: 'varchar', length: 20, default: 'SENT' })
  status: string;

  @Column({ name: 'is_read', type: 'boolean', default: false })
  isRead: boolean;

  @Column({ name: 'correlation_id', type: 'varchar', length: 100, nullable: true })
  correlationId: string | null;

  @Column({ name: 'sent_at', type: 'timestamptz', nullable: true })
  sentAt: Date | null;

  @CreateDateColumn({ name: 'created_at', type: 'timestamptz' })
  createdAt: Date;
}
