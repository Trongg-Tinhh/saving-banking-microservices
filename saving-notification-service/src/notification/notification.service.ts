import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { NotificationEntity } from './notification.entity';

export interface NotificationPayload {
  type: 'CONTRACT_OPENED' | 'CONTRACT_CLOSED' | 'CONTRACT_MATURED';
  contractNo: string;
  cif: string;
  recipientEmail?: string;
  recipientPhone?: string;
  details: Record<string, unknown>;
  correlationId?: string;
}

const TEMPLATE_MAP: Record<string, string> = {
  CONTRACT_OPENED:  'CONTRACT_OPENED_EMAIL',
  CONTRACT_CLOSED:  'CONTRACT_CLOSED_EMAIL',
  CONTRACT_MATURED: 'CONTRACT_MATURED_EMAIL',
};

const SUMMARY_MAP: Record<string, (d: Record<string, unknown>) => string> = {
  CONTRACT_OPENED:  (d) =>
    `Mở sổ tiết kiệm ${d['contractNo']} — Số tiền: ${Number(d['principalAmount']).toLocaleString('vi-VN')}đ`,
  CONTRACT_CLOSED:  (d) =>
    `Tất toán hợp đồng ${d['contractNo']} — Nhận về: ${Number(d['totalPayout']).toLocaleString('vi-VN')}đ`,
  CONTRACT_MATURED: (d) =>
    `Hợp đồng ${d['contractNo']} đã đến hạn. Vui lòng liên hệ để tất toán hoặc gia hạn.`,
};

@Injectable()
export class NotificationService {
  private readonly logger = new Logger(NotificationService.name);

  constructor(
    @InjectRepository(NotificationEntity)
    private readonly repo: Repository<NotificationEntity>,
  ) {}

  // ── Dispatch (called by RabbitMQ consumer) ─────────────────────────────────

  async dispatch(payload: NotificationPayload): Promise<void> {
    const { type, contractNo, cif, correlationId, details } = payload;

    this.logger.log(
      `[${correlationId ?? 'N/A'}] Dispatching ${type}: contractNo=${contractNo} cif=${cif}`,
    );

    const summary = SUMMARY_MAP[type]?.(details) ??
      `${type} — hợp đồng ${contractNo}`;

    const entity = this.repo.create({
      cif,
      eventType:      type,
      contractNo,
      channel:        'EMAIL',
      templateCode:   TEMPLATE_MAP[type] ?? type,
      contentSummary: summary,
      status:         'SENT',
      isRead:         false,
      correlationId,
      sentAt:         new Date(),
    });

    await this.repo.save(entity);
    this.logger.log(`[${correlationId ?? 'N/A'}] Notification saved: id=${entity.notificationId}`);
  }

  // ── List (paginated) ────────────────────────────────────────────────────────

  async list(page: number, size: number): Promise<{
    content: NotificationEntity[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
    empty: boolean;
  }> {
    const [content, total] = await this.repo.findAndCount({
      order:  { createdAt: 'DESC' },
      skip:   page * size,
      take:   size,
    });

    const totalPages = Math.ceil(total / size) || 1;

    return {
      content,
      totalElements:    total,
      totalPages,
      size,
      number:           page,
      first:            page === 0,
      last:             page >= totalPages - 1,
      empty:            content.length === 0,
    };
  }

  // ── Mark read ───────────────────────────────────────────────────────────────

  async markRead(notificationId: string): Promise<void> {
    const n = await this.repo.findOne({ where: { notificationId } });
    if (!n) throw new NotFoundException(`Notification ${notificationId} not found`);
    n.isRead = true;
    await this.repo.save(n);
  }

  async markAllRead(cif?: string): Promise<void> {
    const qb = this.repo.createQueryBuilder()
      .update(NotificationEntity)
      .set({ isRead: true })
      .where('is_read = false');

    if (cif) qb.andWhere('cif = :cif', { cif });

    await qb.execute();
  }

  // ── Unread count ────────────────────────────────────────────────────────────

  async unreadCount(cif?: string): Promise<number> {
    const qb = this.repo.createQueryBuilder('n')
      .where('n.isRead = false');
    if (cif) qb.andWhere('n.cif = :cif', { cif });
    return qb.getCount();
  }
}
