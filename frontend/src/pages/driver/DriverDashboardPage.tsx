import { useState } from 'react';
import { Link } from 'react-router-dom';
import { driversApi, tripsApi } from '../../api/endpoints';
import type { Trip, TripStatus } from '../../api/types';
import { TripCard } from '../../components/TripCard';
import { Alert, EmptyState, Pagination, Spinner } from '../../components/ui';
import { errorMessage } from '../../utils/format';
import { useLoad } from '../../utils/useLoad';

type TabKey = 'requests' | 'confirmed' | 'open' | 'completed' | 'cancelled';

const TABS: { key: TabKey; label: string; status: TripStatus | null }[] = [
  { key: 'requests', label: 'Requests for you', status: 'REQUESTED' },
  { key: 'confirmed', label: 'Confirmed', status: 'CONFIRMED' },
  { key: 'open', label: 'Open trips nearby', status: null },
  { key: 'completed', label: 'Completed', status: 'COMPLETED' },
  { key: 'cancelled', label: 'Cancelled', status: 'CANCELLED' },
];

const EMPTY_TEXT: Record<TabKey, string> = {
  requests: 'No customers have requested you right now.',
  confirmed: 'No confirmed trips.',
  open: 'No open trips in your city right now. Check back soon.',
  completed: 'No completed trips yet.',
  cancelled: 'Nothing cancelled.',
};

export function DriverDashboardPage() {
  const [tab, setTab] = useState<TabKey>('requests');
  const [page, setPage] = useState(0);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const profile = useLoad(() => driversApi.me(), []);
  const active = TABS.find((t) => t.key === tab)!;
  const trips = useLoad(
    () => (active.status ? tripsApi.assigned(active.status, page) : tripsApi.open(page)),
    [tab, page],
  );

  function selectTab(key: TabKey) {
    setTab(key);
    setPage(0);
    setActionError(null);
  }

  async function act(trip: Trip, action: () => Promise<unknown>) {
    setBusyId(trip.id);
    setActionError(null);
    try {
      await action();
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setBusyId(null);
      trips.reload();
    }
  }

  async function toggleAvailability(available: boolean) {
    try {
      await driversApi.setAvailability(available);
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      profile.reload(true);
    }
  }

  return (
    <section>
      <div className="page-head">
        <div>
          <h1>Driver dashboard</h1>
          {profile.data && (
            <p className="muted">
              {profile.data.fullName} · {profile.data.city}
            </p>
          )}
        </div>
        {profile.data && (
          <label className="switch">
            <input type="checkbox" checked={profile.data.available}
              onChange={(e) => void toggleAvailability(e.target.checked)} />
            <span>{profile.data.available ? 'Available for hire' : 'Not available'}</span>
          </label>
        )}
      </div>

      {profile.data && !profile.data.available && (
        <Alert kind="info">
          You’re hidden from customer searches. You can still accept open trips and handle existing requests.{' '}
          <Link to="/driver/profile">Edit profile</Link>
        </Alert>
      )}

      <div className="tabs" role="tablist" aria-label="Trips">
        {TABS.map((t) => (
          <button key={t.key} type="button" role="tab" aria-selected={tab === t.key}
            className={tab === t.key ? 'tab active' : 'tab'} onClick={() => selectTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      {actionError && <Alert>{actionError}</Alert>}
      {trips.error && <Alert>{trips.error}</Alert>}
      {trips.loading && !trips.data && <Spinner />}
      {trips.data && trips.data.content.length === 0 && <EmptyState title={EMPTY_TEXT[tab]} />}

      <div className="stack">
        {trips.data?.content.map((trip) => {
          const busy = busyId === trip.id;
          return (
            <TripCard key={trip.id} trip={trip} viewer="driver">
              {trip.status === 'REQUESTED' && (
                <>
                  <button type="button" className="btn btn-primary" disabled={busy}
                    onClick={() => void act(trip, () => tripsApi.accept(trip.id))}>
                    Accept
                  </button>
                  <button type="button" className="btn btn-secondary" disabled={busy}
                    onClick={() => void act(trip, () => tripsApi.decline(trip.id))}>
                    Decline
                  </button>
                </>
              )}
              {trip.status === 'CONFIRMED' && (
                <>
                  <button type="button" className="btn btn-primary" disabled={busy}
                    onClick={() => void act(trip, () => tripsApi.complete(trip.id))}>
                    Mark completed
                  </button>
                  <button type="button" className="btn btn-danger" disabled={busy}
                    onClick={() => {
                      if (window.confirm('Drop this trip? It goes back to the open list for the customer.')) {
                        void act(trip, () => tripsApi.decline(trip.id));
                      }
                    }}>
                    Drop trip
                  </button>
                </>
              )}
              {trip.status === 'OPEN' && (
                <button type="button" className="btn btn-primary" disabled={busy}
                  onClick={() => void act(trip, () => tripsApi.accept(trip.id))}>
                  Accept this trip
                </button>
              )}
            </TripCard>
          );
        })}
      </div>
      {trips.data && <Pagination page={trips.data.page} totalPages={trips.data.totalPages} onPage={setPage} />}
    </section>
  );
}
