import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import NavBar from './components/NavBar.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import { HOME, useAuth } from './context/AuthContext.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Addresses from './pages/customer/Addresses.jsx'
import ChooseStore from './pages/customer/ChooseStore.jsx'
import StoreItems from './pages/customer/StoreItems.jsx'
import Checkout from './pages/customer/Checkout.jsx'
import Orders from './pages/customer/Orders.jsx'
import OrderDetail from './pages/customer/OrderDetail.jsx'
import Receipt from './pages/customer/Receipt.jsx'
import AdminStores from './pages/admin/AdminStores.jsx'
import AdminStoreDetail from './pages/admin/AdminStoreDetail.jsx'
import PartnerProfile from './pages/partner/PartnerProfile.jsx'
import PartnerDeliveries from './pages/partner/PartnerDeliveries.jsx'

export default function App() {
  const { user } = useAuth()
  const location = useLocation()
  const home = user ? HOME[user.role] : '/login'

  return (
    <>
      <NavBar />
      <main className="container">
        <ErrorBoundary resetKey={location.pathname}>
        <Routes>
          <Route path="/" element={<Navigate to={home} replace />} />
          <Route path="/login" element={user ? <Navigate to={home} replace /> : <Login />} />
          <Route path="/register" element={user ? <Navigate to={home} replace /> : <Register />} />

          <Route element={<ProtectedRoute role="CUSTOMER" />}>
            <Route path="/shop" element={<ChooseStore />} />
            <Route path="/shop/store/:storeId" element={<StoreItems />} />
            <Route path="/checkout" element={<Checkout />} />
            <Route path="/orders" element={<Orders />} />
            <Route path="/orders/:orderId" element={<OrderDetail />} />
            <Route path="/orders/:orderId/receipt" element={<Receipt />} />
            <Route path="/addresses" element={<Addresses />} />
          </Route>

          <Route element={<ProtectedRoute role="ADMIN" />}>
            <Route path="/admin" element={<AdminStores />} />
            <Route path="/admin/stores/:storeId" element={<AdminStoreDetail />} />
          </Route>

          <Route element={<ProtectedRoute role="PARTNER" />}>
            <Route path="/partner" element={<PartnerProfile />} />
            <Route path="/partner/deliveries" element={<PartnerDeliveries />} />
          </Route>

          <Route path="*" element={<Navigate to={home} replace />} />
        </Routes>
        </ErrorBoundary>
      </main>
    </>
  )
}
