import { Tag } from 'antd';
import type { ContractStatus } from '@/types';
import { getStatusConfig } from '@/utils/contractStatus';

interface StatusChipProps {
  status: ContractStatus;
}

export default function StatusChip({ status }: StatusChipProps) {
  const { label, color } = getStatusConfig(status);
  return <Tag color={color}>{label}</Tag>;
}
