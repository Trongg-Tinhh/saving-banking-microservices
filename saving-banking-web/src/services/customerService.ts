import api from '@/services/api';
import type { ApiResponse } from '@/types/api.types';
import type {
  Customer,
  CustomerContact,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  UpdateContactRequest,
  CreateContactRequest,
} from '@/types';

// ─── Customer Service ─────────────────────────────────────────────

export const customerService = {
  /** Create a new customer (staff only) */
  async createCustomer(request: CreateCustomerRequest): Promise<Customer> {
    const { data } = await api.post<ApiResponse<Customer>>(
      '/api/v1/customers',
      request,
    );
    return data.data!;
  },

  /** Get full customer profile (info + KYC status) by CIF */
  async getCustomer(cif: string): Promise<Customer> {
    const { data } = await api.get<ApiResponse<Customer>>(`/api/v1/customers/${cif}`);
    return data.data!;
  },

  /** Update basic customer info (fullName, DOB, gender, nationality, status) */
  async updateCustomer(cif: string, request: UpdateCustomerRequest): Promise<Customer> {
    const { data } = await api.put<ApiResponse<Customer>>(
      `/api/v1/customers/${cif}`,
      request,
    );
    return data.data!;
  },

  /** Get all contact records for a customer */
  async getContacts(cif: string): Promise<CustomerContact[]> {
    const { data } = await api.get<ApiResponse<CustomerContact[]>>(
      `/api/v1/customers/${cif}/contacts`,
    );
    return data.data ?? [];
  },

  /** Update an existing contact (phone, email, address) */
  async updateContact(
    cif: string,
    contactId: string,
    request: UpdateContactRequest,
  ): Promise<CustomerContact> {
    const { data } = await api.put<ApiResponse<CustomerContact>>(
      `/api/v1/customers/${cif}/contacts/${contactId}`,
      request,
    );
    return data.data!;
  },

  /** Add a new contact record for a customer */
  async addContact(cif: string, request: CreateContactRequest): Promise<CustomerContact> {
    const { data } = await api.post<ApiResponse<CustomerContact>>(
      `/api/v1/customers/${cif}/contacts`,
      request,
    );
    return data.data!;
  },
};
