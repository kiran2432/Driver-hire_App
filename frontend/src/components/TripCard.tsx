import type { ReactNode } from 'react';
import type { Trip } from '../api/types';
import {
  CAR_TYPE_LABEL,
  TRANSMISSION_LABEL,
  TRIP_TYPE_LABEL,
  formatDateTime,
  formatMoney,
} from '../utils/format';
import { StatusBadge } from './ui';

interface TripCardProps {
  trip: Trip;
  /** Whose perspective: drivers see the customer, customers see the driver. */
  viewer: 'customer' | 'driver';
  children?: ReactNode;
}

export function TripCard({ trip, viewer, children }: TripCardProps) {
  return (
    <article className="card trip-card">
      <header className="card-header">
        <h3>
          {trip.pickupLocation} <span aria-label="to">→</span> {trip.destination}
        </h3>
        <StatusBadge status={trip.status} />
      </header>

      <dl className="details">
        <div>
          <dt>Pickup city</dt>
          <dd>{trip.pickupCity}</dd>
        </div>
        <div>
          <dt>Trip</dt>
          <dd>{TRIP_TYPE_LABEL[trip.tripType]}</dd>
        </div>
        <div>
          <dt>Starts</dt>
          <dd>{formatDateTime(trip.startAt)}</dd>
        </div>
        {trip.returnAt && (
          <div>
            <dt>Returns</dt>
            <dd>{formatDateTime(trip.returnAt)}</dd>
          </div>
        )}
        <div>
          <dt>Car</dt>
          <dd>
            {trip.car.make} {trip.car.model} · {CAR_TYPE_LABEL[trip.car.type]} ·{' '}
            {TRANSMISSION_LABEL[trip.car.transmission]}
          </dd>
        </div>
        {trip.notes && (
          <div>
            <dt>Notes</dt>
            <dd>{trip.notes}</dd>
          </div>
        )}
      </dl>

      {viewer === 'driver' && (
        <div className="party">
          <strong>Customer:</strong> {trip.customer.fullName}
          {trip.customer.phone ? (
            <>
              {' · '}
              <a href={`tel:${trip.customer.phone}`}>{trip.customer.phone}</a>
            </>
          ) : (
            <span className="muted"> · phone shared once you confirm</span>
          )}
        </div>
      )}

      {viewer === 'customer' && trip.driver && (
        <div className="party">
          <strong>Driver:</strong> {trip.driver.fullName} · {trip.driver.yearsOfExperience} yrs experience ·{' '}
          {formatMoney(trip.driver.dailyRate)}/day
          {trip.driver.phone ? (
            <>
              {' · '}
              <a href={`tel:${trip.driver.phone}`}>{trip.driver.phone}</a>
            </>
          ) : (
            <span className="muted"> · phone shared once the driver confirms</span>
          )}
        </div>
      )}

      {children && <footer className="card-actions">{children}</footer>}
    </article>
  );
}
