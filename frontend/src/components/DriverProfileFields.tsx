import type { DriverProfileInput, DriverProfileResponse } from '../api/types';
import { CheckboxField, TextAreaField, TextField } from './Field';

/** Form state for the driver details; numbers and the language list are kept as text while editing. */
export interface DriverFormValues {
  address: string;
  city: string;
  state: string;
  yearsOfExperience: string;
  licenseNumber: string;
  licenseExpiry: string;
  drivesManual: boolean;
  drivesAutomatic: boolean;
  languages: string;
  bio: string;
  dailyRate: string;
}

export const emptyDriverForm: DriverFormValues = {
  address: '',
  city: '',
  state: '',
  yearsOfExperience: '',
  licenseNumber: '',
  licenseExpiry: '',
  drivesManual: true,
  drivesAutomatic: false,
  languages: '',
  bio: '',
  dailyRate: '',
};

export function driverFormFrom(p: DriverProfileResponse): DriverFormValues {
  return {
    address: p.address,
    city: p.city,
    state: p.state,
    yearsOfExperience: String(p.yearsOfExperience),
    licenseNumber: p.licenseNumber,
    licenseExpiry: p.licenseExpiry,
    drivesManual: p.drivesManual,
    drivesAutomatic: p.drivesAutomatic,
    languages: p.languages.join(', '),
    bio: p.bio ?? '',
    dailyRate: String(p.dailyRate),
  };
}

export function toDriverInput(f: DriverFormValues): DriverProfileInput {
  return {
    address: f.address,
    city: f.city,
    state: f.state,
    yearsOfExperience: Number(f.yearsOfExperience),
    licenseNumber: f.licenseNumber,
    licenseExpiry: f.licenseExpiry,
    drivesManual: f.drivesManual,
    drivesAutomatic: f.drivesAutomatic,
    languages: f.languages
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean),
    bio: f.bio,
    dailyRate: Number(f.dailyRate),
  };
}

interface Props {
  values: DriverFormValues;
  onChange: <K extends keyof DriverFormValues>(key: K, value: DriverFormValues[K]) => void;
  /** Server validation errors, keyed like "profile.city". */
  errors: Record<string, string>;
}

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function DriverProfileFields({ values, onChange, errors }: Props) {
  const err = (name: string) => errors[`profile.${name}`];
  return (
    <>
      <TextField label="Address" value={values.address} onChange={(v) => onChange('address', v)} required
        maxLength={255} autoComplete="street-address" error={err('address')} />
      <div className="row">
        <TextField label="City" value={values.city} onChange={(v) => onChange('city', v)} required maxLength={100}
          autoComplete="address-level2" error={err('city')} hint="Customers in this city will find you first" />
        <TextField label="State" value={values.state} onChange={(v) => onChange('state', v)} required maxLength={100}
          autoComplete="address-level1" error={err('state')} />
      </div>
      <div className="row">
        <TextField label="Years of driving experience" type="number" min={0} max={60} value={values.yearsOfExperience}
          onChange={(v) => onChange('yearsOfExperience', v)} required error={err('yearsOfExperience')} />
        <TextField label="Daily rate (₹)" type="number" min={0} step="50" value={values.dailyRate}
          onChange={(v) => onChange('dailyRate', v)} required error={err('dailyRate')} />
      </div>
      <div className="row">
        <TextField label="Driving licence number" value={values.licenseNumber}
          onChange={(v) => onChange('licenseNumber', v)} required maxLength={50} error={err('licenseNumber')}
          hint="Only you can see this" />
        <TextField label="Licence expiry date" type="date" min={today()} value={values.licenseExpiry}
          onChange={(v) => onChange('licenseExpiry', v)} required error={err('licenseExpiry')} />
      </div>
      <fieldset>
        <legend>Cars you can drive</legend>
        <CheckboxField label="Manual" checked={values.drivesManual} onChange={(c) => onChange('drivesManual', c)} />
        <CheckboxField label="Automatic" checked={values.drivesAutomatic}
          onChange={(c) => onChange('drivesAutomatic', c)} />
      </fieldset>
      <TextField label="Languages" value={values.languages} onChange={(v) => onChange('languages', v)}
        hint="Comma separated, e.g. Kannada, Hindi, English" error={err('languages')} />
      <TextAreaField label="About you" value={values.bio} onChange={(v) => onChange('bio', v)} maxLength={1000}
        hint="Optional: routes you know, type of trips you prefer" error={err('bio')} />
    </>
  );
}
