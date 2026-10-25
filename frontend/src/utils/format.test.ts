import { describe, expect, it } from 'vitest';
import { ApiError } from '../api/client';
import { errorMessage, fieldErrorsOf, formatMoney, localInputToIso, STATUS_LABEL } from './format';

describe('format helpers', () => {
  it('formats rupees without decimals', () => {
    expect(formatMoney(1200)).toBe('₹1,200');
    expect(formatMoney(1500.5)).toMatch(/^₹1,50[01]$/);
  });

  it('converts a datetime-local value to an ISO instant', () => {
    expect(localInputToIso('2030-01-15T10:30')).toBe(new Date('2030-01-15T10:30').toISOString());
  });

  it('labels every trip status', () => {
    expect(Object.keys(STATUS_LABEL)).toEqual(['OPEN', 'REQUESTED', 'CONFIRMED', 'COMPLETED', 'CANCELLED']);
  });

  it('shows API messages but hides unknown errors', () => {
    expect(errorMessage(new ApiError(409, 'Already taken'))).toBe('Already taken');
    expect(errorMessage(new Error('boom: internal detail'))).toBe('Something went wrong. Please try again.');
  });

  it('extracts field errors only from ApiError', () => {
    expect(fieldErrorsOf(new ApiError(400, 'x', { email: 'bad' }))).toEqual({ email: 'bad' });
    expect(fieldErrorsOf(new Error('x'))).toEqual({});
  });
});
