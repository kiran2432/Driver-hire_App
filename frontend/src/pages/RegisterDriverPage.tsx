import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { DriverProfileFields, emptyDriverForm, toDriverInput, type DriverFormValues } from '../components/DriverProfileFields';
import { TextField } from '../components/Field';
import { Alert } from '../components/ui';
import { errorMessage, fieldErrorsOf } from '../utils/format';

export function RegisterDriverPage() {
  const { registerDriver } = useAuth();
  const navigate = useNavigate();
  const [account, setAccount] = useState({ fullName: '', email: '', phone: '', password: '' });
  const [profile, setProfile] = useState<DriverFormValues>(emptyDriverForm);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  const setAcc = (key: keyof typeof account) => (value: string) => setAccount((p) => ({ ...p, [key]: value }));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setErrors({});
    try {
      await registerDriver({ ...account, profile: toDriverInput(profile) });
      navigate('/driver', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
      setErrors(fieldErrorsOf(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="narrow">
      <h1>Register as a driver</h1>
      <form onSubmit={(e) => void onSubmit(e)} className="card form">
        {error && <Alert>{error}</Alert>}
        <h2>Your account</h2>
        <TextField label="Full name" value={account.fullName} onChange={setAcc('fullName')} required maxLength={120}
          autoComplete="name" error={errors.fullName} />
        <TextField label="Email" type="email" value={account.email} onChange={setAcc('email')} required
          autoComplete="email" error={errors.email} />
        <TextField label="Mobile number" type="tel" value={account.phone} onChange={setAcc('phone')} required
          autoComplete="tel" hint="Shared with a customer only after you accept their trip" error={errors.phone} />
        <TextField label="Password" type="password" value={account.password} onChange={setAcc('password')} required
          autoComplete="new-password" hint="At least 8 characters with a letter and a number"
          error={errors.password} />

        <h2>Driver details</h2>
        <DriverProfileFields values={profile} errors={errors}
          onChange={(key, value) => setProfile((p) => ({ ...p, [key]: value }))} />

        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Creating profile…' : 'Create driver profile'}
        </button>
      </form>
      <p className="muted center">
        Looking to hire? <Link to="/register/customer">Register as a customer</Link>
      </p>
    </section>
  );
}
