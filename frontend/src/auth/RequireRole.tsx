import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { Role } from '../api/types';
import { homePathFor, useAuth } from './AuthContext';

/** Route guard: signed-out users go to /login, users with the wrong role go to their own home. */
export function RequireRole({ role }: { role: Role }) {
  const { status, user } = useAuth();
  const location = useLocation();

  if (status === 'loading') return <p className="center muted" role="status">Loading…</p>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (user.role !== role) return <Navigate to={homePathFor(user.role)} replace />;
  return <Outlet />;
}
