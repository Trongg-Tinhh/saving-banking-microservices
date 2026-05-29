import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Spin } from 'antd';
import { ROUTES } from '@/constants/routes';
import ProtectedRoute from './ProtectedRoute';
import MainLayout from '@/layouts/MainLayout';
import AuthLayout from '@/layouts/AuthLayout';

// ─── Lazy-loaded pages ─────────────────────────────────────────

const LoginPage             = lazy(() => import('@/pages/auth/LoginPage'));
const DashboardPage         = lazy(() => import('@/pages/dashboard/DashboardPage'));
const ProductListPage       = lazy(() => import('@/pages/products/ProductListPage'));
const ProductDetailPage     = lazy(() => import('@/pages/products/ProductDetailPage'));
const ProductFormPage       = lazy(() => import('@/pages/products/ProductFormPage'));
const SimulatePage          = lazy(() => import('@/pages/simulate/SimulatePage'));
const ContractListPage      = lazy(() => import('@/pages/contracts/ContractListPage'));
const ContractDetailPage    = lazy(() => import('@/pages/contracts/ContractDetailPage'));
const OpenSavingPage        = lazy(() => import('@/pages/contracts/OpenSavingPage'));
const CloseSavingPage       = lazy(() => import('@/pages/contracts/CloseSavingPage'));
const MaturityPage          = lazy(() => import('@/pages/contracts/MaturityPage'));
const TransactionPage       = lazy(() => import('@/pages/transactions/TransactionPage'));
const NotificationPage      = lazy(() => import('@/pages/notifications/NotificationPage'));
const CreateAccountPage     = lazy(() => import('@/pages/accounts/CreateAccountPage'));
const AccountDetailPage     = lazy(() => import('@/pages/accounts/AccountDetailPage'));
const CustomerSearchPage    = lazy(() => import('@/pages/customers/CustomerSearchPage'));
const CustomerCreatePage    = lazy(() => import('@/pages/customers/CustomerCreatePage'));
const CustomerDetailPage    = lazy(() => import('@/pages/customers/CustomerDetailPage'));
const NotFoundPage          = lazy(() => import('@/pages/errors/NotFoundPage'));
const ForbiddenPage         = lazy(() => import('@/pages/errors/ForbiddenPage'));

const PageFallback = () => (
  <div style={{ padding: 40, textAlign: 'center' }}>
    <Spin size="large" />
  </div>
);

// ─── Router Definition ──────────────────────────────────────────

export const router = createBrowserRouter([
  // ── Auth routes (no sidebar) ──────────────────────────────────
  {
    path: ROUTES.LOGIN,
    element: (
      <AuthLayout>
        <Suspense fallback={<PageFallback />}>
          <LoginPage />
        </Suspense>
      </AuthLayout>
    ),
  },

  // ── Protected app routes (with sidebar) ───────────────────────
  {
    path: ROUTES.ROOT,
    element: (
      <ProtectedRoute>
        <MainLayout />
      </ProtectedRoute>
    ),
    children: [
      // Default redirect to dashboard
      { index: true, element: <Navigate to={ROUTES.DASHBOARD} replace /> },

      {
        path: ROUTES.DASHBOARD,
        element: <Suspense fallback={<PageFallback />}><DashboardPage /></Suspense>,
      },
      {
        path: ROUTES.PRODUCTS,
        element: <Suspense fallback={<PageFallback />}><ProductListPage /></Suspense>,
      },
      // create must come BEFORE :productCode to avoid route collision
      {
        path: ROUTES.PRODUCT_CREATE,
        element: <Suspense fallback={<PageFallback />}><ProductFormPage /></Suspense>,
      },
      {
        path: ROUTES.PRODUCT_EDIT,
        element: <Suspense fallback={<PageFallback />}><ProductFormPage /></Suspense>,
      },
      {
        path: ROUTES.PRODUCT_DETAIL,
        element: <Suspense fallback={<PageFallback />}><ProductDetailPage /></Suspense>,
      },
      {
        path: ROUTES.SIMULATE,
        element: <Suspense fallback={<PageFallback />}><SimulatePage /></Suspense>,
      },
      // Contract open must come BEFORE :contractNo to avoid route collision
      {
        path: ROUTES.CONTRACT_OPEN,
        element: <Suspense fallback={<PageFallback />}><OpenSavingPage /></Suspense>,
      },
      {
        path: ROUTES.CONTRACTS,
        element: <Suspense fallback={<PageFallback />}><ContractListPage /></Suspense>,
      },
      {
        path: ROUTES.CONTRACT_DETAIL,
        element: <Suspense fallback={<PageFallback />}><ContractDetailPage /></Suspense>,
      },
      {
        path: ROUTES.CONTRACT_CLOSE,
        element: <Suspense fallback={<PageFallback />}><CloseSavingPage /></Suspense>,
      },
      {
        path: ROUTES.CONTRACT_MATURITY,
        element: <Suspense fallback={<PageFallback />}><MaturityPage /></Suspense>,
      },
      {
        path: ROUTES.TRANSACTIONS,
        element: <Suspense fallback={<PageFallback />}><TransactionPage /></Suspense>,
      },
      // Accounts — create must come BEFORE :accountNo
      {
        path: ROUTES.ACCOUNTS_CREATE,
        element: <Suspense fallback={<PageFallback />}><CreateAccountPage /></Suspense>,
      },
      {
        path: ROUTES.ACCOUNT_DETAIL,
        element: <Suspense fallback={<PageFallback />}><AccountDetailPage /></Suspense>,
      },
      // Customers — create must come BEFORE :cif to avoid route collision
      {
        path: ROUTES.CUSTOMER_CREATE,
        element: <Suspense fallback={<PageFallback />}><CustomerCreatePage /></Suspense>,
      },
      {
        path: ROUTES.CUSTOMERS,
        element: <Suspense fallback={<PageFallback />}><CustomerSearchPage /></Suspense>,
      },
      {
        path: ROUTES.CUSTOMER_DETAIL,
        element: <Suspense fallback={<PageFallback />}><CustomerDetailPage /></Suspense>,
      },
      {
        path: ROUTES.NOTIFICATIONS,
        element: <Suspense fallback={<PageFallback />}><NotificationPage /></Suspense>,
      },
    ],
  },

  // ── Error pages ───────────────────────────────────────────────
  {
    path: ROUTES.FORBIDDEN,
    element: <Suspense fallback={<PageFallback />}><ForbiddenPage /></Suspense>,
  },
  {
    path: ROUTES.NOT_FOUND,
    element: <Suspense fallback={<PageFallback />}><NotFoundPage /></Suspense>,
  },
  {
    path: '*',
    element: <Navigate to={ROUTES.NOT_FOUND} replace />,
  },
]);
