import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, api, onSessionChange, refreshSession, setAccessToken } from './client';
import type { AuthResponse } from './types';

const session: AuthResponse = {
  accessToken: 'new-token',
  tokenType: 'Bearer',
  expiresIn: 900,
  user: { id: 1, email: 'a@b.c', fullName: 'A', phone: '9999999999', role: 'CUSTOMER' },
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

describe('api client', () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock);
    setAccessToken('old-token');
    onSessionChange(null);
  });

  afterEach(() => {
    fetchMock.mockReset();
    vi.unstubAllGlobals();
  });

  it('sends the bearer token and JSON body', async () => {
    fetchMock.mockResolvedValueOnce(json({ ok: true }));
    await api('/api/things', { method: 'POST', body: { a: 1 } });

    const [url, init] = fetchMock.mock.calls[0]!;
    expect(url).toBe('/api/things');
    expect((init!.headers as Record<string, string>).Authorization).toBe('Bearer old-token');
    expect(init!.body).toBe('{"a":1}');
  });

  it('skips the Authorization header for public auth calls', async () => {
    fetchMock.mockResolvedValueOnce(json(session));
    await api('/api/auth/login', { method: 'POST', body: {}, auth: false });
    const [, init] = fetchMock.mock.calls[0]!;
    expect((init!.headers as Record<string, string>).Authorization).toBeUndefined();
  });

  it('builds the query string and drops empty values', async () => {
    fetchMock.mockResolvedValueOnce(json({}));
    await api('/api/drivers', { query: { city: 'Pune', transmission: '', minExperience: 0, page: undefined } });
    expect(fetchMock.mock.calls[0]![0]).toBe('/api/drivers?city=Pune&minExperience=0');
  });

  it('refreshes once on 401 and retries with the new token', async () => {
    const listener = vi.fn();
    onSessionChange(listener);
    fetchMock
      .mockResolvedValueOnce(json({ detail: 'expired' }, 401))
      .mockResolvedValueOnce(json(session)) // /api/auth/refresh
      .mockResolvedValueOnce(json({ hello: 'world' }));

    const result = await api<{ hello: string }>('/api/me');

    expect(result.hello).toBe('world');
    expect(fetchMock.mock.calls[1]![0]).toBe('/api/auth/refresh');
    const retryHeaders = fetchMock.mock.calls[2]![1]!.headers as Record<string, string>;
    expect(retryHeaders.Authorization).toBe('Bearer new-token');
    expect(listener).toHaveBeenCalledWith(session);
  });

  it('signs the user out when the refresh fails', async () => {
    const listener = vi.fn();
    onSessionChange(listener);
    fetchMock
      .mockResolvedValueOnce(json({ detail: 'expired' }, 401))
      .mockResolvedValueOnce(json({ detail: 'Session expired' }, 401));

    await expect(api('/api/me')).rejects.toMatchObject({ status: 401 });
    expect(listener).toHaveBeenCalledWith(null);
  });

  it('shares one refresh request between concurrent callers', async () => {
    fetchMock.mockResolvedValue(json(session));
    await Promise.all([refreshSession(), refreshSession(), refreshSession()]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('turns problem+json validation errors into ApiError with field messages', async () => {
    fetchMock.mockResolvedValueOnce(
      json({ detail: 'Validation failed', errors: { 'profile.city': 'must not be blank' } }, 400),
    );
    const err = await api('/api/auth/register/driver', { method: 'POST', body: {}, auth: false }).catch(
      (e: unknown) => e,
    );
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).fieldErrors['profile.city']).toBe('must not be blank');
    expect((err as ApiError).status).toBe(400);
  });

  it('gives a friendly message for rate limiting', async () => {
    fetchMock.mockResolvedValueOnce(new Response('{}', { status: 429 }));
    await expect(api('/api/auth/login', { method: 'POST', body: {}, auth: false })).rejects.toThrow(/too many/i);
  });

  it('returns undefined for 204 responses', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(api('/api/auth/logout', { method: 'POST', auth: false })).resolves.toBeUndefined();
  });
});
