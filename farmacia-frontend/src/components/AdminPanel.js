import React, { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom'; // <-- Importar

const AdminPanel = () => {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate(); // <-- Hook para navegación

    return (
        <div style={{ padding: '20px' }}>
            <h1>Panel de Administrador</h1>
            <p>Bienvenido, {user.nombre} (Rol: {user.rol})</p>

            {/* Botón para registrar nuevo usuario */}
            <button
                onClick={() => navigate('/admin/register')}
                style={{ padding: '10px', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '5px', marginRight: '10px' }}
            >
                Registrar Nuevo Usuario
            </button>

            <button onClick={logout} style={{ padding: '10px', backgroundColor: '#dc3545', color: 'white', border: 'none', borderRadius: '5px' }}>
                Cerrar sesión
            </button>
        </div>
    );
};

export default AdminPanel;