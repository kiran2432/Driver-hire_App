import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import type { Role } from '../api/types';
import { RequireRole } from './RequireRole';

const useAuth = vi.fn();
vi.mock('./AuthContext', () => ({
  useAuth: () => useAuth(),
  homePathFor: (role: Role) => (role === 'DRIVER' ? '/driver' : '/customer/trips'),
}));

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<p>login page</p>} />
        <Route path="/driver" element={<p>driver home</p>} />
        <Route element={<RequireRole role="CUSTOMER" />}>
          <Route path="/customer/trips" element={<p>customer trips</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('RequireRole', () => {
  it('shows a loading state while the session is being restored', () => {
    useAuth.mockReturnValue({ status: 'loading', user: null });
    renderAt('/customer/trips');
    expect(screen.getByRole('status')).toHaveTextContent('Loading');
  });

  it('sends signed-out visitors to the login page', () => {
    useAuth.mockReturnValue({ status: 'anonymous', user: null });
    renderAt('/customer/trips');
    expect(screen.getByText('login page')).toBeInTheDocument();
  });

  it('sends users with the wrong role to their own home', () => {
    useAuth.mockReturnValue({ status: 'authenticated', user: { role: 'DRIVER' } });
    renderAt('/customer/trips');
    expect(screen.getByText('driver home')).toBeInTheDocument();
  });

  it('renders the page for the right role', () => {
    useAuth.mockReturnValue({ status: 'authenticated', user: { role: 'CUSTOMER' } });
    renderAt('/customer/trips');
    expect(screen.getByText('customer trips')).toBeInTheDocument();
  });
});
