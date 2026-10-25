// Mirrors the backend DTOs (see backend/src/main/java/com/driverhire/**).

export type Role = 'DRIVER' | 'CUSTOMER';
export type TripStatus = 'OPEN' | 'REQUESTED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
export type TripType = 'ONE_WAY' | 'ROUND_TRIP';
export type CarType = 'HATCHBACK' | 'SEDAN' | 'SUV' | 'MUV' | 'LUXURY';
export type Transmission = 'MANUAL' | 'AUTOMATIC';

export interface UserView {
  id: number;
  email: string;
  fullName: string;
  phone: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserView;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DriverProfileInput {
  address: string;
  city: string;
  state: string;
  yearsOfExperience: number;
  licenseNumber: string;
  licenseExpiry: string; // yyyy-mm-dd
  drivesManual: boolean;
  drivesAutomatic: boolean;
  languages: string[];
  bio: string;
  dailyRate: number;
}

export interface CustomerProfileInput {
  address: string;
  city: string;
  state: string;
}

export interface RegisterDriverRequest {
  email: string;
  password: string;
  fullName: string;
  phone: string;
  profile: DriverProfileInput;
}

export interface RegisterCustomerRequest {
  email: string;
  password: string;
  fullName: string;
  phone: string;
  profile: CustomerProfileInput;
}

export interface DriverProfileResponse extends DriverProfileInput {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  available: boolean;
}

export interface CustomerProfileResponse extends CustomerProfileInput {
  id: number;
  fullName: string;
  email: string;
  phone: string;
}

export interface MeResponse {
  user: UserView;
  driver: DriverProfileResponse | null;
  customer: CustomerProfileResponse | null;
}

export interface DriverSummary {
  id: number;
  fullName: string;
  city: string;
  state: string;
  yearsOfExperience: number;
  drivesManual: boolean;
  drivesAutomatic: boolean;
  languages: string[];
  bio: string | null;
  dailyRate: number;
}

export interface CreateTripRequest {
  pickupCity: string;
  pickupLocation: string;
  destination: string;
  tripType: TripType;
  startAt: string; // ISO instant
  returnAt: string | null;
  carMake: string;
  carModel: string;
  carType: CarType;
  transmission: Transmission;
  notes: string;
}

export interface TripDriverInfo {
  id: number;
  fullName: string;
  phone: string | null; // only after the driver confirms
  city: string;
  state: string;
  yearsOfExperience: number;
  languages: string[];
  bio: string | null;
  dailyRate: number;
}

export interface Trip {
  id: number;
  status: TripStatus;
  tripType: TripType;
  pickupCity: string;
  pickupLocation: string;
  destination: string;
  startAt: string;
  returnAt: string | null;
  car: { make: string; model: string; type: CarType; transmission: Transmission };
  notes: string | null;
  customer: { fullName: string; phone: string | null };
  driver: TripDriverInfo | null;
  createdAt: string;
}
