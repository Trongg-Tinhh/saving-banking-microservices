import { Typography } from 'antd';
import { formatVND, formatAmount } from '@/utils/formatCurrency';

const { Text } = Typography;

interface AmountTextProps {
  amount: number;
  currency?: string;
  showSymbol?: boolean;
  strong?: boolean;
  style?: React.CSSProperties;
  type?: 'success' | 'danger' | 'warning' | 'secondary';
}

export default function AmountText({
  amount,
  currency,
  showSymbol = true,
  strong = false,
  style,
  type,
}: AmountTextProps) {
  const text = currency ? formatAmount(amount, currency) : formatVND(amount, showSymbol);

  return (
    <Text strong={strong} type={type} style={style}>
      {text}
    </Text>
  );
}
