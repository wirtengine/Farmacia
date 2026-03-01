import React, { useContext } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, AuthContext } from './context/AuthContext';

// Login y dashboard
import Login from './components/Login';
import Dashboard from './components/Dashboard';

// Admin
import AdminLayout from './components/admin/AdminLayout';
import AdminDashboard from './components/admin/AdminDashboard';
import MedicamentoList from './components/medicamentos/MedicamentoList';
import MedicamentoForm from './components/medicamentos/MedicamentoForm';

// Lotes
import LoteList from './components/lotes/LoteList';
import LoteForm from './components/lotes/LoteForm';

// Usuarios (módulo admin)
import UsuarioList from './components/usuarios/UsuarioList';
import UsuarioForm from './components/usuarios/UsuarioForm';

// Proveedores (módulo admin)
import ProveedorList from './components/proveedores/ProveedorList';
import ProveedorForm from './components/proveedores/ProveedorForm';

// Vendedor
import VendedorPanel from './components/VendedorPanel';

// Clientes
import ClienteList from './components/clientes/ClienteList';
import ClienteForm from './components/clientes/ClienteForm';

// Registro solo admin
import Register from './components/Register';

// Componentes para ventas y devoluciones
import VentaList from './components/ventas/VentaList';
import VentaForm from './components/ventas/VentaForm';
import VentaDetalle from './components/ventas/VentaDetalle';
import DevolucionList from './components/devoluciones/DevolucionList';
import DevolucionSolicitud from './components/devoluciones/DevolucionSolicitud';
import DevolucionAprobacion from './components/devoluciones/DevolucionAprobacion';

const PrivateRoute = ({ children, allowedRoles }) => {
    const { user } = useContext(AuthContext);

    if (!user) return <Navigate to="/login" />;
    if (!allowedRoles.includes(user.rol)) return <Navigate to="/dashboard" />;

    return children;
};

function AppRoutes() {
    return (
        <Routes>
            {/* Público */}
            <Route path="/login" element={<Login />} />
            <Route path="/dashboard" element={<Dashboard />} />

            {/* ADMIN */}
            <Route
                path="/admin"
                element={
                    <PrivateRoute allowedRoles={['ADMIN']}>
                        <AdminLayout />
                    </PrivateRoute>
                }
            >
                <Route index element={<Navigate to="dashboard" />} />
                <Route path="dashboard" element={<AdminDashboard />} />

                {/* Medicamentos */}
                <Route path="medicamentos" element={<MedicamentoList />} />
                <Route path="medicamentos/nuevo" element={<MedicamentoForm />} />
                <Route path="medicamentos/editar/:id" element={<MedicamentoForm />} />

                {/* Lotes ADMIN */}
                <Route path="lotes" element={<LoteList />} />
                <Route path="lotes/nuevo" element={<LoteForm />} />
                <Route path="lotes/editar/:id" element={<LoteForm />} />

                {/* Usuarios */}
                <Route path="usuarios" element={<UsuarioList />} />
                <Route path="usuarios/nuevo" element={<UsuarioForm />} />
                <Route path="usuarios/editar/:id" element={<UsuarioForm />} />

                {/* Proveedores */}
                <Route path="proveedores" element={<ProveedorList />} />
                <Route path="proveedores/nuevo" element={<ProveedorForm />} />
                <Route path="proveedores/editar/:id" element={<ProveedorForm />} />

                {/* Registro */}
                <Route path="register" element={<Register />} />

                {/* Clientes dentro del admin */}
                <Route path="clientes" element={<ClienteList />} />
                <Route path="clientes/nuevo" element={<ClienteForm />} />
                <Route path="clientes/editar/:id" element={<ClienteForm />} />

                {/* Ventas ADMIN */}
                <Route path="ventas" element={<VentaList />} />
                <Route path="ventas/nueva" element={<VentaForm />} />
                <Route path="ventas/rapida" element={<VentaForm modo="rapida" />} />
                <Route path="ventas/:id" element={<VentaDetalle />} />

                {/* Devoluciones ADMIN */}
                <Route path="devoluciones" element={<DevolucionList />} />
                <Route path="devoluciones/nueva" element={<DevolucionSolicitud />} />
                <Route path="devoluciones/:id/aprobar" element={<DevolucionAprobacion />} />
            </Route>

            {/* VENDEDOR */}
            <Route
                path="/vendedor"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <VendedorPanel />
                    </PrivateRoute>
                }
            />

            {/* Vendedor → Medicamentos */}
            <Route
                path="/vendedor/medicamentos"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <MedicamentoList />
                    </PrivateRoute>
                }
            />

            {/* Vendedor → Proveedores */}
            <Route
                path="/vendedor/proveedores"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <ProveedorList />
                    </PrivateRoute>
                }
            />

            {/* Vendedor → Clientes */}
            <Route
                path="/vendedor/clientes"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <ClienteList />
                    </PrivateRoute>
                }
            />
            <Route
                path="/vendedor/clientes/nuevo"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <ClienteForm />
                    </PrivateRoute>
                }
            />
            <Route
                path="/vendedor/clientes/editar/:id"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <ClienteForm />
                    </PrivateRoute>
                }
            />

            {/* Vendedor → Lotes (solo consulta) */}
            <Route
                path="/vendedor/lotes"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <LoteList />
                    </PrivateRoute>
                }
            />

            {/* Vendedor → Ventas */}
            <Route
                path="/vendedor/ventas"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <VentaList />
                    </PrivateRoute>
                }
            />
            <Route
                path="/vendedor/ventas/nueva"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <VentaForm />
                    </PrivateRoute>
                }
            />
            <Route
                path="/vendedor/ventas/rapida"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <VentaForm modo="rapida" />
                    </PrivateRoute>
                }
            />
            <Route
                path="/vendedor/ventas/:id"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <VentaDetalle />
                    </PrivateRoute>
                }
            />

            {/* Vendedor → Devoluciones */}
            <Route
                path="/vendedor/devoluciones"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <DevolucionList />
                    </PrivateRoute>
                }
            />
            <Route
                path="/vendedor/devoluciones/nueva"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <DevolucionSolicitud />
                    </PrivateRoute>
                }
            />

            {/* Redirección por defecto */}
            <Route path="*" element={<Navigate to="/login" />} />
        </Routes>
    );
}

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <AppRoutes />
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;