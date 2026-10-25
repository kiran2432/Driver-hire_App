import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { homePathFor, useAuth } from '../auth/AuthContext';

export function Layout() {
  const { user, status, logout } = useAuth();
  const navigate = useNavigate();

  async function signOut() {
    await logout();
    navigate('/', { replace: true });
  }

  return (
    <>
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <header className="site-header">
        <div className="container bar">
          <Link to={user ? homePathFor(user.role) : '/'} className="brand">
            <span className="logo" aria-hidden="true" />
            Driver Hire
          </Link>
          <nav aria-label="Main">
            {user?.role === 'CUSTOMER' && (
              <>
                <NavLink to="/customer/book">Book a driver</NavLink>
                <NavLink to="/customer/trips">My trips</NavLink>
              </>
            )}
            {user?.role === 'DRIVER' && (
              <>
                <NavLink to="/driver" end>
                  Dashboard
                </NavLink>
                <NavLink to="/driver/profile">My profile</NavLink>
              </>
            )}
            {status !== 'loading' && !user && (
              <>
                <NavLink to="/login">Sign in</NavLink>
                <Link to="/register" className="btn btn-primary btn-sm">
                  Create account
                </Link>
              </>
            )}
            {user && (
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => void signOut()}>
                Sign out
              </button>
            )}
          </nav>
        </div>
      </header>
      <main id="main" className="container">
        <Outlet />
      </main>
      <footer className="site-footer container muted">© {new Date().getFullYear()} Driver Hire</footer>
    </>
  );
}
