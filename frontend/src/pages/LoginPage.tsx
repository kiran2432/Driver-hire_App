import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { homePathFor, useAuth } from '../auth/AuthContext';
import { Alert } from '../components/ui';
import { TextField } from '../components/Field';
import { errorMessage } from '../utils/format';

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={homePathFor(user.role)} replace />;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const u = await login(email, password);
      navigate(from ?? homePathFor(u.role), { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="narrow">
      <h1>Sign in</h1>
      <form onSubmit={(e) => void onSubmit(e)} className="card form" noValidate={false}>
        {error && <Alert>{error}</Alert>}
        <TextField label="Email" type="email" value={email} onChange={setEmail} required autoComplete="email" />
        <TextField label="Password" type="password" value={password} onChange={setPassword} required
          autoComplete="current-password" />
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="muted center">
        New here? <Link to="/register">Create an account</Link>
      </p>
    </section>
  );
}
