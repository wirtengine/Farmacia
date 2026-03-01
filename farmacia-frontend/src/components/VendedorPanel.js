import React, { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

const VendedorPanel = () => {
    const { user, logout } = useContext(AuthContext);

    return (
        <div style={{ padding: '20px' }}>
            <h1>Panel de Vendedor</h1>
            <p>Bienvenido, {user.nombre} (Rol: {user.rol})</p>
            <button onClick={logout} style={{ padding: '10px', backgroundColor: '#dc3545', color: 'white', border: 'none', borderRadius: '5px' }}>
                Cerrar sesión
            </button>
            {/* Aquí irán las funcionalidades de ventas */}
        </div>
    );
};

export default VendedorPanel;