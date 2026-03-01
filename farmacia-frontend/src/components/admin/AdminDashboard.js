import React, { useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';

const AdminDashboard = () => {
    const { user } = useContext(AuthContext);
    return (
        <div className="card">
            <h2>Dashboard Ejecutivo</h2>
            <p>Bienvenido, {user?.nombre}. Aquí irán los indicadores y gráficos.</p>
        </div>
    );
};

export default AdminDashboard;