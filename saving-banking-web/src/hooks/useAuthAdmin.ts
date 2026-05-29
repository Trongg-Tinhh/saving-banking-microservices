import { useMutation } from '@tanstack/react-query';
import { authAdminService } from '@/services/authAdminService';
import type { CreateAuthUserRequest } from '@/types';

export function useCreateAuthUser() {
  return useMutation({
    mutationFn: (req: CreateAuthUserRequest) => authAdminService.createUser(req),
  });
}
