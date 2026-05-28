// Khớp với RecordTransactionRequest và seed data của backend
export type TransactionType =
  | 'DEBIT'
  | 'CREDIT'
  | 'INTEREST'
  | 'OPEN_SAVING'
  | 'CLOSE_SAVING'
  | 'INTEREST_PAYMENT';

export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'SUCCESS' | 'FAILED';
export type CbsSyncStatus = 'PENDING' | 'SYNCED' | 'FAILED';

export interface Transaction {
  transactionId:  string;
  transactionRef: string;
  accountNo:      string;
  cif:            string;
  transactionType: TransactionType;
  amount:         number;
  currency:       string;
  description?:   string | null;
  contractNo?:    string | null;
  cbsReference?:  string | null;
  status:         TransactionStatus;
  cbsSyncStatus?: CbsSyncStatus;
  correlationId?: string;
  createdAt:      string;
  cbsSyncedAt?:   string | null;
}

export interface TransactionFilter {
  contractNo?: string;
  cif?:        string;
  txType?:     TransactionType;
  status?:     TransactionStatus;
  fromDate?:   string;
  toDate?:     string;
}
