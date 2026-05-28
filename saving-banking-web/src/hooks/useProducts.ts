import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { App } from 'antd';
import { useNavigate } from 'react-router-dom';
import { productService } from '@/services/productService';
import { getApiErrorMessage } from '@/services/api';
import { QUERY_KEYS } from '@/constants/queryKeys';
import { ROUTES, buildPath } from '@/constants/routes';
import type {
  CreateProductRequest,
  UpdateProductRequest,
  CreateTermRequest,
  UpdateTermRequest,
  CreateRateConfigRequest,
  UpsertEarlyWithdrawalPolicyRequest,
} from '@/types';

// ─── Product List ─────────────────────────────────────────────────

export function useProducts(onlyActive = false) {
  return useQuery({
    queryKey: [...QUERY_KEYS.PRODUCTS, { onlyActive }],
    queryFn: () => productService.listProducts(onlyActive),
    staleTime: 5 * 60 * 1000,
  });
}

// ─── Single Product ───────────────────────────────────────────────

export function useProduct(productCode: string | undefined) {
  return useQuery({
    queryKey: QUERY_KEYS.PRODUCT(productCode ?? ''),
    queryFn: () => productService.getProduct(productCode!),
    enabled: Boolean(productCode),
    staleTime: 5 * 60 * 1000,
  });
}

// ─── Product Terms ────────────────────────────────────────────────

/** Active terms only (default — used on most read views) */
export function useProductTerms(productCode: string | undefined) {
  return useQuery({
    queryKey: ['products', productCode, 'terms'],
    queryFn: () => productService.getTerms(productCode!),
    enabled: Boolean(productCode),
    staleTime: 5 * 60 * 1000,
  });
}

/** All terms including disabled — for ADMIN dropdowns (e.g. add rate config) */
export function useAllProductTerms(productCode: string | undefined) {
  return useQuery({
    queryKey: ['products', productCode, 'terms', 'all'],
    queryFn: () => productService.getTerms(productCode!, false),
    enabled: Boolean(productCode),
    staleTime: 5 * 60 * 1000,
  });
}

// ─── Rate History ─────────────────────────────────────────────────

export function useProductRates(productCode: string | undefined) {
  return useQuery({
    queryKey: ['products', productCode, 'rates'],
    queryFn: () => productService.getRates(productCode!),
    enabled: Boolean(productCode),
    staleTime: 5 * 60 * 1000,
  });
}

// ─── Early Withdrawal Policy ──────────────────────────────────────

export function useEarlyWithdrawalPolicy(productCode: string | undefined) {
  return useQuery({
    queryKey: ['products', productCode, 'early-withdrawal'],
    queryFn: () => productService.getEarlyWithdrawalPolicy(productCode!),
    enabled: Boolean(productCode),
    staleTime: 10 * 60 * 1000,
  });
}

// ─── Add Term ─────────────────────────────────────────────────────

export function useCreateTerm(productCode: string | undefined) {
  const { message } = App.useApp();
  const qc          = useQueryClient();

  return useMutation({
    mutationFn: (req: CreateTermRequest) => productService.createTerm(productCode!, req),
    onSuccess: () => {
      message.success('Thêm kỳ hạn thành công!');
      qc.invalidateQueries({ queryKey: ['products', productCode, 'terms'] });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.PRODUCT(productCode!) });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể thêm kỳ hạn.'));
    },
  });
}

// ─── Update Term (rename / toggle) ───────────────────────────────

export function useUpdateTerm(productCode: string | undefined) {
  const { message } = App.useApp();
  const qc          = useQueryClient();

  return useMutation({
    mutationFn: ({ termId, req }: { termId: string; req: UpdateTermRequest }) =>
      productService.updateTerm(productCode!, termId, req),
    onSuccess: (_data, { req }) => {
      const action = req.isActive === false ? 'Đã vô hiệu hoá' : req.isActive === true ? 'Đã kích hoạt' : 'Đã cập nhật';
      message.success(`${action} kỳ hạn`);
      qc.invalidateQueries({ queryKey: ['products', productCode, 'terms'] });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.PRODUCT(productCode!) });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể cập nhật kỳ hạn.'));
    },
  });
}

// ─── Add Rate Config ──────────────────────────────────────────────

export function useAddRateConfig(productCode: string | undefined) {
  const { message } = App.useApp();
  const qc          = useQueryClient();

  return useMutation({
    mutationFn: (req: CreateRateConfigRequest) => productService.addRateConfig(productCode!, req),
    onSuccess: () => {
      message.success('Thêm lãi suất thành công!');
      qc.invalidateQueries({ queryKey: ['products', productCode, 'rates'] });
      qc.invalidateQueries({ queryKey: ['products', productCode, 'terms'] });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.PRODUCT(productCode!) });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể thêm lãi suất.'));
    },
  });
}

// ─── Upsert Early Withdrawal Policy ──────────────────────────────

export function useUpsertEarlyWithdrawalPolicy(productCode: string | undefined) {
  const { message } = App.useApp();
  const qc          = useQueryClient();

  return useMutation({
    mutationFn: (req: UpsertEarlyWithdrawalPolicyRequest) =>
      productService.upsertEarlyWithdrawalPolicy(productCode!, req),
    onSuccess: () => {
      message.success('Lưu chính sách rút sớm thành công!');
      qc.invalidateQueries({ queryKey: ['products', productCode, 'early-withdrawal'] });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.PRODUCT(productCode!) });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể lưu chính sách rút sớm.'));
    },
  });
}

// ─── Create Product ───────────────────────────────────────────────

export function useCreateProduct() {
  const { message } = App.useApp();
  const navigate    = useNavigate();
  const qc          = useQueryClient();

  return useMutation({
    mutationFn: (req: CreateProductRequest) => productService.createProduct(req),
    onSuccess: (product) => {
      message.success(`Tạo sản phẩm "${product.productName}" thành công!`);
      qc.invalidateQueries({ queryKey: QUERY_KEYS.PRODUCTS });
      navigate(buildPath(ROUTES.PRODUCT_DETAIL, { productCode: product.productCode }));
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể tạo sản phẩm. Vui lòng thử lại.'));
    },
  });
}

// ─── Update Product ───────────────────────────────────────────────

export function useUpdateProduct(productCode: string | undefined) {
  const { message } = App.useApp();
  const navigate    = useNavigate();
  const qc          = useQueryClient();

  return useMutation({
    mutationFn: (req: UpdateProductRequest) =>
      productService.updateProduct(productCode!, req),
    onSuccess: (product) => {
      message.success(`Cập nhật sản phẩm "${product.productName}" thành công!`);
      qc.invalidateQueries({ queryKey: QUERY_KEYS.PRODUCTS });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.PRODUCT(productCode!) });
      navigate(buildPath(ROUTES.PRODUCT_DETAIL, { productCode: product.productCode }));
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể cập nhật sản phẩm. Vui lòng thử lại.'));
    },
  });
}

// ─── Toggle Product Status (disable / re-enable) ──────────────────

export function useToggleProductStatus(productCode: string | undefined) {
  const { message } = App.useApp();
  const qc          = useQueryClient();

  return useMutation({
    mutationFn: (isActive: boolean) =>
      productService.toggleProductStatus(productCode!, isActive),
    onSuccess: (product) => {
      const action = product.isActive ? 'kích hoạt' : 'vô hiệu hoá';
      message.success(`Đã ${action} sản phẩm "${product.productName}"`);
      qc.invalidateQueries({ queryKey: QUERY_KEYS.PRODUCTS });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.PRODUCT(productCode!) });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể cập nhật trạng thái sản phẩm.'));
    },
  });
}
