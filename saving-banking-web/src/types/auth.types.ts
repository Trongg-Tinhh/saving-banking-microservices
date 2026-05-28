export type UserRole = 'CUSTOMER' | 'TELLER' | 'ADMIN' | 'MANAGER' | 'SYSTEM';

export interface LoginRequest {
  username: string;
  password: string;
  clientIp?: string;
  deviceInfo?: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: 'Bearer';
  expiresIn: number;      // seconds
  userId: string;
  username: string;
  cif: string | null;
  roles: UserRole[];
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface UserProfile {
  userId: string;
  username: string;
  cif: string | null;
  fullName?: string;
  status?: string;
  roles: UserRole[];
  lastLoginAt: string | null;
}
