import api from '@/services/api';
import type { ApiResponse, PaginatedResponse, PaginationParams } from '@/types/api.types';
import type {
  SavingContract,
  OpenSavingRequest,
  OpenSavingResponse,
  CloseSavingRequest,
  CloseSavingResponse,
  ContractStatusHistory,
  MaturityInstruction,
  ContractStatus,
} from '@/types';

// ─── Query filters ────────────────────────────────────────────────

export interface ContractListFilter extends PaginationParams {
  cif?: string;
  status?: ContractStatus;
  productCode?: string;
  fromDate?: string;
  toDate?: string;
}

// ─── Contract Service ─────────────────────────────────────────────

export const contractService = {
  /** Paginated list of contracts */
  async listContracts(
    filters: ContractListFilter = {},
  ): Promise<PaginatedResponse<SavingContract>> {
    const params: Record<string, unknown> = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
      sort: filters.sort ?? 'openDate,desc',
    };
    if (filters.cif)         params.cif         = filters.cif;
    if (filters.status)      params.status      = filters.status;
    if (filters.productCode) params.productCode = filters.productCode;
    if (filters.fromDate)    params.fromDate    = filters.fromDate;
    if (filters.toDate)      params.toDate      = filters.toDate;

    const { data } = await api.get<ApiResponse<PaginatedResponse<SavingContract>>>(
      '/api/v1/contracts',
      { params },
    );
    return data.data ?? { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0, numberOfElements: 0, first: true, last: true, empty: true };
  },

  /** Get a single contract by contract number */
  async getContract(contractNo: string): Promise<SavingContract> {
    const { data } = await api.get<ApiResponse<SavingContract>>(
      `/api/v1/contracts/${contractNo}`,
    );
    return data.data!;
  },

  /** Get status history for a contract */
  async getStatusHistory(contractNo: string): Promise<ContractStatusHistory[]> {
    const { data } = await api.get<ApiResponse<ContractStatusHistory[]>>(
      `/api/v1/contracts/${contractNo}/status-history`,
    );
    return data.data ?? [];
  },

  /** Open a new saving contract */
  async openSaving(req: OpenSavingRequest): Promise<OpenSavingResponse> {
    const { data } = await api.post<ApiResponse<OpenSavingResponse>>(
      '/api/v1/contracts',
      req,
    );
    return data.data!;
  },

  /** Close a contract (maturity or early withdrawal) */
  async closeContract(contractNo: string, req: CloseSavingRequest): Promise<CloseSavingResponse> {
    const { data } = await api.post<ApiResponse<CloseSavingResponse>>(
      `/api/v1/contracts/${contractNo}/close`,
      req,
    );
    return data.data!;
  },

  /** Process maturity instruction for a matured contract */
  async processMaturity(contractNo: string, instruction: MaturityInstruction): Promise<SavingContract> {
    const { data } = await api.post<ApiResponse<SavingContract>>(
      `/api/v1/contracts/${contractNo}/process-maturity`,
      instruction,
    );
    return data.data!;
  },
};
