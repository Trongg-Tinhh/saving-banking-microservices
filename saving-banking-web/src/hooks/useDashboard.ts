import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '@/services/dashboardService';
import { QUERY_KEYS } from '@/constants/queryKeys';

export function useDashboard() {
  return useQuery({
    queryKey: QUERY_KEYS.DASHBOARD,
    queryFn: () => dashboardService.getSummary(),
    staleTime: 60_000, // 1 min
    retry: 1,
  });
}
