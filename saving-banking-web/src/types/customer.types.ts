// ── Customer ───────────────────────────────────────────────────────

export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';
export type Gender         = 'MALE' | 'FEMALE' | 'OTHER';
export type KycStatus      = 'PENDING' | 'VERIFIED' | 'REJECTED';
export type IdType         = 'NATIONAL_ID' | 'PASSPORT' | 'DRIVER_LICENSE' | 'MILITARY_ID';

export interface CustomerContact {
  contactId: string;
  phoneNumber: string | null;
  email:       string | null;
  address:     string | null;
  district:    string | null;
  city:        string | null;
  isPrimary:   boolean;
  createdAt:   string;
}

export interface Customer {
  cif:         string;
  fullName:    string;
  dateOfBirth: string | null;   // YYYY-MM-DD
  gender:      Gender | null;
  nationality: string | null;
  idNumber:    string | null;
  idType:      IdType | null;
  status:      CustomerStatus;
  kycStatus:   KycStatus | null;
  createdAt:   string;
  updatedAt:   string;
  contacts?:   CustomerContact[];
}

// ── Requests ──────────────────────────────────────────────────────

export interface CreateCustomerRequest {
  fullName:       string;
  dateOfBirth?:   string;   // YYYY-MM-DD
  gender?:        Gender;
  nationality?:   string;
  idType:         IdType;
  idNumber:       string;
  primaryContact: {
    phoneNumber?: string;
    email?:       string;
    address?:     string;
    district?:    string;
    city?:        string;
    isPrimary:    true;
  };
}

export interface UpdateCustomerRequest {
  fullName?:    string;
  dateOfBirth?: string;       // YYYY-MM-DD
  gender?:      Gender;
  nationality?: string;
  status?:      CustomerStatus;
}

export interface UpdateContactRequest {
  phoneNumber?: string;
  email?:       string;
  address?:     string;
  district?:    string;
  city?:        string;
}

export interface CreateContactRequest {
  phoneNumber?: string;
  email?:       string;
  address?:     string;
  district?:    string;
  city?:        string;
  isPrimary?:   boolean;
}
