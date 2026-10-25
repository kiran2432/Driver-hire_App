import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { tripsApi } from '../../api/endpoints';
import { TripCard } from '../../components/TripCard';
import { Alert, Spinner } from '../../components/ui';
import { errorMessage } from '../../utils/format';
import { useLoad } from '../../utils/useLoad';

const POLL_MS = 15_000;

export function CustomerTripPage() {
  const id = Number(useParams().id);
  const { data: trip, error, loading, reload } = useLoad(() => tripsApi.get(id), [id]);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // While the trip is still in progress, check quietly for the driver's response.
  const live = trip && ['OPEN', 'REQUESTED', 'CONFIRMED'].includes(trip.status);
  useEffect(() => {
    if (!live) return;
    const timer = window.setInterval(() => reload(true), POLL_MS);
    return () => window.clearInterval(timer);
  }, [live, reload]);

  async function cancel() {
    if (!window.confirm('Cancel this trip?')) return;
    setBusy(true);
    setActionError(null);
    try {
      await tripsApi.cancel(id);
      reload();
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (loading && !trip) return <Spinner />;
  if (error && !trip) return <Alert>{error}</Alert>;
  if (!trip) return null;

  return (
    <section className="narrow">
      <p>
        <Link to="/customer/trips">← All trips</Link>
      </p>
      <h1>Trip #{trip.id}</h1>
      {actionError && <Alert>{actionError}</Alert>}

      {trip.status === 'REQUESTED' && (
        <Alert kind="info">
          Request sent to {trip.driver?.fullName}. You’ll see their phone number here once they accept.
        </Alert>
      )}
      {trip.status === 'CONFIRMED' && (
        <Alert kind="success">{trip.driver?.fullName} has accepted your trip. Contact details are below.</Alert>
      )}
      {trip.status === 'OPEN' && (
        <Alert kind="info">No driver yet. Choose one, or wait for a driver in {trip.pickupCity} to accept.</Alert>
      )}

      <TripCard trip={trip} viewer="customer">
        {trip.status === 'OPEN' && (
          <Link to={`/customer/trips/${trip.id}/drivers`} className="btn btn-primary">
            Find a driver
          </Link>
        )}
        {['OPEN', 'REQUESTED', 'CONFIRMED'].includes(trip.status) && (
          <button type="button" className="btn btn-danger" disabled={busy} onClick={() => void cancel()}>
            Cancel trip
          </button>
        )}
      </TripCard>

      {trip.driver?.bio && (
        <div className="card">
          <h3>About {trip.driver.fullName}</h3>
          <p>{trip.driver.bio}</p>
          {trip.driver.languages.length > 0 && <p className="muted">Speaks {trip.driver.languages.join(', ')}</p>}
        </div>
      )}
    </section>
  );
}
