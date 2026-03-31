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

function AppContent() {
    const { user, loading } = useAuth();

    if (loading) return <div className="loading-screen">Cargando...</div>;

    return (
        <div className="app-layout">
            {/* El Sidebar solo se muestra si el usuario está logueado */}
            {user && <Sidebar />}

            <div className={user ? 'main-content with-sidebar' : 'main-content'}>
                <Routes>
                    {/* Redirección inicial */}
                    <Route path="/" element={<Navigate to="/login" replace />} />

                    {/* Autenticación */}
                    <Route
                        path="/login"
                        element={user ? <Navigate to="/dashboard" replace /> : <Login />}
                    />

                    {/* Dashboard Principal */}
                    <Route
                        path="/dashboard"
                        element={
                            <PrivateRoute>
                                <Dashboard />
                            </PrivateRoute>
                        }
                    />

                    {/* Módulos de Notificaciones e Inteligencia */}
                    <Route
                        path="/alerts"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Alerts />
                            </PrivateRoute>
                        }
                    />

                    <Route
                        path="/recommendations"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Recommendations />
                            </PrivateRoute>
                        }
                    />

                    {/* Gestión de Inventario y Operaciones */}
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

                    {/* Ventas y Clientes */}
                    <Route
                        path="/ventas"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Ventas />
                            </PrivateRoute>
                        }
                    />

                    <Route
                        path="/clientes"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Clientes />
                            </PrivateRoute>
                        }
                    />

                    {/* Recursos Humanos y Logística */}
                    <Route
                        path="/empleados"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN']}>
                                <Empleados />
                            </PrivateRoute>
                        }
                    />

                    <Route
                        path="/proveedores"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Proveedores />
                            </PrivateRoute>
                        }
                    />

                    {/* Devoluciones y Ubicaciones */}
                    <Route
                        path="/devoluciones"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'VENDEDOR']}>
                                <Devoluciones />
                            </PrivateRoute>
                        }
                    />

                    <Route
                        path="/devoluciones-proveedor"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN']}>
                                <DevolucionesProveedor />
                            </PrivateRoute>
                        }
                    />

                    <Route
                        path="/ubicaciones"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN']}>
                                <Ubicaciones />
                            </PrivateRoute>
                        }
                    />

                    {/* Fallback de seguridad */}
                    <Route path="*" element={<Navigate to="/login" replace />} />
                </Routes>
            </div>

            {/* El Asistente se renderiza fuera del flujo de las rutas para que sea global.
               Recuerda que internamente el componente filtrará si debe mostrarse
               según la URL actual.
            */}
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