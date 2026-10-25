import { ApiError } from '../api/client';
import type { CarType, Transmission, TripStatus, TripType } from '../api/types';

const moneyFormat = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

export function formatMoney(amount: number): string {
  return moneyFormat.format(amount);
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

/** Converts a <input type="datetime-local"> value (local time, no zone) to an ISO instant for the API. */
export function localInputToIso(value: string): string {
  return new Date(value).toISOString();
}

/** The value for a datetime-local input's "min" attribute: now, in local time, to the minute. */
export function nowForDatetimeLocal(): string {
  const d = new Date();
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

export const STATUS_LABEL: Record<TripStatus, string> = {
  OPEN: 'Open',
  REQUESTED: 'Awaiting driver',
  CONFIRMED: 'Confirmed',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

export const TRIP_TYPE_LABEL: Record<TripType, string> = {
  ONE_WAY: 'One way',
  ROUND_TRIP: 'Round trip',
};

export const CAR_TYPE_LABEL: Record<CarType, string> = {
  HATCHBACK: 'Hatchback',
  SEDAN: 'Sedan',
  SUV: 'SUV',
  MUV: 'MUV / minivan',
  LUXURY: 'Luxury',
};

export const TRANSMISSION_LABEL: Record<Transmission, string> = {
  MANUAL: 'Manual',
  AUTOMATIC: 'Automatic',
};

/** Human-readable message for any thrown value. */
export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  return 'Something went wrong. Please try again.';
}

export function fieldErrorsOf(e: unknown): Record<string, string> {
  return e instanceof ApiError ? e.fieldErrors : {};
}
