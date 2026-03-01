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
                <button onClick={handleLogout} className="btn btn-danger">
                    Cerrar sesión
                </button>
            </div>

            <p>Bienvenido, {user?.nombre} (Rol: {user?.rol})</p>

            <div
                style={{
                    marginTop: '30px',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
                    gap: '20px'
                }}
            >
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

                <Link to="/vendedor/clientes" style={{ textDecoration: 'none' }}>
                    <div className="card" style={{ textAlign: 'center', cursor: 'pointer' }}>
                        <h3>Gestión de Clientes</h3>
                        <p>Administrar clientes</p>
                    </div>
                </Link>

                <Link to="/vendedor/lotes" style={{ textDecoration: 'none' }}>
                    <div className="card" style={{ textAlign: 'center', cursor: 'pointer' }}>
                        <h3>Consultar Lotes</h3>
                        <p>Ver lotes y fechas de vencimiento</p>
                    </div>
                </Link>

                {/* Ventas: dos opciones */}
                <Link to="/vendedor/ventas/nueva" style={{ textDecoration: 'none' }}>
                    <div className="card" style={{ textAlign: 'center', cursor: 'pointer' }}>
                        <h3>Venta Normal</h3>
                        <p>Seleccionar cliente</p>
                    </div>
                </Link>

                <Link to="/vendedor/ventas/rapida" style={{ textDecoration: 'none' }}>
                    <div className="card" style={{ textAlign: 'center', cursor: 'pointer' }}>
                        <h3>Venta Rápida</h3>
                        <p>Consumidor final</p>
                    </div>
                </Link>

                <Link to="/vendedor/ventas" style={{ textDecoration: 'none' }}>
                    <div className="card" style={{ textAlign: 'center', cursor: 'pointer' }}>
                        <h3>Mis Ventas</h3>
                        <p>Historial de ventas propias</p>
                    </div>
                </Link>

                <Link to="/vendedor/devoluciones/nueva" style={{ textDecoration: 'none' }}>
                    <div className="card" style={{ textAlign: 'center', cursor: 'pointer' }}>
                        <h3>Solicitar Devolución</h3>
                    </div>
                </Link>
            </div>
        </div>
    );
};

export default VendedorPanel;