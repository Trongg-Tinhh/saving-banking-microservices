import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { App } from 'antd';
import { useNavigate } from 'react-router-dom';
import { contractService, type ContractListFilter } from '@/services/contractService';
import { getApiErrorMessage } from '@/services/api';
import { QUERY_KEYS } from '@/constants/queryKeys';
import { buildPath, ROUTES } from '@/constants/routes';
import type {
  OpenSavingRequest,
  CloseSavingRequest,
  MaturityInstruction,
} from '@/types';

// ─── List Contracts ───────────────────────────────────────────────

export function useContracts(filters: ContractListFilter = {}) {
  return useQuery({
    queryKey: QUERY_KEYS.CONTRACTS(filters),
    queryFn: () => contractService.listContracts(filters),
    staleTime: 30_000, // 30 sec — contract list can change frequently
  });
}

// ─── Single Contract ──────────────────────────────────────────────

export function useContract(contractNo: string | undefined) {
  return useQuery({
    queryKey: QUERY_KEYS.CONTRACT(contractNo ?? ''),
    queryFn: () => contractService.getContract(contractNo!),
    enabled: Boolean(contractNo),
    staleTime: 30_000,
  });
}

// ─── Status History ───────────────────────────────────────────────

export function useContractHistory(contractNo: string | undefined) {
  return useQuery({
    queryKey: ['contracts', contractNo, 'history'],
    queryFn: () => contractService.getStatusHistory(contractNo!),
    enabled: Boolean(contractNo),
    staleTime: 60_000,
  });
}

// ─── Open Saving ──────────────────────────────────────────────────

export function useOpenSaving() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const qc = useQueryClient();

  return useMutation({
    mutationFn: (req: OpenSavingRequest) => contractService.openSaving(req),
    onSuccess: (res) => {
      message.success(`Mở sổ thành công! Số hợp đồng: ${res.contractNo}`);
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CONTRACTS() });
      navigate(buildPath(ROUTES.CONTRACT_DETAIL, { contractNo: res.contractNo }));
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể mở sổ tiết kiệm. Vui lòng kiểm tra lại thông tin.'));
    },
  });
}

// ─── Close Contract ───────────────────────────────────────────────

export function useCloseSaving(contractNo: string | undefined) {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const qc = useQueryClient();

  return useMutation({
    mutationFn: (req: CloseSavingRequest) =>
      contractService.closeContract(contractNo!, req),
    onSuccess: (res) => {
      const type = res.earlyWithdrawal ? 'tất toán trước hạn' : 'tất toán';
      message.success(`${type} thành công! Tổng chi trả: ${res.totalPayout.toLocaleString('vi-VN')} ₫`);
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CONTRACTS() });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CONTRACT(contractNo!) });
      navigate(ROUTES.CONTRACTS);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể tất toán hợp đồng. Vui lòng thử lại.'));
    },
  });
}

// ─── Process Maturity ─────────────────────────────────────────────

export function useProcessMaturity(contractNo: string | undefined) {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const qc = useQueryClient();

  return useMutation({
    mutationFn: (instruction: MaturityInstruction) =>
      contractService.processMaturity(contractNo!, instruction),
    onSuccess: () => {
      message.success('Xử lý đáo hạn thành công!');
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CONTRACTS() });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CONTRACT(contractNo!) });
      navigate(contractNo
        ? buildPath(ROUTES.CONTRACT_DETAIL, { contractNo })
        : ROUTES.CONTRACTS
      );
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error, 'Không thể xử lý đáo hạn. Vui lòng thử lại.'));
    },
  });
}
