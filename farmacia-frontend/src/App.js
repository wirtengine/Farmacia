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

// Usuarios (módulo admin)
import UsuarioList from './components/usuarios/UsuarioList';
import UsuarioForm from './components/usuarios/UsuarioForm';

// Proveedores (módulo admin)
import ProveedorList from './components/proveedores/ProveedorList';
import ProveedorForm from './components/proveedores/ProveedorForm';

// Vendedor
import VendedorPanel from './components/VendedorPanel';

// Registro (solo admin)
import Register from './components/Register';

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

            {/* ADMIN → rutas anidadas */}
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

                {/* Usuarios */}
                <Route path="usuarios" element={<UsuarioList />} />
                <Route path="usuarios/nuevo" element={<UsuarioForm />} />
                <Route path="usuarios/editar/:id" element={<UsuarioForm />} />

                {/* Proveedores */}
                <Route path="proveedores" element={<ProveedorList />} />
                <Route path="proveedores/nuevo" element={<ProveedorForm />} />
                <Route path="proveedores/editar/:id" element={<ProveedorForm />} />

                {/* Registro solo admin */}
                <Route path="register" element={<Register />} />
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

            {/* Medicamentos solo lectura para vendedor */}
            <Route
                path="/vendedor/medicamentos"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <MedicamentoList />
                    </PrivateRoute>
                }
            />

            {/* Proveedores solo lectura para vendedor */}
            <Route
                path="/vendedor/proveedores"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <ProveedorList />
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