import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { TextField } from '../components/Field';
import { Alert } from '../components/ui';
import { errorMessage, fieldErrorsOf } from '../utils/format';

export function RegisterCustomerPage() {
  const { registerCustomer } = useAuth();
  const navigate = useNavigate();
  const [f, setF] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    address: '',
    city: '',
    state: '',
  });
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  const set = (key: keyof typeof f) => (value: string) => setF((p) => ({ ...p, [key]: value }));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setErrors({});
    try {
      await registerCustomer({
        email: f.email,
        password: f.password,
        fullName: f.fullName,
        phone: f.phone,
        profile: { address: f.address, city: f.city, state: f.state },
      });
      navigate('/customer/book', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
      setErrors(fieldErrorsOf(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="narrow">
      <h1>Register as a customer</h1>
      <form onSubmit={(e) => void onSubmit(e)} className="card form">
        {error && <Alert>{error}</Alert>}
        <TextField label="Full name" value={f.fullName} onChange={set('fullName')} required maxLength={120}
          autoComplete="name" error={errors.fullName} />
        <TextField label="Email" type="email" value={f.email} onChange={set('email')} required
          autoComplete="email" error={errors.email} />
        <TextField label="Mobile number" type="tel" value={f.phone} onChange={set('phone')} required
          autoComplete="tel" hint="10–15 digits, e.g. 9876543210" error={errors.phone} />
        <TextField label="Password" type="password" value={f.password} onChange={set('password')} required
          autoComplete="new-password" hint="At least 8 characters with a letter and a number"
          error={errors.password} />
        <TextField label="Address" value={f.address} onChange={set('address')} required maxLength={255}
          autoComplete="street-address" error={errors['profile.address']} />
        <div className="row">
          <TextField label="City" value={f.city} onChange={set('city')} required maxLength={100}
            autoComplete="address-level2" error={errors['profile.city']} />
          <TextField label="State" value={f.state} onChange={set('state')} required maxLength={100}
            autoComplete="address-level1" error={errors['profile.state']} />
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="muted center">
        Are you a driver? <Link to="/register/driver">Register as a driver</Link>
      </p>
    </section>
  );
}
