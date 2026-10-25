import type { AuthResponse } from './types';

/**
 * Thin fetch wrapper.
 *  - The short-lived access token lives only in memory (never localStorage), so page scripts can't steal a
 *    long-lived credential. The long-lived refresh token is an HttpOnly cookie the browser sends by itself.
 *  - On a 401 the call is retried once after a silent refresh. Concurrent refreshes share one request,
 *    because the server rotates refresh tokens and a second concurrent use would look like token theft.
 */

export class ApiError extends Error {
  readonly status: number;
  /** Field -> message, from the server's validation errors (keys like "email" or "profile.city"). */
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

let accessToken: string | null = null;
let sessionListener: ((session: AuthResponse | null) => void) | null = null;
let inflightRefresh: Promise<AuthResponse | null> | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

/** AuthProvider registers here to learn about silent refreshes and forced sign-outs. */
export function onSessionChange(listener: ((session: AuthResponse | null) => void) | null): void {
  sessionListener = listener;
}

async function readError(res: Response): Promise<ApiError> {
  let message = `Request failed (${res.status})`;
  let fieldErrors: Record<string, string> = {};
  try {
    const body = (await res.json()) as { detail?: string; errors?: Record<string, string> };
    if (body.detail) message = body.detail;
    if (body.errors) fieldErrors = body.errors;
  } catch {
    // Non-JSON error body (e.g. a proxy error page): keep the generic message.
  }
  if (res.status === 429) message = 'Too many attempts. Please wait a minute and try again.';
  return new ApiError(res.status, message, fieldErrors);
}

/** Exchanges the refresh cookie for a new access token. Resolves null when there is no valid session. */
export function refreshSession(): Promise<AuthResponse | null> {
  if (!inflightRefresh) {
    inflightRefresh = (async () => {
      try {
        const res = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'same-origin' });
        if (!res.ok) {
          accessToken = null;
          return null;
        }
        const session = (await res.json()) as AuthResponse;
        accessToken = session.accessToken;
        return session;
      } catch {
        return null;
      } finally {
        inflightRefresh = null;
      }
    })();
  }
  return inflightRefresh;
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined | null>;
  /** false for the public /api/auth endpoints. */
  auth?: boolean;
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  if (!query) return path;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') params.set(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${path}?${qs}` : path;
}

async function send(path: string, opts: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
  if (opts.auth !== false && accessToken) headers.Authorization = `Bearer ${accessToken}`;
  return fetch(buildUrl(path, opts.query), {
    method: opts.method ?? 'GET',
    headers,
    body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
    credentials: 'same-origin',
  });
}

export async function api<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  let res = await send(path, opts);

  if (res.status === 401 && opts.auth !== false) {
    const session = await refreshSession();
    if (session) {
      sessionListener?.(session);
      res = await send(path, opts);
    } else {
      sessionListener?.(null);
    }
  }

  if (!res.ok) throw await readError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}
