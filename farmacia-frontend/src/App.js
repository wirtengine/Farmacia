import React, { useContext } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, AuthContext } from './context/AuthContext';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import AdminPanel from './components/AdminPanel';
import VendedorPanel from './components/VendedorPanel';
import Register from './components/Register'; // <-- Importar

const PrivateRoute = ({ children, allowedRoles }) => {
    const { user } = useContext(AuthContext);
    if (!user) return <Navigate to="/login" />;
    if (!allowedRoles.includes(user.rol)) return <Navigate to="/dashboard" />;
    return children;
};

function AppRoutes() {
    return (
        <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route
                path="/admin"
                element={
                    <PrivateRoute allowedRoles={['ADMIN']}>
                        <AdminPanel />
                    </PrivateRoute>
                }
            />
            <Route
                path="/admin/register"
                element={
                    <PrivateRoute allowedRoles={['ADMIN']}>
                        <Register />
                    </PrivateRoute>
                }
            />
            <Route
                path="/vendedor"
                element={
                    <PrivateRoute allowedRoles={['VENDEDOR']}>
                        <VendedorPanel />
                    </PrivateRoute>
                }
            />
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