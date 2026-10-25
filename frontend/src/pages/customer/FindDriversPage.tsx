import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { driversApi, tripsApi } from '../../api/endpoints';
import type { Transmission } from '../../api/types';
import { DriverCard } from '../../components/DriverCard';
import { SelectField, TextField } from '../../components/Field';
import { TripCard } from '../../components/TripCard';
import { Alert, EmptyState, Pagination, Spinner } from '../../components/ui';
import { errorMessage, formatMoney } from '../../utils/format';
import { useLoad } from '../../utils/useLoad';

interface Filters {
  city: string;
  transmission: Transmission | '';
  minExperience: string;
}

export function FindDriversPage() {
  const tripId = Number(useParams().id);
  const navigate = useNavigate();
  const trip = useLoad(() => tripsApi.get(tripId), [tripId]);

  // `draft` is what the inputs show; `applied` is what the last search used.
  const [draft, setDraft] = useState<Filters | null>(null);
  const [applied, setApplied] = useState<Filters | null>(null);
  const [page, setPage] = useState(0);
  const [hireError, setHireError] = useState<string | null>(null);
  const [hiring, setHiring] = useState<number | null>(null);

  // Start with filters that match the trip: its pickup city and the car's gearbox.
  useEffect(() => {
    if (trip.data && !applied) {
      const initial: Filters = {
        city: trip.data.pickupCity,
        transmission: trip.data.car.transmission,
        minExperience: '',
      };
      setDraft(initial);
      setApplied(initial);
    }
  }, [trip.data, applied]);

  const drivers = useLoad(
    () =>
      applied
        ? driversApi.search({
            city: applied.city,
            transmission: applied.transmission,
            minExperience: applied.minExperience ? Number(applied.minExperience) : 0,
            page,
          })
        : Promise.resolve(null),
    [applied, page],
  );

  function onSearch(e: FormEvent) {
    e.preventDefault();
    setPage(0);
    setApplied(draft);
  }

  async function hire(driverId: number) {
    setHiring(driverId);
    setHireError(null);
    try {
      await tripsApi.hire(tripId, driverId);
      navigate(`/customer/trips/${tripId}`);
    } catch (err) {
      setHireError(errorMessage(err));
    } finally {
      setHiring(null);
    }
  }

  if (trip.loading && !trip.data) return <Spinner />;
  if (trip.error || !trip.data) return <Alert>{trip.error ?? 'Trip not found'}</Alert>;

  if (trip.data.status !== 'OPEN') {
    return (
      <section>
        <Alert kind="info">This trip already has a driver or is closed.</Alert>
        <Link to={`/customer/trips/${tripId}`} className="btn btn-primary">
          View trip
        </Link>
      </section>
    );
  }

  return (
    <section>
      <div className="page-head">
        <h1>Choose a driver</h1>
        <Link to={`/customer/trips/${tripId}`} className="btn btn-ghost">
          Skip for now
        </Link>
      </div>
      <TripCard trip={trip.data} viewer="customer" />
      <p className="muted">
        Not ready to pick? Leave the trip open: drivers in {trip.data.pickupCity} can also accept it.
      </p>

      {draft && (
        <form onSubmit={onSearch} className="card filters">
          <TextField label="City" value={draft.city} onChange={(v) => setDraft({ ...draft, city: v })} />
          <SelectField label="Gearbox" value={draft.transmission}
            onChange={(v) => setDraft({ ...draft, transmission: v })} placeholder="Any"
            options={[
              { value: 'MANUAL', label: 'Manual' },
              { value: 'AUTOMATIC', label: 'Automatic' },
            ]} />
          <TextField label="Min. years of experience" type="number" min={0} max={60} value={draft.minExperience}
            onChange={(v) => setDraft({ ...draft, minExperience: v })} />
          <button type="submit" className="btn btn-primary">
            Search
          </button>
        </form>
      )}

      {hireError && <Alert>{hireError}</Alert>}
      {drivers.error && <Alert>{drivers.error}</Alert>}
      {drivers.loading && !drivers.data && <Spinner label="Finding drivers…" />}

      {drivers.data && drivers.data.content.length === 0 && (
        <EmptyState title="No drivers match yet">
          <p className="muted">
            Try another city or remove filters. You can also leave the trip open so drivers can find you.
          </p>
        </EmptyState>
      )}

      <div className="grid cards">
        {drivers.data?.content.map((d) => (
          <DriverCard key={d.id} driver={d}>
            <button type="button" className="btn btn-primary" disabled={hiring !== null}
              onClick={() => void hire(d.id)}>
              {hiring === d.id ? 'Sending…' : `Hire for ${formatMoney(d.dailyRate)}/day`}
            </button>
          </DriverCard>
        ))}
      </div>
      {drivers.data && <Pagination page={drivers.data.page} totalPages={drivers.data.totalPages} onPage={setPage} />}
    </section>
  );
}
