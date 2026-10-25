import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { onSessionChange, refreshSession, setAccessToken } from '../api/client';
import { authApi } from '../api/endpoints';
import type { AuthResponse, RegisterCustomerRequest, RegisterDriverRequest, UserView } from '../api/types';

type Status = 'loading' | 'anonymous' | 'authenticated';

interface AuthContextValue {
  status: Status;
  user: UserView | null;
  login: (email: string, password: string) => Promise<UserView>;
  registerDriver: (req: RegisterDriverRequest) => Promise<UserView>;
  registerCustomer: (req: RegisterCustomerRequest) => Promise<UserView>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<Status>('loading');
  const [user, setUser] = useState<UserView | null>(null);

  const apply = useCallback((session: AuthResponse | null) => {
    setUser(session ? session.user : null);
    setStatus(session ? 'authenticated' : 'anonymous');
  }, []);

  // On first load, try to resume a session from the refresh cookie. The API client also reports silent
  // refreshes and forced sign-outs (expired / revoked session) through the same listener.
  useEffect(() => {
    onSessionChange(apply);
    void refreshSession().then(apply);
    return () => onSessionChange(null);
  }, [apply]);

  const finish = useCallback(
    (session: AuthResponse) => {
      setAccessToken(session.accessToken);
      apply(session);
      return session.user;
    },
    [apply],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      login: async (email, password) => finish(await authApi.login(email, password)),
      registerDriver: async (req) => finish(await authApi.registerDriver(req)),
      registerCustomer: async (req) => finish(await authApi.registerCustomer(req)),
      logout: async () => {
        try {
          await authApi.logout();
        } finally {
          setAccessToken(null);
          apply(null);
        }
      },
    }),
    [status, user, finish, apply],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}

export function homePathFor(role: UserView['role']): string {
  return role === 'DRIVER' ? '/driver' : '/customer/trips';
}
