import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Sidebar from './components/Sidebar';
import ChatAssistant from './components/ChatAssistant';

import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Empleados from './pages/Empleados';
import Medicamentos from './pages/Medicamentos';
import Lotes from './pages/Lotes';
import Proveedores from './pages/Proveedores';
import Clientes from './pages/Clientes';
import Ventas from './pages/Ventas';
import Devoluciones from './pages/Devoluciones';
import DevolucionesProveedor from './pages/DevolucionesProveedor';
import Ubicaciones from './pages/Ubicaciones';
import Alerts from './pages/Alerts';
import Recommendations from './pages/Recommendations';
import Perdidas from './pages/Perdidas';

// 🆕 NUEVO
import Recetas from './pages/Recetas';

function AppContent() {
    const { user, loading } = useAuth();

    if (loading) return <div className="loading-screen">Cargando...</div>;

    return (
        <div className="app-layout">
            {user && <Sidebar />}

            <div className={user ? 'main-content with-sidebar' : 'main-content'}>
                <Routes>
                    {/* Redirección inicial */}
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
                                <Dashboard />
                            </PrivateRoute>
                        }
                    />

                    {/* Alertas */}
                    <Route
                        path="/alerts"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Alerts />
                            </PrivateRoute>
                        }
                    />

                    {/* Recomendaciones */}
                    <Route
                        path="/recommendations"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Recommendations />
                            </PrivateRoute>
                        }
                    />

                    {/* Inventario */}
                    <Route
                        path="/medicamentos"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Medicamentos />
                            </PrivateRoute>
                        }
                    />

                    <Route
                        path="/lotes"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Lotes />
                            </PrivateRoute>
                        }
                    />

                    <Route
                        path="/perdidas"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN']}>
                                <Perdidas />
                            </PrivateRoute>
                        }
                    />

                    {/* 🆕 RECETAS */}
                    <Route
                        path="/recetas"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'FARMACEUTICO']}>
                                <Recetas />
                            </PrivateRoute>
                        }
                    />

                    {/* Ventas */}
                    <Route
                        path="/ventas"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR', 'FARMACEUTICO']}>
                                <Ventas />
                            </PrivateRoute>
                        }
                    />

                    {/* Clientes */}
                    <Route
                        path="/clientes"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR', 'FARMACEUTICO']}>
                                <Clientes />
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

                    {/* Proveedores */}
                    <Route
                        path="/proveedores"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Proveedores />
                            </PrivateRoute>
                        }
                    />

                    {/* Devoluciones */}
                    <Route
                        path="/devoluciones"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR', 'FARMACEUTICO']}>
                                <Devoluciones />
                            </PrivateRoute>
                        }
                    />

                    {/* Devoluciones proveedor */}
                    <Route
                        path="/devoluciones-proveedor"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN']}>
                                <DevolucionesProveedor />
                            </PrivateRoute>
                        }
                    />

                    {/* Ubicaciones */}
                    <Route
                        path="/ubicaciones"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN']}>
                                <Ubicaciones />
                            </PrivateRoute>
                        }
                    />

                    {/* Fallback */}
                    <Route path="*" element={<Navigate to="/login" replace />} />
                </Routes>
            </div>

            {user && <ChatAssistant />}
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