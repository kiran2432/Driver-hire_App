import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { meApi, tripsApi } from '../../api/endpoints';
import type { CarType, Transmission, TripType } from '../../api/types';
import { SelectField, TextAreaField, TextField } from '../../components/Field';
import { Alert } from '../../components/ui';
import { useLoad } from '../../utils/useLoad';
import {
  CAR_TYPE_LABEL,
  TRANSMISSION_LABEL,
  errorMessage,
  fieldErrorsOf,
  localInputToIso,
  nowForDatetimeLocal,
} from '../../utils/format';

const carTypeOptions = (Object.keys(CAR_TYPE_LABEL) as CarType[]).map((v) => ({ value: v, label: CAR_TYPE_LABEL[v] }));
const transmissionOptions = (Object.keys(TRANSMISSION_LABEL) as Transmission[]).map((v) => ({
  value: v,
  label: TRANSMISSION_LABEL[v],
}));

export function NewTripPage() {
  const navigate = useNavigate();
  const me = useLoad(() => meApi.get(), []);

  const [f, setF] = useState({
    pickupCity: '',
    pickupLocation: '',
    destination: '',
    tripType: 'ONE_WAY' as TripType,
    startAt: '',
    returnAt: '',
    carMake: '',
    carModel: '',
    carType: 'SEDAN' as CarType,
    transmission: 'MANUAL' as Transmission,
    notes: '',
  });
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  // Default the pickup city to the customer's own city, unless they already typed something.
  const defaultCity = me.data?.customer?.city;
  useEffect(() => {
    if (defaultCity) setF((p) => (p.pickupCity ? p : { ...p, pickupCity: defaultCity }));
  }, [defaultCity]);

  const set = <K extends keyof typeof f>(key: K) => (value: (typeof f)[K]) => setF((p) => ({ ...p, [key]: value }));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setErrors({});
    try {
      const trip = await tripsApi.create({
        pickupCity: f.pickupCity,
        pickupLocation: f.pickupLocation,
        destination: f.destination,
        tripType: f.tripType,
        startAt: localInputToIso(f.startAt),
        returnAt: f.tripType === 'ROUND_TRIP' && f.returnAt ? localInputToIso(f.returnAt) : null,
        carMake: f.carMake,
        carModel: f.carModel,
        carType: f.carType,
        transmission: f.transmission,
        notes: f.notes,
      });
      navigate(`/customer/trips/${trip.id}/drivers`);
    } catch (err) {
      setError(errorMessage(err));
      setErrors(fieldErrorsOf(err));
    } finally {
      setBusy(false);
    }
  }

  const min = nowForDatetimeLocal();

  return (
    <section className="narrow">
      <h1>Book a driver</h1>
      <p className="muted">Tell us about your trip and your car. Next you’ll choose a driver.</p>
      <form onSubmit={(e) => void onSubmit(e)} className="card form">
        {error && <Alert>{error}</Alert>}

        <h2>Trip</h2>
        <fieldset>
          <legend>Trip type</legend>
          <div className="segmented">
            {(['ONE_WAY', 'ROUND_TRIP'] as TripType[]).map((t) => (
              <label key={t} className={f.tripType === t ? 'selected' : ''}>
                <input type="radio" name="tripType" value={t} checked={f.tripType === t}
                  onChange={() => set('tripType')(t)} />
                {t === 'ONE_WAY' ? 'One way' : 'Round trip'}
              </label>
            ))}
          </div>
        </fieldset>
        <TextField label="Pickup city" value={f.pickupCity} onChange={set('pickupCity')} required maxLength={100}
          error={errors.pickupCity} hint="Drivers in this city can see your trip" />
        <TextField label="Pickup location" value={f.pickupLocation} onChange={set('pickupLocation')} required
          maxLength={255} error={errors.pickupLocation} />
        <TextField label="Destination" value={f.destination} onChange={set('destination')} required maxLength={255}
          error={errors.destination} />
        <div className="row">
          <TextField label="Start date & time" type="datetime-local" min={min} value={f.startAt}
            onChange={set('startAt')} required error={errors.startAt} />
          {f.tripType === 'ROUND_TRIP' && (
            <TextField label="Return date & time" type="datetime-local" min={f.startAt || min} value={f.returnAt}
              onChange={set('returnAt')} required error={errors.returnAt} />
          )}
        </div>

        <h2>Your car</h2>
        <div className="row">
          <TextField label="Make" value={f.carMake} onChange={set('carMake')} required maxLength={60}
            error={errors.carMake} hint="e.g. Maruti, Hyundai" />
          <TextField label="Model" value={f.carModel} onChange={set('carModel')} required maxLength={60}
            error={errors.carModel} hint="e.g. Swift, Creta" />
        </div>
        <div className="row">
          <SelectField label="Car type" value={f.carType} onChange={set('carType')} options={carTypeOptions}
            error={errors.carType} />
          <SelectField label="Transmission" value={f.transmission} onChange={set('transmission')}
            options={transmissionOptions} error={errors.transmission} />
        </div>
        <TextAreaField label="Notes for the driver" value={f.notes} onChange={set('notes')} maxLength={1000}
          hint="Optional: luggage, number of passengers, stops on the way" error={errors.notes} />

        <div className="actions">
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Saving…' : 'Save trip & find drivers'}
          </button>
          <Link to="/customer/trips" className="btn btn-ghost">
            Cancel
          </Link>
        </div>
      </form>
    </section>
  );
}
