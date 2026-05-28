import api from '@/services/api';
import type { ApiResponse } from '@/types/api.types';
import type {
  SavingProduct,
  SavingTerm,
  InterestRate,
  EarlyWithdrawalPolicy,
  CreateProductRequest,
  UpdateProductRequest,
  CreateTermRequest,
  UpdateTermRequest,
  CreateRateConfigRequest,
  UpsertEarlyWithdrawalPolicyRequest,
} from '@/types';

// ─── Product Service ────────────────────────────────────────────

export const productService = {
  /** List saving products. Pass activeOnly=true to fetch only active products. */
  async listProducts(activeOnly = false): Promise<SavingProduct[]> {
    const { data } = await api.get<ApiResponse<SavingProduct[]>>('/api/v1/products', {
      params: { activeOnly },
    });
    return data.data ?? [];
  },

  /** Get a single product by code */
  async getProduct(productCode: string): Promise<SavingProduct> {
    const { data } = await api.get<ApiResponse<SavingProduct>>(
      `/api/v1/products/${productCode}`,
    );
    return data.data!;
  },

  /** Get terms for a product.
   *  Pass activeOnly=false to include disabled terms (admin use only). */
  async getTerms(productCode: string, activeOnly = true): Promise<SavingTerm[]> {
    const { data } = await api.get<ApiResponse<Record<string, unknown>[]>>(
      `/api/v1/products/${productCode}/terms`,
      { params: { activeOnly } },
    );
    // Java DTO field is 'currentAnnualRate', TypeScript type expects 'annualRate'
    return (data.data ?? []).map((t) => ({
      termId:      t['termId']      as string,
      productCode: t['productCode'] as string,
      termMonths:  t['termMonths']  as number,
      termDays:    t['termDays']    as number,
      termLabel:   t['termLabel']   as string,
      isActive:    t['isActive']    as boolean,
      annualRate:  (t['annualRate'] ?? t['currentAnnualRate']) as number,
    }));
  },

  /** Add a new term to a product (ADMIN) */
  async createTerm(productCode: string, req: CreateTermRequest): Promise<SavingTerm> {
    const { data } = await api.post<ApiResponse<SavingTerm>>(
      `/api/v1/products/${productCode}/terms`,
      req,
    );
    return data.data!;
  },

  /** Update term label or toggle active (ADMIN) */
  async updateTerm(productCode: string, termId: string, req: UpdateTermRequest): Promise<SavingTerm> {
    const { data } = await api.put<ApiResponse<SavingTerm>>(
      `/api/v1/products/${productCode}/terms/${termId}`,
      req,
    );
    return data.data!;
  },

  /** Add a rate config for a term (ADMIN) */
  async addRateConfig(productCode: string, req: CreateRateConfigRequest): Promise<InterestRate> {
    const { data } = await api.post<ApiResponse<InterestRate>>(
      `/api/v1/products/${productCode}/rates`,
      req,
    );
    return data.data!;
  },

  /** Create or update the early withdrawal policy (ADMIN) */
  async upsertEarlyWithdrawalPolicy(
    productCode: string,
    req: UpsertEarlyWithdrawalPolicyRequest,
  ): Promise<EarlyWithdrawalPolicy> {
    const { data } = await api.post<ApiResponse<EarlyWithdrawalPolicy>>(
      `/api/v1/products/${productCode}/early-withdrawal`,
      req,
    );
    return data.data!;
  },

  /** Get interest rate history for a product */
  async getRates(productCode: string): Promise<InterestRate[]> {
    const { data } = await api.get<ApiResponse<InterestRate[]>>(
      `/api/v1/products/${productCode}/rates`,
    );
    return data.data ?? [];
  },

  /** Create a new saving product (ADMIN only) */
  async createProduct(req: CreateProductRequest): Promise<SavingProduct> {
    const { data } = await api.post<ApiResponse<SavingProduct>>('/api/v1/products', req);
    return data.data!;
  },

  /** Update product fields (ADMIN only) */
  async updateProduct(productCode: string, req: UpdateProductRequest): Promise<SavingProduct> {
    const { data } = await api.put<ApiResponse<SavingProduct>>(
      `/api/v1/products/${productCode}`,
      req,
    );
    return data.data!;
  },

  /** Enable or disable a product (ADMIN only)
   *  Existing contracts are NOT affected — they continue until maturity. */
  async toggleProductStatus(productCode: string, isActive: boolean): Promise<SavingProduct> {
    const { data } = await api.put<ApiResponse<SavingProduct>>(
      `/api/v1/products/${productCode}`,
      { isActive },
    );
    return data.data!;
  },

  /** Get early withdrawal policy */
  async getEarlyWithdrawalPolicy(productCode: string): Promise<EarlyWithdrawalPolicy | null> {
    try {
      const { data } = await api.get<ApiResponse<EarlyWithdrawalPolicy>>(
        `/api/v1/products/${productCode}/early-withdrawal`,
      );
      return data.data ?? null;
    } catch {
      return null;
    }
  },
};
