import { create } from 'zustand';
import type { UserProfile, UserRole } from '@/types';
import { storage } from '@/utils/storage';

interface AuthState {
  // State
  accessToken: string | null;
  user: UserProfile | null;
  isAuthenticated: boolean;
  isInitializing: boolean;

  // Actions
  setAuth: (token: string, user: UserProfile) => void;
  setUser: (user: UserProfile) => void;
  clearAuth: () => void;
  setInitializing: (v: boolean) => void;

  // Helpers
  hasRole: (role: UserRole) => boolean;
  hasAnyRole: (...roles: UserRole[]) => boolean;
  isCustomer: () => boolean;
  isTeller: () => boolean;
  isAdmin: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  // ── Initial state ────────────────────────────────────────────
  accessToken: null,
  user: null,
  isAuthenticated: false,
  isInitializing: true,

  // ── Actions ──────────────────────────────────────────────────
  setAuth: (token, user) => {
    set({ accessToken: token, user, isAuthenticated: true });
  },

  setUser: (user) => {
    set({ user });
  },

  clearAuth: () => {
    storage.clearAll();
    set({ accessToken: null, user: null, isAuthenticated: false });
  },

  setInitializing: (v) => set({ isInitializing: v }),

  // ── Helpers ──────────────────────────────────────────────────
  hasRole: (role) => {
    const { user } = get();
    return user?.roles?.includes(role) ?? false;
  },

  hasAnyRole: (...roles) => {
    const { user } = get();
    return roles.some((r) => user?.roles?.includes(r)) ?? false;
  },

  isCustomer: () => get().hasRole('CUSTOMER'),
  isTeller:   () => get().hasRole('TELLER'),
  isAdmin:    () => get().hasRole('ADMIN'),
}));
