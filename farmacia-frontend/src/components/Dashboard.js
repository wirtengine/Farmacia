import React, { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { Navigate } from 'react-router-dom';

const Dashboard = () => {
    const { user } = useContext(AuthContext);

    if (!user) return <Navigate to="/login" />;

    if (user.rol === 'ADMIN') {
        return <Navigate to="/admin" />;
    } else {
        return <Navigate to="/vendedor" />;
    }
};

export default Dashboard;