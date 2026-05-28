import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { App } from 'antd';
import { notificationService } from '@/services/notificationService';
import { getApiErrorMessage } from '@/services/api';
import { QUERY_KEYS } from '@/constants/queryKeys';

export function useNotifications(page = 0) {
  return useQuery({
    queryKey: QUERY_KEYS.NOTIFICATIONS(page),
    queryFn: () => notificationService.listNotifications({ page, size: 20 }),
    staleTime: 30_000,
  });
}

export function useMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (notificationId: string) => notificationService.markRead(notificationId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATIONS() });
    },
  });
}

export function useMarkAllRead() {
  const { message } = App.useApp();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => notificationService.markAllRead(),
    onSuccess: () => {
      message.success('Đã đánh dấu tất cả là đã đọc');
      qc.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATIONS() });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể cập nhật. Vui lòng thử lại.'));
    },
  });
}
