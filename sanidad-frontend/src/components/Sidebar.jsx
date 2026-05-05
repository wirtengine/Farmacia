import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useState } from 'react';
import './Sidebar.css';

const MENU_SECTIONS = [
    {
        title: 'Principal',
        roles: ['ALL'],
        items: [
            { path: '/dashboard', icon: '🏠', label: 'Dashboard' },

            // 🔔 Alertas
            { path: '/alerts', icon: '🔔', label: 'Alertas' },

            // 💡 Recomendaciones
            { path: '/recommendations', icon: '💡', label: 'Recomendaciones' },
        ],
    },
    {
        title: 'Inventario',
        roles: ['ALL'],
        items: [
            { path: '/medicamentos', icon: '💊', label: 'Medicamentos' },
            { path: '/lotes', icon: '📦', label: 'Lotes' },
            { path: '/proveedores', icon: '🚚', label: 'Proveedores' },
            {
                path: '/ubicaciones',
                icon: '📦',
                label: 'Ubicaciones de estante',
                roles: ['ADMIN']
            },
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

            // ✅ Control de pérdidas
            { path: '/perdidas', icon: '📉', label: 'Control de Pérdidas' },

            // 🆕 NUEVO: Recetas (solo FARMACEUTICO y ADMIN)
            { path: '/recetas', icon: '📄', label: 'Recetas', roles: ['FARMACEUTICO', 'ADMIN'] },
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
                                    <p className="section-title">{section.title}</p>
                                )}

                                {section.items
                                    .filter(
                                        (item) =>
                                            !item.roles || item.roles.includes(userRole)
                                    )
                                    .map((item) => (
                                        <NavLink
                                            key={item.path}
                                            to={item.path}
                                            className={({ isActive }) =>
                                                isActive ? 'link active' : 'link'
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
                        <span role="img" aria-label="Cerrar sesión">
                            🚪
                        </span>
                        {!collapsed && "Cerrar sesión"}
                    </button>
                </div>
            </aside>

            {showModal && (
                <div
                    className="modal-overlay"
                    onClick={() => setShowModal(false)}
                >
                    <div
                        className="logout-modal"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="modal-icon">⚠️</div>
                        <h3>¿Cerrar sesión?</h3>
                        <p>
                            Estás a punto de salir del sistema. ¿Deseas continuar?
                        </p>
                        <div className="modal-actions">
                            <button
                                className="btn-cancel"
                                onClick={() => setShowModal(false)}
                            >
                                Cancelar
                            </button>
                            <button
                                className="btn-danger"
                                onClick={confirmLogout}
                            >
                                Cerrar sesión
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}