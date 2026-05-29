import api from '@/services/api';
import type { ApiResponse } from '@/types/api.types';
import type { CreateAuthUserRequest, CreateAuthUserResponse } from '@/types';

export const authAdminService = {
  /** Create a CUSTOMER login account linked to an existing CIF (TELLER/ADMIN only) */
  async createUser(request: CreateAuthUserRequest): Promise<CreateAuthUserResponse> {
    const { data } = await api.post<ApiResponse<CreateAuthUserResponse>>(
      '/api/v1/auth/admin/users',
      request,
    );
    return data.data!;
  },
};
