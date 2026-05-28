import { Empty, Button } from 'antd';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title?: string;
  description?: string;
  action?: React.ReactNode;
  // Legacy props (kept for backwards compatibility)
  actionLabel?: string;
  onAction?: () => void;
}

export default function EmptyState({
  icon,
  title = 'Không có dữ liệu',
  description,
  action,
  actionLabel,
  onAction,
}: EmptyStateProps) {
  const cta = action ?? (actionLabel && onAction ? (
    <Button type="primary" onClick={onAction}>
      {actionLabel}
    </Button>
  ) : null);

  return (
    <Empty
      image={icon ?? Empty.PRESENTED_IMAGE_SIMPLE}
      description={
        <span>
          <strong>{title}</strong>
          {description && (
            <>
              <br />
              <span style={{ color: '#888', fontSize: 13 }}>{description}</span>
            </>
          )}
        </span>
      }
    >
      {cta}
    </Empty>
  );
}
