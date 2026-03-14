import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function PrivateRoute({ children, allowedRoles }) {
    const { user, loading } = useAuth();

    if (loading) return <div>Cargando...</div>;

    if (!user) return <Navigate to="/login" />;

    if (allowedRoles && !allowedRoles.includes(user.rol)) {
        return <Navigate to="/dashboard" />; // o a una página de no autorizado
    }

    return children;
}