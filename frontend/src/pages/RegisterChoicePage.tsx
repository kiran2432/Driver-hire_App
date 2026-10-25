import { Link } from 'react-router-dom';

export function RegisterChoicePage() {
  return (
    <section className="narrow">
      <h1>Create your account</h1>
      <div className="grid two">
        <Link to="/register/customer" className="card choice">
          <h2>I need a driver</h2>
          <p>Post your trip, choose a driver and hire them.</p>
          <span className="btn btn-primary">Register as customer</span>
        </Link>
        <Link to="/register/driver" className="card choice">
          <h2>I’m a driver</h2>
          <p>Create your driver profile and get requests from customers.</p>
          <span className="btn btn-secondary">Register as driver</span>
        </Link>
      </div>
      <p className="muted center">
        Already registered? <Link to="/login">Sign in</Link>
      </p>
    </section>
  );
}
