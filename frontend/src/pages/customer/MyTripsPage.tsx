import { useState } from 'react';
import { Link } from 'react-router-dom';
import { tripsApi } from '../../api/endpoints';
import { TripCard } from '../../components/TripCard';
import { Alert, EmptyState, Pagination, Spinner } from '../../components/ui';
import { useLoad } from '../../utils/useLoad';

export function MyTripsPage() {
  const [page, setPage] = useState(0);
  const { data, error, loading } = useLoad(() => tripsApi.mine(page), [page]);

  return (
    <section>
      <div className="page-head">
        <h1>My trips</h1>
        <Link to="/customer/book" className="btn btn-primary">
          Book a driver
        </Link>
      </div>

      {error && <Alert>{error}</Alert>}
      {loading && !data && <Spinner />}

      {data && data.content.length === 0 && (
        <EmptyState title="No trips yet">
          <p className="muted">Post your first trip and pick a driver.</p>
          <Link to="/customer/book" className="btn btn-primary">
            Book a driver
          </Link>
        </EmptyState>
      )}

      <div className="stack">
        {data?.content.map((trip) => (
          <TripCard key={trip.id} trip={trip} viewer="customer">
            <Link to={`/customer/trips/${trip.id}`} className="btn btn-secondary btn-sm">
              View details
            </Link>
            {trip.status === 'OPEN' && (
              <Link to={`/customer/trips/${trip.id}/drivers`} className="btn btn-primary btn-sm">
                Find a driver
              </Link>
            )}
          </TripCard>
        ))}
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} onPage={setPage} />}
    </section>
  );
}
