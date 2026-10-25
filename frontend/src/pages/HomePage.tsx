import { Link, Navigate } from 'react-router-dom';
import { homePathFor, useAuth } from '../auth/AuthContext';

export function HomePage() {
  const { user } = useAuth();
  if (user) return <Navigate to={homePathFor(user.role)} replace />;

  return (
    <>
      <section className="hero">
        <h1>A trusted driver for every trip</h1>
        <p className="lead">
          Planning a long drive, an outstation trip or a night out? Tell us about your trip and your car,
          pick an experienced driver and hire them in minutes. Drivers: find customers looking for you.
        </p>
        <div className="hero-actions">
          <Link to="/register/customer" className="btn btn-primary btn-lg">
            I need a driver
          </Link>
          <Link to="/register/driver" className="btn btn-secondary btn-lg">
            I’m a driver
          </Link>
        </div>
        <p className="muted">
          Already registered? <Link to="/login">Sign in</Link>
        </p>
      </section>

      <section aria-labelledby="how">
        <h2 id="how">How it works</h2>
        <ol className="steps">
          <li>
            <h3>Describe your trip</h3>
            <p>Pickup, destination, dates, one-way or round trip, and the car you’ll be travelling in.</p>
          </li>
          <li>
            <h3>Choose a driver</h3>
            <p>Compare experience, languages, car skills and daily rates, then send a hire request.</p>
          </li>
          <li>
            <h3>Confirm and go</h3>
            <p>Once the driver accepts, you both get each other’s contact details.</p>
          </li>
        </ol>
      </section>
    </>
  );
}
