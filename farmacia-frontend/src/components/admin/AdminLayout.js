import React, { useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { FiLogOut, FiPackage, FiUsers, FiShoppingCart, FiBarChart2, FiTruck } from 'react-icons/fi';


const AdminLayout = () => {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <div style={{ display: 'flex', minHeight: '100vh' }}>
            {/* Sidebar */}
            <div style={{ width: '250px', backgroundColor: '#1f2937', color: 'white', padding: '20px' }}>
                <h3 style={{ marginBottom: '30px', textAlign: 'center' }}>Farmacia Admin</h3>
                <nav>
                    <ul style={{ listStyle: 'none', padding: 0 }}>
                        <li style={{ marginBottom: '10px' }}>
                            <Link to="/admin/dashboard" style={{ color: 'white', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '10px', padding: '8px', borderRadius: '4px' }}>
                                <FiBarChart2 /> Dashboard
                            </Link>
                        </li>
                        <li style={{ marginBottom: '10px' }}>
                            <Link to="/admin/medicamentos" style={{ color: 'white', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '10px', padding: '8px', borderRadius: '4px' }}>
                                <FiPackage /> Medicamentos
                            </Link>
                        </li>
                        <li style={{ marginBottom: '10px' }}>
                            <Link to="/admin/proveedores" style={{ color: 'white', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '10px', padding: '8px', borderRadius: '4px' }}>
                                {/* eslint-disable-next-line react/jsx-no-undef */}
                                <FiTruck /> Proveedores
                            </Link>
                        </li>
                        <li style={{ marginBottom: '10px' }}>
                            <Link to="/admin/usuarios" style={{ color: 'white', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '10px', padding: '8px', borderRadius: '4px' }}>
                                <FiUsers /> Usuarios
                            </Link>
                        </li>
                        <li style={{ marginBottom: '10px' }}>
                            <Link to="/admin/ventas" style={{ color: 'white', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '10px', padding: '8px', borderRadius: '4px' }}>
                                <FiShoppingCart /> Ventas
                            </Link>
                        </li>
                    </ul>
                </nav>
                <div style={{ marginTop: 'auto', paddingTop: '20px' }}>
                    <button onClick={handleLogout} style={{ backgroundColor: 'transparent', border: '1px solid white', color: 'white', padding: '8px', width: '100%', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }}>
                        <FiLogOut /> Cerrar sesión
                    </button>
                </div>

            </div>

            {/* Contenido principal */}
            <div style={{ flex: 1, backgroundColor: '#f3f4f6', padding: '20px' }}>
                <Outlet />
            </div>
        </div>
    );
};

export default AdminLayout;