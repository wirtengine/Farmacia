import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import {
    listarProveedores,
    crearProveedor,
    actualizarProveedor,
    desactivarProveedor
} from '../services/proveedores';
import './Proveedores.css';

import {
    alertaConfirmacion,
    alertaExito,
    alertaError
} from '../alertas';

export default function Proveedores() {
    const { user } = useAuth();
    const esAdmin = user?.rol === 'ADMIN';

    const [proveedores, setProveedores] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);

    const [currentPage, setCurrentPage] = useState(1);
    const rowsPerPage = 15;

    const [drawerOpen, setDrawerOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentId, setCurrentId] = useState(null);

    const [formData, setFormData] = useState({
        ruc: '',
        nombre: '',
        telefono: '',
        email: ''
    });

    useEffect(() => {
        cargarProveedores();
    }, []);

    const cargarProveedores = async () => {
        setLoading(true);

        try {
            const response = await listarProveedores();
            const sorted = (response.data || []).sort((a, b) => b.id - a.id);
            setProveedores(sorted);
        } catch (error) {
            alertaError('Error al conectar con el servidor');
        } finally {
            setLoading(false);
        }
    };

    const proveedoresFiltrados = useMemo(() => {
        const term = searchTerm.toLowerCase().trim();

        return proveedores.filter(p => {
            if (!p.activo) return false;

            const nombreMatch = p.nombre.toLowerCase().includes(term);
            const rucMatch = p.ruc.toLowerCase().includes(term);

            return !term || nombreMatch || rucMatch;
        });
    }, [proveedores, searchTerm]);

    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm]);

    const totalPages = Math.ceil(proveedoresFiltrados.length / rowsPerPage);

    const paginatedProveedores = useMemo(() => {
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;

        return proveedoresFiltrados.slice(start, end);
    }, [proveedoresFiltrados, currentPage]);

    const goToPage = (page) => {
        if (page >= 1 && page <= totalPages) {
            setCurrentPage(page);
        }
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
                <button
                    key={1}
                    className="pagination-number"
                    onClick={() => goToPage(1)}
                >
                    1
                </button>
            );

            if (startPage > 2) {
                pages.push(
                    <span key="ellipsis-start" className="pagination-ellipsis">
                        ...
                    </span>
                );
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
                pages.push(
                    <span key="ellipsis-end" className="pagination-ellipsis">
                        ...
                    </span>
                );
            }

            pages.push(
                <button
                    key={totalPages}
                    className="pagination-number"
                    onClick={() => goToPage(totalPages)}
                >
                    {totalPages}
                </button>
            );
        }

        return pages;
    };

    const handleNuevo = () => {
        setEditMode(false);
        setCurrentId(null);

        setFormData({
            ruc: '',
            nombre: '',
            telefono: '',
            email: ''
        });

        setDrawerOpen(true);
    };

    const handleEditar = (prov) => {
        setEditMode(true);
        setCurrentId(prov.id);

        setFormData({
            ruc: prov.ruc,
            nombre: prov.nombre,
            telefono: prov.telefono || '',
            email: prov.email || ''
        });

        setDrawerOpen(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.ruc.trim() || !formData.nombre.trim()) {
            alertaError('RUC y Nombre son obligatorios');
            return;
        }

        setLoading(true);

        try {
            if (editMode) {
                await actualizarProveedor(currentId, formData);
                alertaExito('Proveedor actualizado correctamente');
            } else {
                await crearProveedor(formData);
                alertaExito('Proveedor registrado correctamente');
            }

            setDrawerOpen(false);
            cargarProveedores();

        } catch (error) {
            const errorMsg =
                error.response?.data?.message ||
                'Error en la operación';

            alertaError(errorMsg);

        } finally {
            setLoading(false);
        }
    };

    const handleDesactivar = async (id) => {
        const result = await alertaConfirmacion({
            titulo: 'Dar de baja proveedor',
            texto: '¿Desea dar de baja a este proveedor?',
            confirmar: 'Dar de baja',
            cancelar: 'Cancelar',
            icono: 'warning'
        });

        if (!result.isConfirmed) return;

        try {
            await desactivarProveedor(id);

            alertaExito('Proveedor desactivado');

            cargarProveedores();

        } catch (error) {
            alertaError('Error al desactivar');
        }
    };

    return (
        <div className="module-container">
            <header className="module-header">
                <div className="header-title">
                    <h1>Directorio de Proveedores</h1>
                    <p>Gestión de entidades comerciales y suministros</p>
                </div>

                <div className="header-actions-row">
                    <div className="search-box">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            placeholder="Buscar por nombre o RUC..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    {esAdmin && (
                        <button className="btn-primary-compact" onClick={handleNuevo}>
                            <span>+</span> Nuevo Proveedor
                        </button>
                    )}
                </div>
            </header>

            <div className="table-card">
                <div className="table-responsive">
                    <table className="custom-table">
                        <thead>
                        <tr>
                            <th>RUC</th>
                            <th>Razón Social / Nombre</th>
                            <th>Teléfono</th>
                            <th>Correo Electrónico</th>
                            <th className="text-center">Acciones</th>
                        </tr>
                        </thead>

                        <tbody>
                        {loading ? (
                            Array.from({ length: rowsPerPage }).map((_, idx) => (
                                <tr key={idx} className="skeleton-row">
                                    <td><div className="skeleton-cell" /></td>
                                    <td><div className="skeleton-cell" /></td>
                                    <td><div className="skeleton-cell" /></td>
                                    <td><div className="skeleton-cell" /></td>
                                    <td><div className="skeleton-cell" /></td>
                                </tr>
                            ))
                        ) : (
                            paginatedProveedores.map((p, idx) => (
                                <tr
                                    key={p.id}
                                    className="fade-in-row"
                                    style={{ animationDelay: `${idx * 0.05}s` }}
                                >
                                    <td className="font-mono text-muted">{p.ruc}</td>
                                    <td className="font-bold">{p.nombre}</td>
                                    <td>{p.telefono || '—'}</td>
                                    <td>{p.email || '—'}</td>

                                    <td className="text-center">
                                        {esAdmin ? (
                                            <div className="action-buttons-group">
                                                <button
                                                    className="btn-edit-icon"
                                                    onClick={() => handleEditar(p)}
                                                    title="Editar"
                                                >
                                                    ✏️
                                                </button>

                                                <button
                                                    className="btn-delete-icon"
                                                    onClick={() => handleDesactivar(p.id)}
                                                    title="Desactivar"
                                                >
                                                    🗑️
                                                </button>
                                            </div>
                                        ) : (
                                            <span className="badge-gray">Solo lectura</span>
                                        )}
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>

                    {!loading && proveedoresFiltrados.length === 0 && (
                        <div className="empty-state">
                            No se encontraron proveedores activos.
                        </div>
                    )}
                </div>

                {!loading && proveedoresFiltrados.length > 0 && (
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

            {drawerOpen && esAdmin && (
                <div className="drawer-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="drawer-panel" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header-compact">
                            <h2>
                                {editMode ? 'Editar Proveedor' : 'Nuevo Proveedor'}
                            </h2>

                            <button
                                className="close-btn-round"
                                onClick={() => setDrawerOpen(false)}
                            >
                                ×
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="drawer-body-scrollable">
                            <h4 className="section-divider">Datos Fiscales</h4>

                            <div className="field-group">
                                <label>RUC *</label>
                                <input
                                    type="text"
                                    value={formData.ruc}
                                    onChange={(e) =>
                                        setFormData({
                                            ...formData,
                                            ruc: e.target.value
                                        })
                                    }
                                    disabled={editMode}
                                    placeholder="Ej: J031000000"
                                    required
                                />
                            </div>

                            <div className="field-group">
                                <label>Nombre o Razón Social *</label>
                                <input
                                    type="text"
                                    value={formData.nombre}
                                    onChange={(e) =>
                                        setFormData({
                                            ...formData,
                                            nombre: e.target.value
                                        })
                                    }
                                    placeholder="Nombre oficial"
                                    required
                                />
                            </div>

                            <h4 className="section-divider">Contacto</h4>

                            <div className="field-group">
                                <label>Teléfono</label>
                                <input
                                    type="text"
                                    value={formData.telefono}
                                    onChange={(e) =>
                                        setFormData({
                                            ...formData,
                                            telefono: e.target.value
                                        })
                                    }
                                    placeholder="+505 0000-0000"
                                />
                            </div>

                            <div className="field-group">
                                <label>Correo Electrónico</label>
                                <input
                                    type="email"
                                    value={formData.email}
                                    onChange={(e) =>
                                        setFormData({
                                            ...formData,
                                            email: e.target.value
                                        })
                                    }
                                    placeholder="ejemplo@proveedor.com"
                                />
                            </div>

                            <div className="drawer-footer-fixed">
                                <button
                                    type="button"
                                    className="btn-cancel"
                                    onClick={() => setDrawerOpen(false)}
                                >
                                    Cancelar
                                </button>

                                <button
                                    type="submit"
                                    className="btn-save-final"
                                    disabled={loading}
                                >
                                    {loading
                                        ? 'Guardando...'
                                        : editMode
                                            ? 'Actualizar'
                                            : 'Guardar'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}