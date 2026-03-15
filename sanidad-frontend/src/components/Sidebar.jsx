import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useState } from 'react';
import './Sidebar.css';

const MENU_SECTIONS = [
    {
        title: 'Principal',
        roles: ['ALL'],
        items: [
            { path: '/dashboard', icon: '🏠', label: 'Dashboard' }
        ],
    },
    {
        title: 'Inventario',
        roles: ['ALL'],
        items: [
            { path: '/medicamentos', icon: '💊', label: 'Medicamentos' },
            { path: '/lotes', icon: '📦', label: 'Lotes' },
            { path: '/proveedores', icon: '🚚', label: 'Proveedores' },
        ],
    },
    {
        title: 'Operaciones',
        roles: ['ALL'],
        items: [
            { path: '/ventas', icon: '💰', label: 'Ventas' },
            { path: '/devoluciones', icon: '🔄', label: 'Devoluciones' },
            { path: '/devoluciones-proveedor', icon: '📦', label: 'Devoluciones a Proveedores', roles: ['ADMIN'] },
            { path: '/clientes', icon: '👤', label: 'Clientes' },
        ],
    },
    {
        title: 'Administración',
        roles: ['ADMIN'],
        items: [
            { path: '/empleados', icon: '👥', label: 'Empleados' },
        ],
    },
];

export default function Sidebar() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const [showModal, setShowModal] = useState(false);
    const [collapsed, setCollapsed] = useState(false);

    const userRole = user?.rol || 'Portal';

    const confirmLogout = () => {
        logout();
        navigate('/login');
    };

    const toggleSidebar = () => {
        setCollapsed(!collapsed);
        document.body.classList.toggle('sidebar-collapsed', !collapsed);
    };

    return (
        <>
            <aside className={`pharmacy-sidebar ${collapsed ? 'collapsed' : ''}`}>
                <div className="sidebar-header">
                    <div className="admin-badge">
                        <div className="admin-avatar">
                            {userRole.charAt(0)}
                        </div>

                        {!collapsed && (
                            <div className="admin-info">
                                <span className="brand-name">FarmaSystem</span>
                                <span className="user-role">{userRole}</span>
                            </div>
                        )}
                    </div>

                    <button
                        className="sidebar-toggle"
                        onClick={toggleSidebar}
                        aria-label={collapsed ? "Expandir menú" : "Colapsar menú"}
                    >
                        {collapsed ? "▶" : "◀"}
                    </button>
                </div>

                <nav className="sidebar-links">
                    {MENU_SECTIONS.map((section) => {

                        const hasAccess =
                            section.roles.includes('ALL') ||
                            section.roles.includes(userRole);

                        if (!hasAccess) return null;

                        return (
                            <div key={section.title} className="menu-section">

                                {!collapsed && (
                                    <p className="section-title">
                                        {section.title}
                                    </p>
                                )}

                                {section.items
                                    .filter(item => !item.roles || item.roles.includes(userRole))
                                    .map((item) => (
                                        <NavLink
                                            key={item.path}
                                            to={item.path}
                                            className={({ isActive }) =>
                                                `link ${isActive ? 'active' : ''}`
                                            }
                                            title={collapsed ? item.label : ""}
                                        >
                                            <span
                                                role="img"
                                                aria-label={item.label}
                                                className="menu-icon"
                                            >
                                                {item.icon}
                                            </span>

                                            {!collapsed && (
                                                <span className="menu-label">
                                                    {item.label}
                                                </span>
                                            )}
                                        </NavLink>
                                    ))}
                            </div>
                        );
                    })}
                </nav>

                <div className="sidebar-footer">
                    <button
                        className="logout-trigger"
                        onClick={() => setShowModal(true)}
                        title={collapsed ? "Cerrar sesión" : ""}
                    >
                        <span role="img" aria-label="Cerrar sesión">🚪</span>
                        {!collapsed && "Cerrar sesión"}
                    </button>
                </div>
            </aside>

            {/* Modal Confirmación */}
            {showModal && (
                <div
                    className="modal-overlay"
                    onClick={() => setShowModal(false)}
                >
                    <div
                        className="confirm-modal"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="modal-icon-warning">!</div>

                        <h3>¿Desea cerrar sesión?</h3>
                        <p>Se cerrará el acceso al sistema actual.</p>

                        <div className="modal-actions">
                            <button
                                className="btn-cancel-modal"
                                onClick={() => setShowModal(false)}
                            >
                                Volver
                            </button>

                            <button
                                className="btn-confirm-modal"
                                onClick={confirmLogout}
                            >
                                Cerrar Sesión
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}