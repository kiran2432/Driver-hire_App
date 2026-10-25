import { api } from './client';
import type {
  AuthResponse,
  CreateTripRequest,
  DriverProfileInput,
  DriverProfileResponse,
  DriverSummary,
  MeResponse,
  PageResponse,
  RegisterCustomerRequest,
  RegisterDriverRequest,
  Transmission,
  Trip,
  TripStatus,
} from './types';

export const authApi = {
  login: (email: string, password: string) =>
    api<AuthResponse>('/api/auth/login', { method: 'POST', body: { email, password }, auth: false }),
  registerDriver: (req: RegisterDriverRequest) =>
    api<AuthResponse>('/api/auth/register/driver', { method: 'POST', body: req, auth: false }),
  registerCustomer: (req: RegisterCustomerRequest) =>
    api<AuthResponse>('/api/auth/register/customer', { method: 'POST', body: req, auth: false }),
  logout: () => api<void>('/api/auth/logout', { method: 'POST', auth: false }),
};

export const meApi = {
  get: () => api<MeResponse>('/api/me'),
};

export interface DriverSearch {
  city?: string;
  transmission?: Transmission | '';
  minExperience?: number;
  page?: number;
}

export const driversApi = {
  search: (q: DriverSearch) =>
    api<PageResponse<DriverSummary>>('/api/drivers', {
      query: { city: q.city, transmission: q.transmission, minExperience: q.minExperience, page: q.page, size: 9 },
    }),
  me: () => api<DriverProfileResponse>('/api/drivers/me'),
  update: (fullName: string, phone: string, profile: DriverProfileInput) =>
    api<DriverProfileResponse>('/api/drivers/me', { method: 'PUT', body: { fullName, phone, profile } }),
  setAvailability: (available: boolean) =>
    api<DriverProfileResponse>('/api/drivers/me/availability', { method: 'PATCH', body: { available } }),
};

export const tripsApi = {
  create: (req: CreateTripRequest) => api<Trip>('/api/trips', { method: 'POST', body: req }),
  mine: (page = 0) => api<PageResponse<Trip>>('/api/trips/mine', { query: { page, size: 10 } }),
  open: (page = 0) => api<PageResponse<Trip>>('/api/trips/open', { query: { page, size: 10 } }),
  assigned: (status: TripStatus, page = 0) =>
    api<PageResponse<Trip>>('/api/trips/assigned', { query: { status, page, size: 10 } }),
  get: (id: number) => api<Trip>(`/api/trips/${id}`),
  hire: (id: number, driverId: number) =>
    api<Trip>(`/api/trips/${id}/hire`, { method: 'POST', body: { driverId } }),
  cancel: (id: number) => api<Trip>(`/api/trips/${id}/cancel`, { method: 'POST' }),
  accept: (id: number) => api<Trip>(`/api/trips/${id}/accept`, { method: 'POST' }),
  decline: (id: number) => api<Trip>(`/api/trips/${id}/decline`, { method: 'POST' }),
  complete: (id: number) => api<Trip>(`/api/trips/${id}/complete`, { method: 'POST' }),
};
