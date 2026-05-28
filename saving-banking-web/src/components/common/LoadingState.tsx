import { Skeleton, Card, Row, Col } from 'antd';

interface LoadingStateProps {
  type?: 'card' | 'table' | 'detail';
  rows?: number;
}

export default function LoadingState({ type = 'card', rows = 3 }: LoadingStateProps) {
  if (type === 'table') {
    return (
      <Card>
        {Array.from({ length: rows }).map((_, i) => (
          <Skeleton key={i} active paragraph={{ rows: 1 }} style={{ marginBottom: 16 }} />
        ))}
      </Card>
    );
  }

  if (type === 'detail') {
    return (
      <Card>
        <Skeleton active avatar paragraph={{ rows: 6 }} />
      </Card>
    );
  }

  // Default: card grid skeleton
  return (
    <Row gutter={[16, 16]}>
      {Array.from({ length: rows }).map((_, i) => (
        <Col key={i} xs={24} sm={12} lg={6}>
          <Card>
            <Skeleton active paragraph={{ rows: 2 }} />
          </Card>
        </Col>
      ))}
    </Row>
  );
}
