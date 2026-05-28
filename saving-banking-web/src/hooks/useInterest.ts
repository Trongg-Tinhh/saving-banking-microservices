import { useMutation } from '@tanstack/react-query';
import { App } from 'antd';
import { interestService } from '@/services/interestService';
import { getApiErrorMessage } from '@/services/api';
import type { InterestSimulationRequest, InterestSimulationResponse } from '@/types';

export interface SimulatePayload extends InterestSimulationRequest {
  startDate: string; // ISO date, e.g. "2026-05-27"
}

// ─── useSimulate ──────────────────────────────────────────────────
/**
 * Mutation that calls both FastAPI /calculate + /project
 * and returns the merged InterestSimulationResponse.
 */
export function useSimulate() {
  const { message } = App.useApp();

  return useMutation<InterestSimulationResponse, Error, SimulatePayload>({
    mutationFn: ({ startDate, ...req }) =>
      interestService.simulate(req, startDate),
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể tính toán lãi suất. Vui lòng thử lại.'));
    },
  });
}
