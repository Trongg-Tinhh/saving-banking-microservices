import { Modal, Typography } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';

const { Text } = Typography;

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  content: React.ReactNode;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
  okText?: string;
  cancelText?: string;
  danger?: boolean;
}

export default function ConfirmDialog({
  open,
  title,
  content,
  onConfirm,
  onCancel,
  loading = false,
  okText = 'Xác nhận',
  cancelText = 'Huỷ',
  danger = false,
}: ConfirmDialogProps) {
  return (
    <Modal
      open={open}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <ExclamationCircleOutlined style={{ color: danger ? '#ff4d4f' : '#faad14', fontSize: 18 }} />
          <Text strong>{title}</Text>
        </div>
      }
      onOk={onConfirm}
      onCancel={onCancel}
      okText={okText}
      cancelText={cancelText}
      okButtonProps={{ loading, danger }}
      cancelButtonProps={{ disabled: loading }}
      maskClosable={!loading}
      closable={!loading}
    >
      {content}
    </Modal>
  );
}
