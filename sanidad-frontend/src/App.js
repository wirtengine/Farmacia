import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Sidebar from './components/Sidebar';

import Login from './pages/Login';
import Empleados from './pages/Empleados';
import Medicamentos from './pages/Medicamentos';
import Lotes from './pages/Lotes';
import Proveedores from './pages/Proveedores';
import Clientes from './pages/Clientes';
import Ventas from './pages/Ventas';
import Devoluciones from './pages/Devoluciones';
import DevolucionesProveedor from './pages/DevolucionesProveedor'; // <-- NUEVO IMPORT

const DashboardPlaceholder = () => (
    <div className="module-container">
        <h1>Bienvenido al Sistema</h1>
    </div>
);

function AppContent() {
    const { user, loading } = useAuth();

    if (loading) return <div className="loading-screen">Cargando...</div>;

    return (
        <div className="app-layout">
            {user && <Sidebar />}

            <div className={user ? 'main-content with-sidebar' : 'main-content'}>
                <Routes>

                    {/* Ruta principal SIEMPRE al login */}
                    <Route path="/" element={<Navigate to="/login" replace />} />

                    {/* Login */}
                    <Route
                        path="/login"
                        element={user ? <Navigate to="/dashboard" replace /> : <Login />}
                    />

                    {/* Dashboard */}
                    <Route
                        path="/dashboard"
                        element={
                            <PrivateRoute>
                                <DashboardPlaceholder />
                            </PrivateRoute>
                        }
                    />

                    {/* Empleados */}
                    <Route
                        path="/empleados"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN']}>
                                <Empleados />
                            </PrivateRoute>
                        }
                    />

                    {/* Medicamentos */}
                    <Route
                        path="/medicamentos"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Medicamentos />
                            </PrivateRoute>
                        }
                    />

                    {/* Lotes */}
                    <Route
                        path="/lotes"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Lotes />
                            </PrivateRoute>
                        }
                    />

                    {/* Proveedores */}
                    <Route
                        path="/proveedores"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Proveedores />
                            </PrivateRoute>
                        }
                    />

                    {/* Clientes */}
                    <Route
                        path="/clientes"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Clientes />
                            </PrivateRoute>
                        }
                    />

                    {/* Ventas */}
                    <Route
                        path="/ventas"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Ventas />
                            </PrivateRoute>
                        }
                    />

                    {/* Devoluciones */}
                    <Route
                        path="/devoluciones"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Devoluciones />
                            </PrivateRoute>
                        }
                    />

                    {/* NUEVA RUTA: Devoluciones a proveedor */}
                    <Route
                        path="/devoluciones-proveedor"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN']}>
                                <DevolucionesProveedor />
                            </PrivateRoute>
                        }
                    />

                    {/* Cualquier ruta que no exista */}
                    <Route path="*" element={<Navigate to="/login" replace />} />

                </Routes>
            </div>
        </div>
    );
}

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <AppContent />
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;