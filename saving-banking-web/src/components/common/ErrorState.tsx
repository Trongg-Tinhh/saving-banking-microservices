import { Result, Button } from 'antd';

interface ErrorStateProps {
  message?: string;
  description?: string;
  onRetry?: () => void;
}

export default function ErrorState({
  message = 'Đã có lỗi xảy ra',
  description = 'Vui lòng thử lại hoặc liên hệ hỗ trợ nếu lỗi vẫn tiếp diễn.',
  onRetry,
}: ErrorStateProps) {
  return (
    <Result
      status="error"
      title={message}
      subTitle={description}
      extra={
        onRetry && (
          <Button type="primary" onClick={onRetry}>
            Thử lại
          </Button>
        )
      }
    />
  );
}
