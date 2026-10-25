import { Route, Routes } from 'react-router-dom';
import { RequireRole } from './auth/RequireRole';
import { Layout } from './components/Layout';
import { CustomerTripPage } from './pages/customer/CustomerTripPage';
import { FindDriversPage } from './pages/customer/FindDriversPage';
import { MyTripsPage } from './pages/customer/MyTripsPage';
import { NewTripPage } from './pages/customer/NewTripPage';
import { DriverDashboardPage } from './pages/driver/DriverDashboardPage';
import { DriverProfilePage } from './pages/driver/DriverProfilePage';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { RegisterChoicePage } from './pages/RegisterChoicePage';
import { RegisterCustomerPage } from './pages/RegisterCustomerPage';
import { RegisterDriverPage } from './pages/RegisterDriverPage';

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterChoicePage />} />
        <Route path="register/customer" element={<RegisterCustomerPage />} />
        <Route path="register/driver" element={<RegisterDriverPage />} />

        <Route element={<RequireRole role="CUSTOMER" />}>
          <Route path="customer/book" element={<NewTripPage />} />
          <Route path="customer/trips" element={<MyTripsPage />} />
          <Route path="customer/trips/:id" element={<CustomerTripPage />} />
          <Route path="customer/trips/:id/drivers" element={<FindDriversPage />} />
        </Route>

        <Route element={<RequireRole role="DRIVER" />}>
          <Route path="driver" element={<DriverDashboardPage />} />
          <Route path="driver/profile" element={<DriverProfilePage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
