// ─── Generic API Response Wrapper ──────────────────────────────
// Matches backend format: { success, message, data, error, timestamp, correlationId }

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  error?: ApiError | null;
  timestamp: string;
  correlationId: string;
}

export interface ApiError {
  code: string;
  details: string;
}

// ─── Paginated Response ─────────────────────────────────────────
// Spring Page<T> wrapper

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;       // current page (0-indexed)
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// ─── Common Query Params ────────────────────────────────────────

export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}
