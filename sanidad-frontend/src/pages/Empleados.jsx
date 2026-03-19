import { useState, useEffect, useMemo } from 'react';
import { listarUsuarios, crearUsuario, actualizarUsuario } from '../services/usuarios';
import './Empleados.css';

export default function Empleados() {
    // --- ESTADOS ---
    const [usuarios, setUsuarios] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [message, setMessage] = useState({ text: '', type: '' });
    const [loading, setLoading] = useState(false);

    // Paginación
    const [currentPage, setCurrentPage] = useState(1);
    const rowsPerPage = 15;

    // Estado para el Drawer (Panel Lateral)
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState(null);
    const [formData, setFormData] = useState({
        username: '',
        password: '',
        rol: 'VENDEDOR'
    });

    useEffect(() => {
        cargarUsuarios();
    }, []);

    const cargarUsuarios = async () => {
        setLoading(true);
        try {
            const response = await listarUsuarios();
            // Ordenar por ID descendente (último registrado primero)
            const sorted = (response.data || []).sort((a, b) => b.id - a.id);
            setUsuarios(sorted);
            if (sorted.length === 0) {
                setMessage({ text: 'No hay usuarios registrados', type: 'info' });
            }
        } catch (error) {
            setMessage({ text: 'Error al conectar con el servidor', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    // Filtrado
    const usuariosFiltrados = useMemo(() => {
        const term = searchTerm.toLowerCase().trim();
        return usuarios.filter(u =>
            u.username.toLowerCase().includes(term) ||
            u.rol.toLowerCase().includes(term)
        );
    }, [usuarios, searchTerm]);

    // Resetear página al cambiar filtro
    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm]);

    // Paginación
    const totalPages = Math.ceil(usuariosFiltrados.length / rowsPerPage);
    const paginatedUsuarios = useMemo(() => {
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        return usuariosFiltrados.slice(start, end);
    }, [usuariosFiltrados, currentPage]);

    const goToPage = (page) => {
        if (page >= 1 && page <= totalPages) setCurrentPage(page);
    };

    const renderPageNumbers = () => {
        if (totalPages <= 1) return null;
        const pages = [];
        const maxVisible = 5;
        let startPage = Math.max(1, currentPage - 2);
        let endPage = Math.min(totalPages, currentPage + 2);

        if (currentPage <= 3) {
            startPage = 1;
            endPage = Math.min(totalPages, maxVisible);
        }
        if (currentPage > totalPages - 3) {
            startPage = Math.max(1, totalPages - maxVisible + 1);
            endPage = totalPages;
        }

        if (startPage > 1) {
            pages.push(
                <button key={1} className="pagination-number" onClick={() => goToPage(1)}>1</button>
            );
            if (startPage > 2) {
                pages.push(<span key="ellipsis-start" className="pagination-ellipsis">...</span>);
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            pages.push(
                <button
                    key={i}
                    className={`pagination-number ${currentPage === i ? 'active' : ''}`}
                    onClick={() => goToPage(i)}
                >
                    {i}
                </button>
            );
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                pages.push(<span key="ellipsis-end" className="pagination-ellipsis">...</span>);
            }
            pages.push(
                <button key={totalPages} className="pagination-number" onClick={() => goToPage(totalPages)}>
                    {totalPages}
                </button>
            );
        }
        return pages;
    };

    const handleOpenCreate = () => {
        setEditMode(false);
        setSelectedUserId(null);
        setFormData({ username: '', password: '', rol: 'VENDEDOR' });
        setIsModalOpen(true);
    };

    const handleOpenEdit = (usuario) => {
        setEditMode(true);
        setSelectedUserId(usuario.id);
        setFormData({
            username: usuario.username,
            password: '',
            rol: usuario.rol
        });
        setIsModalOpen(true);
    };

    const handleSave = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            if (editMode) {
                const dataToUpdate = {
                    rol: formData.rol,
                    password: formData.password || undefined
                };
                await actualizarUsuario(selectedUserId, dataToUpdate);
                setMessage({ text: 'Usuario actualizado con éxito', type: 'success' });
            } else {
                await crearUsuario(formData);
                setMessage({ text: 'Usuario creado con éxito', type: 'success' });
            }
            setIsModalOpen(false);
            cargarUsuarios();
        } catch (error) {
            setMessage({ text: 'Error al procesar la solicitud', type: 'error' });
        } finally {
            setLoading(false);
            setTimeout(() => setMessage({ text: '', type: '' }), 3000);
        }
    };

    return (
        <div className="module-container">
            {/* HEADER DE DOS FILAS */}
            <header className="module-header">
                <div className="header-title">
                    <h1>Gestión de Personal</h1>
                    <p>Administra los accesos y roles de la farmacia</p>
                </div>
                <div className="header-actions-row">
                    <div className="search-bar-container">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            className="search-input-main"
                            placeholder="Buscar por usuario o rol..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <button className="btn-primary-compact" onClick={handleOpenCreate}>
                        <span>+</span> Nuevo Usuario
                    </button>
                </div>
            </header>

            {/* Alertas dinámicas */}
            {message.text && (
                <div className={`alert-banner ${message.type}`}>
                    <span>{message.text}</span>
                    <button className="close-alert" onClick={() => setMessage({ text: '', type: '' })}>×</button>
                </div>
            )}

            {/* Tabla de Datos */}
            <div className="table-wrapper">
                <div className="table-responsive">
                    <table className="modern-table">
                        <thead>
                        <tr>
                            <th>Usuario</th>
                            <th>Rol / Permisos</th>
                            <th>Estado</th>
                            <th className="text-center">Acciones</th>
                        </tr>
                        </thead>
                        <tbody>
                        {loading ? (
                            // Skeleton loader
                            Array.from({ length: rowsPerPage }).map((_, idx) => (
                                <tr key={idx} className="skeleton-row">
                                    <td><div className="skeleton-cell" /></td>
                                    <td><div className="skeleton-cell" /></td>
                                    <td><div className="skeleton-cell" /></td>
                                    <td><div className="skeleton-cell" /></td>
                                </tr>
                            ))
                        ) : (
                            paginatedUsuarios.map((u, idx) => (
                                <tr key={u.id} className="fade-in-row" style={{ animationDelay: `${idx * 0.05}s` }}>
                                    <td className="user-td">
                                        <div className="avatar-small">
                                            {u.username.charAt(0).toUpperCase()}
                                        </div>
                                        <span className="username-text">{u.username}</span>
                                    </td>
                                    <td>
                                        <span className={`role-tag ${u.rol.toLowerCase()}`}>
                                            {u.rol}
                                        </span>
                                    </td>
                                    <td>
                                        <span className="status-badge active">
                                            <span className="dot"></span> Activo
                                        </span>
                                    </td>
                                    <td className="text-center">
                                        <button className="btn-edit-icon" onClick={() => handleOpenEdit(u)} title="Editar">
                                            ✏️
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                    {!loading && usuariosFiltrados.length === 0 && (
                        <div className="empty-state">No se encontraron usuarios.</div>
                    )}
                </div>

                {/* Paginación */}
                {!loading && usuariosFiltrados.length > 0 && (
                    <div className="pagination-container">
                        <button
                            className="pagination-btn"
                            onClick={() => goToPage(currentPage - 1)}
                            disabled={currentPage === 1}
                        >
                            ← Anterior
                        </button>
                        <div className="pagination-pages">
                            {renderPageNumbers()}
                        </div>
                        <button
                            className="pagination-btn"
                            onClick={() => goToPage(currentPage + 1)}
                            disabled={currentPage === totalPages}
                        >
                            Siguiente →
                        </button>
                    </div>
                )}
            </div>

            {/* --- DRAWER (PANEL LATERAL) --- */}
            {isModalOpen && (
                <div className="drawer-overlay" onClick={() => setIsModalOpen(false)}>
                    <div className="drawer-panel" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header-compact">
                            <h2>{editMode ? 'Editar Perfil' : 'Nuevo Integrante'}</h2>
                            <button className="close-btn-round" onClick={() => setIsModalOpen(false)}>×</button>
                        </div>

                        <form onSubmit={handleSave} className="drawer-body-scrollable">
                            <div className="field-group">
                                <label>Nombre de Usuario</label>
                                <input
                                    type="text"
                                    value={formData.username}
                                    onChange={(e) => setFormData({...formData, username: e.target.value})}
                                    disabled={editMode}
                                    className={editMode ? "input-disabled" : ""}
                                    placeholder="Ej. mgarcia"
                                    required={!editMode}
                                />
                            </div>

                            <div className="field-group">
                                <label>
                                    {editMode ? 'Nueva Contraseña (opcional)' : 'Contraseña de Acceso'}
                                </label>
                                <input
                                    type="password"
                                    value={formData.password}
                                    onChange={(e) => setFormData({...formData, password: e.target.value})}
                                    placeholder={editMode ? "Dejar vacío para mantener" : "Mínimo 6 caracteres"}
                                    required={!editMode}
                                />
                            </div>

                            <div className="field-group">
                                <label>Rol Asignado</label>
                                <div className="role-selector">
                                    {['VENDEDOR', 'ADMIN'].map(role => (
                                        <button
                                            key={role}
                                            type="button"
                                            className={formData.rol === role ? 'active' : ''}
                                            onClick={() => setFormData({...formData, rol: role})}
                                        >
                                            {role}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div className="drawer-footer-fixed">
                                <button type="button" className="btn-cancel" onClick={() => setIsModalOpen(false)}>
                                    Cancelar
                                </button>
                                <button type="submit" className="btn-save-final" disabled={loading}>
                                    {loading ? 'Guardando...' : (editMode ? 'Actualizar' : 'Guardar')}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}