import { useEffect, useState, type FormEvent } from 'react';
import { driversApi } from '../../api/endpoints';
import {
  DriverProfileFields,
  driverFormFrom,
  emptyDriverForm,
  toDriverInput,
  type DriverFormValues,
} from '../../components/DriverProfileFields';
import { TextField } from '../../components/Field';
import { Alert, Spinner } from '../../components/ui';
import { errorMessage, fieldErrorsOf } from '../../utils/format';
import { useLoad } from '../../utils/useLoad';

export function DriverProfilePage() {
  const { data, loading, error: loadError } = useLoad(() => driversApi.me(), []);
  const [account, setAccount] = useState({ fullName: '', phone: '' });
  const [profile, setProfile] = useState<DriverFormValues>(emptyDriverForm);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (data) {
      setAccount({ fullName: data.fullName, phone: data.phone });
      setProfile(driverFormFrom(data));
    }
  }, [data]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setErrors({});
    setSaved(false);
    try {
      await driversApi.update(account.fullName, account.phone, toDriverInput(profile));
      setSaved(true);
    } catch (err) {
      setError(errorMessage(err));
      setErrors(fieldErrorsOf(err));
    } finally {
      setBusy(false);
    }
  }

  if (loading && !data) return <Spinner />;
  if (loadError && !data) return <Alert>{loadError}</Alert>;

  return (
    <section className="narrow">
      <h1>My profile</h1>
      <form onSubmit={(e) => void onSubmit(e)} className="card form">
        {error && <Alert>{error}</Alert>}
        {saved && <Alert kind="success">Profile saved.</Alert>}
        <TextField label="Full name" value={account.fullName} maxLength={120} required autoComplete="name"
          onChange={(v) => setAccount((p) => ({ ...p, fullName: v }))} error={errors.fullName} />
        <TextField label="Mobile number" type="tel" value={account.phone} required autoComplete="tel"
          onChange={(v) => setAccount((p) => ({ ...p, phone: v }))} error={errors.phone} />
        <p className="muted">Email: {data?.email}</p>
        <DriverProfileFields values={profile} errors={errors}
          onChange={(key, value) => setProfile((p) => ({ ...p, [key]: value }))} />
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Saving…' : 'Save changes'}
        </button>
      </form>
    </section>
  );
}
