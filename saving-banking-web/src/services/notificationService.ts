import api from '@/services/api';
import type { ApiResponse, PaginatedResponse, PaginationParams } from '@/types/api.types';
import type { Notification } from '@/types';

export const notificationService = {
  /** List notifications for the current user (paginated) */
  async listNotifications(
    params: PaginationParams = {},
  ): Promise<PaginatedResponse<Notification>> {
    const { data } = await api.get<ApiResponse<PaginatedResponse<Notification>>>(
      '/api/v1/notifications',
      {
        params: {
          page: params.page ?? 0,
          size: params.size ?? 20,
          sort: params.sort ?? 'createdAt,desc',
        },
      },
    );
    return (
      data.data ?? {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        numberOfElements: 0,
        first: true,
        last: true,
        empty: true,
      }
    );
  },

  /** Mark a single notification as read */
  async markRead(notificationId: string): Promise<void> {
    await api.patch(`/api/v1/notifications/${notificationId}/read`);
  },

  /** Mark all notifications as read */
  async markAllRead(): Promise<void> {
    await api.patch('/api/v1/notifications/read-all');
  },
};
