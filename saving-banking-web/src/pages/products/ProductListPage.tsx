import { useState } from 'react';
import {
  Typography,
  Card,
  Row,
  Col,
  Tag,
  Button,
  Tabs,
  Skeleton,
  Badge,
  Space,
  Tooltip,
  Alert,
} from 'antd';
import {
  AppstoreOutlined,
  ArrowRightOutlined,
  BankOutlined,
  CalendarOutlined,
  PercentageOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useProducts } from '@/hooks/useProducts';
import { buildPath, ROUTES } from '@/constants/routes';
import { useAuthStore } from '@/stores/authStore';
import { formatVND } from '@/utils/formatCurrency';
import type { SavingProduct, InterestPaymentMethod } from '@/types';
import EmptyState from '@/components/common/EmptyState';

const { Title, Text } = Typography;

// ─── Helpers ──────────────────────────────────────────────────────

const PAYMENT_METHOD_LABELS: Record<InterestPaymentMethod, string> = {
  END_OF_TERM: 'Cuối kỳ',
  MONTHLY:     'Hàng tháng',
  QUARTERLY:   'Hàng quý',
};

const PAYMENT_METHOD_COLORS: Record<InterestPaymentMethod, string> = {
  END_OF_TERM: 'default',
  MONTHLY:     'blue',
  QUARTERLY:   'purple',
};

function getRateRange(product: SavingProduct): string {
  // Filter out undefined/null annualRate values (terms with no active rate yet)
  const rates = product.terms
    .map((t) => t.annualRate)
    .filter((r): r is number => r != null && !isNaN(r));
  if (rates.length === 0) return '—';
  const min = Math.min(...rates);
  const max = Math.max(...rates);
  if (min === max) return `${min.toFixed(2)}%`;
  return `${min.toFixed(2)}% – ${max.toFixed(2)}%`;
}

// ─── Product Card ─────────────────────────────────────────────────

function ProductCard({
  product,
  onClick,
}: {
  product: SavingProduct;
  onClick: () => void;
}) {
  return (
    <Card
      hoverable
      onClick={onClick}
      style={{ borderRadius: 12, height: '100%' }}
      styles={{ body: { padding: '20px 24px' } }}
    >
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
        <div style={{
          width: 40,
          height: 40,
          borderRadius: 10,
          background: product.isActive ? '#e6f4ff' : '#f5f5f5',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}>
          <BankOutlined style={{ fontSize: 20, color: product.isActive ? '#1677ff' : '#bfbfbf' }} />
        </div>
        <Badge
          status={product.isActive ? 'success' : 'default'}
          text={product.isActive ? 'Hoạt động' : 'Ngừng'}
          style={{ fontSize: 12 }}
        />
      </div>

      {/* Name & code */}
      <Text strong style={{ fontSize: 15, display: 'block', marginBottom: 2, lineHeight: 1.4 }}>
        {product.productName}
      </Text>
      <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 16 }}>
        Mã: {product.productCode} · {product.currency}
      </Text>

      {/* Stats */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space size={4}>
            <PercentageOutlined style={{ fontSize: 13, color: '#1677ff' }} />
            <Text type="secondary" style={{ fontSize: 13 }}>Lãi suất</Text>
          </Space>
          <Text strong style={{ fontSize: 13 }}>{getRateRange(product)}</Text>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space size={4}>
            <CalendarOutlined style={{ fontSize: 13, color: '#52c41a' }} />
            <Text type="secondary" style={{ fontSize: 13 }}>Kỳ hạn</Text>
          </Space>
          <Text strong style={{ fontSize: 13 }}>{product.terms.length} kỳ hạn</Text>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text type="secondary" style={{ fontSize: 13 }}>Trả lãi</Text>
          <Tag
            color={PAYMENT_METHOD_COLORS[product.interestPaymentMethod]}
            style={{ margin: 0, fontSize: 12 }}
          >
            {PAYMENT_METHOD_LABELS[product.interestPaymentMethod]}
          </Tag>
        </div>
      </div>

      {/* Amount range */}
      <div style={{
        marginTop: 16,
        padding: '10px 12px',
        background: '#fafafa',
        borderRadius: 8,
        fontSize: 12,
      }}>
        <Text type="secondary">
          {formatVND(product.minAmount, false)} – {formatVND(product.maxAmount)} {product.currency}
        </Text>
      </div>

      {/* Footer */}
      <div style={{ marginTop: 14, display: 'flex', justifyContent: 'flex-end' }}>
        <Tooltip title="Xem chi tiết">
          <Button
            type="link"
            icon={<ArrowRightOutlined />}
            style={{ padding: 0, height: 'auto', fontSize: 13 }}
            onClick={(e) => { e.stopPropagation(); onClick(); }}
          >
            Chi tiết
          </Button>
        </Tooltip>
      </div>
    </Card>
  );
}

// ─── Product Skeleton ─────────────────────────────────────────────

function ProductSkeleton() {
  return (
    <Card style={{ borderRadius: 12 }} styles={{ body: { padding: '20px 24px' } }}>
      <Skeleton active paragraph={{ rows: 5 }} />
    </Card>
  );
}

// ─── Page ─────────────────────────────────────────────────────────

export default function ProductListPage() {
  const navigate    = useNavigate();
  const { isAdmin } = useAuthStore();
  const admin       = isAdmin();

  const [filterActive, setFilterActive] = useState(false);

  // Admin: fetch all products (active + inactive) for "Tất cả" tab
  const {
    data: allProducts = [],
    isLoading: loadingAll,
    isError: errorAll,
    refetch: refetchAll,
    isFetching: fetchingAll,
  } = useProducts(false);

  // Active-only: used for admin "Đang hoạt động" tab + non-admin always
  const {
    data: activeProducts = [],
    isLoading: loadingActive,
    isError: errorActive,
    refetch: refetchActive,
    isFetching: fetchingActive,
  } = useProducts(true);

  // What to display depends on role + selected tab
  const showAll   = admin && !filterActive;
  const products  = showAll ? allProducts  : activeProducts;
  const isLoading = showAll ? loadingAll   : loadingActive;
  const isError   = showAll ? errorAll     : errorActive;
  const isFetching = showAll ? fetchingAll : fetchingActive;
  const refetch   = showAll ? refetchAll   : refetchActive;
  const activeCount = activeProducts.length;

  const handleCardClick = (productCode: string) => {
    navigate(buildPath(ROUTES.PRODUCT_DETAIL, { productCode }));
  };

  return (
    <div>
      {/* Page header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>Sản phẩm tiết kiệm</Title>
          <Text type="secondary">
            {isLoading
              ? 'Đang tải...'
              : admin
                ? filterActive
                  ? `${activeCount} sản phẩm đang hoạt động`
                  : `${allProducts.length} sản phẩm (${activeCount} đang hoạt động)`
                : `${activeCount} sản phẩm đang hoạt động`}
          </Text>
        </div>
        <Space>
          {isAdmin() && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate(ROUTES.PRODUCT_CREATE)}
            >
              Tạo sản phẩm
            </Button>
          )}
          <Button
            icon={<ReloadOutlined spin={isFetching} />}
            onClick={() => refetch()}
            disabled={isFetching}
          >
            Làm mới
          </Button>
        </Space>
      </div>

      {/* Error banner */}
      {isError && (
        <Alert
          type="error"
          message="Không thể tải danh sách sản phẩm"
          description="Vui lòng kiểm tra kết nối và thử lại."
          showIcon
          closable
          style={{ marginBottom: 20 }}
          action={<Button size="small" onClick={() => refetch()}>Thử lại</Button>}
        />
      )}

      {/* Filter tabs — admin only (non-admin always sees active products) */}
      {admin && (
        <Tabs
          activeKey={filterActive ? 'active' : 'all'}
          onChange={(key) => setFilterActive(key === 'active')}
          style={{ marginBottom: 20 }}
          items={[
            {
              key:   'all',
              label: loadingAll
                ? 'Tất cả (…)'
                : `Tất cả (${allProducts.length})`,
            },
            {
              key:   'active',
              label: loadingActive
                ? 'Đang hoạt động (…)'
                : `Đang hoạt động (${activeCount})`,
            },
          ]}
        />
      )}

      {/* Grid */}
      {isLoading ? (
        <Row gutter={[16, 16]}>
          {Array.from({ length: 6 }).map((_, i) => (
            <Col key={i} xs={24} sm={12} lg={8}>
              <ProductSkeleton />
            </Col>
          ))}
        </Row>
      ) : products.length === 0 ? (
        <Card style={{ borderRadius: 12 }}>
          <EmptyState
            icon={<AppstoreOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />}
            title="Chưa có sản phẩm nào"
            description={
              filterActive
                ? 'Không có sản phẩm nào đang hoạt động.'
                : 'Chưa có sản phẩm tiết kiệm nào được tạo.'
            }
            action={
              filterActive ? (
                <Button onClick={() => setFilterActive(false)}>Xem tất cả</Button>
              ) : undefined
            }
          />
        </Card>
      ) : (
        <Row gutter={[16, 16]}>
          {products.map((product) => (
            <Col key={product.productCode} xs={24} sm={12} lg={8}>
              <ProductCard
                product={product}
                onClick={() => handleCardClick(product.productCode)}
              />
            </Col>
          ))}
        </Row>
      )}
    </div>
  );
}
