import type { ContractStatus } from '@/types';

interface StatusConfig {
  label: string;
  color: string;       // Ant Design color token or hex
  antColor: 'success' | 'processing' | 'warning' | 'error' | 'default';
}

const STATUS_MAP: Record<ContractStatus, StatusConfig> = {
  PENDING:      { label: 'Chờ xử lý',       color: 'gold',    antColor: 'warning' },
  ACTIVE:       { label: 'Đang hoạt động',   color: 'green',   antColor: 'success' },
  MATURED:      { label: 'Đã đáo hạn',       color: 'blue',    antColor: 'processing' },
  CLOSED:       { label: 'Đã tất toán',      color: 'default', antColor: 'default' },
  EARLY_CLOSED: { label: 'Tất toán trước hạn', color: 'orange', antColor: 'warning' },
  CANCELLED:    { label: 'Đã huỷ',           color: 'red',     antColor: 'error' },
  FAILED:       { label: 'Thất bại',         color: 'red',     antColor: 'error' },
};

export function getStatusConfig(status: ContractStatus): StatusConfig {
  return STATUS_MAP[status] ?? { label: status, color: 'default', antColor: 'default' };
}

export function getStatusLabel(status: ContractStatus): string {
  return STATUS_MAP[status]?.label ?? status;
}

export function getStatusColor(status: ContractStatus): string {
  return STATUS_MAP[status]?.color ?? 'default';
}

export function isContractActive(status: ContractStatus): boolean {
  return status === 'ACTIVE';
}

export function isContractClosed(status: ContractStatus): boolean {
  return ['CLOSED', 'EARLY_CLOSED', 'CANCELLED', 'FAILED'].includes(status);
}
