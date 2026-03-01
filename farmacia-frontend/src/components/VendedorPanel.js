import React, { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { Link, useNavigate } from 'react-router-dom';

const VendedorPanel = () => {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <div style={{ padding: '20px' }}>
            <div className="flex-between">
                <h1>Panel de Vendedor</h1>
                <button onClick={handleLogout} className="btn btn-danger">Cerrar sesión</button>
            </div>
            <p>Bienvenido, {user?.nombre} (Rol: {user?.rol})</p>

            <div style={{ marginTop: '30px', display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '20px' }}>
                <Link to="/vendedor/medicamentos" style={{ textDecoration: 'none' }}>
                    <div className="card" style={{ textAlign: 'center', cursor: 'pointer' }}>
                        <h3>Consultar Inventario</h3>
                        <p>Ver medicamentos disponibles</p>
                    </div>
                </Link>

                <Link to="/vendedor/proveedores" style={{ textDecoration: 'none' }}>
                    <div className="card" style={{ textAlign: 'center', cursor: 'pointer' }}>
                        <h3>Consultar Proveedores</h3>
                        <p>Ver información de proveedores</p>
                    </div>
                </Link>
                {/* Otros enlaces */}
            </div>
        </div>
    );
};

export default VendedorPanel;