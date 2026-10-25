import type { ReactNode } from 'react';
import type { DriverSummary } from '../api/types';
import { formatMoney } from '../utils/format';

export function DriverCard({ driver, children }: { driver: DriverSummary; children?: ReactNode }) {
  const gearboxes = [driver.drivesManual && 'Manual', driver.drivesAutomatic && 'Automatic'].filter(Boolean);
  return (
    <article className="card driver-card">
      <header className="card-header">
        <h3>{driver.fullName}</h3>
        <span className="price">
          {formatMoney(driver.dailyRate)}
          <small>/day</small>
        </span>
      </header>
      <p className="muted">
        {driver.city}, {driver.state}
      </p>
      <ul className="chips">
        <li>{driver.yearsOfExperience} yrs experience</li>
        {gearboxes.map((g) => (
          <li key={String(g)}>{g}</li>
        ))}
        {driver.languages.map((l) => (
          <li key={l}>{l}</li>
        ))}
      </ul>
      {driver.bio && <p>{driver.bio}</p>}
      {children && <footer className="card-actions">{children}</footer>}
    </article>
  );
}
