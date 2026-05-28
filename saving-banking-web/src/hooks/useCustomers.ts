import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { customerService } from '@/services/customerService';
import type { UpdateCustomerRequest, UpdateContactRequest, CreateContactRequest } from '@/types';

// ─── Query keys ──────────────────────────────────────────────────

export const CUSTOMER_KEYS = {
  customer:  (cif: string) => ['customer', cif] as const,
  contacts:  (cif: string) => ['customer', cif, 'contacts'] as const,
};

// ─── Get customer ─────────────────────────────────────────────────

export function useCustomer(cif: string | undefined) {
  return useQuery({
    queryKey:  CUSTOMER_KEYS.customer(cif ?? ''),
    queryFn:   () => customerService.getCustomer(cif!),
    enabled:   Boolean(cif),
    staleTime: 2 * 60_000,
  });
}

// ─── Get contacts ─────────────────────────────────────────────────

export function useCustomerContacts(cif: string | undefined) {
  return useQuery({
    queryKey:  CUSTOMER_KEYS.contacts(cif ?? ''),
    queryFn:   () => customerService.getContacts(cif!),
    enabled:   Boolean(cif),
    staleTime: 2 * 60_000,
  });
}

// ─── Update customer ──────────────────────────────────────────────

export function useUpdateCustomer(cif: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: UpdateCustomerRequest) => customerService.updateCustomer(cif, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CUSTOMER_KEYS.customer(cif) });
    },
  });
}

// ─── Update contact ───────────────────────────────────────────────

export function useUpdateContact(cif: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ contactId, req }: { contactId: string; req: UpdateContactRequest }) =>
      customerService.updateContact(cif, contactId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CUSTOMER_KEYS.contacts(cif) });
      // Also invalidate full customer in case contacts embedded
      queryClient.invalidateQueries({ queryKey: CUSTOMER_KEYS.customer(cif) });
    },
  });
}

// ─── Add contact ──────────────────────────────────────────────────

export function useAddContact(cif: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: CreateContactRequest) => customerService.addContact(cif, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CUSTOMER_KEYS.contacts(cif) });
    },
  });
}
