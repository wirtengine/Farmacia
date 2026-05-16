import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import {
    listarClientes,
    crearCliente,
    actualizarCliente,
    desactivarCliente
} from '../services/clientes';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import './Clientes.css';
import {
    alertaConfirmacion,
    alertaExito,
    alertaError
} from '../alertas';
export default function Clientes() {
    const { user } = useAuth();
    const tienePermiso = user?.rol === 'ADMIN' || user?.rol === 'VENDEDOR';

    // Función auxiliar para redondear a 2 decimales
    const round2 = (num) => Math.round((num + Number.EPSILON) * 100) / 100;

    // Formato de moneda en Córdobas
    const formatCurrency = (value) => `C$ ${value.toFixed(2)}`;

    const [clientes, setClientes] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);

    // Estados para paginación
    const [currentPage, setCurrentPage] = useState(1);
    const rowsPerPage = 15;

    const [drawerOpen, setDrawerOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentId, setCurrentId] = useState(null);

    const [formData, setFormData] = useState({
        cedula: '',
        nombre: '',
        telefono: '',
        email: '',
        saldo: 0
    });

    useEffect(() => {
        cargarClientes();
    }, []);

    const cargarClientes = async () => {
        setLoading(true);
        try {
            const response = await listarClientes();
            const sorted = (response.data || []).sort((a, b) => b.id - a.id);
            setClientes(sorted);
        } catch (error) {
            alertaError('Error al cargar clientes');
        } finally {
            setLoading(false);
        }
    };

    const clientesFiltrados = useMemo(() => {
        const term = searchTerm.toLowerCase().trim();
        return clientes.filter(c =>
            !term ||
            c.nombre.toLowerCase().includes(term) ||
            c.cedula.includes(term)
        );
    }, [clientes, searchTerm]);

    // Resetear página al cambiar búsqueda
    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm]);

    // Paginación
    const totalPages = Math.ceil(clientesFiltrados.length / rowsPerPage);
    const paginatedClientes = useMemo(() => {
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        return clientesFiltrados.slice(start, end);
    }, [clientesFiltrados, currentPage]);

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

    const handleNuevo = () => {
        setEditMode(false);
        setFormData({
            cedula: '',
            nombre: '',
            telefono: '',
            email: '',
            saldo: 0
        });
        setDrawerOpen(true);
    };

    const handleEditar = (cliente) => {
        setEditMode(true);
        setCurrentId(cliente.id);
        setFormData({
            cedula: cliente.cedula,
            nombre: cliente.nombre,
            telefono: cliente.telefono || '',
            email: cliente.email || '',
            saldo: round2(Number(cliente.saldo) || 0)
        });
        setDrawerOpen(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.cedula.trim() || !formData.nombre.trim()) {
            alertaError('Cédula y Nombre son obligatorios');
            return;
        }

        setLoading(true);

        try {
            if (editMode) {
                await actualizarCliente(currentId, formData);
                alertaExito('Cliente actualizado con éxito');
            } else {
                await crearCliente(formData);
                alertaExito('Cliente registrado con éxito');
            }

            setDrawerOpen(false);
            cargarClientes();

        } catch (error) {
            alertaError('Error en la operación');
        } finally {
            setLoading(false);
        }
    };

    const handleDesactivar = async (id) => {
        const result = await alertaConfirmacion({
            titulo: 'Desactivar cliente',
            texto: '¿Está seguro de desactivar este cliente?',
            confirmar: 'Desactivar',
            cancelar: 'Cancelar',
            icono: 'warning'
        });

        if (!result.isConfirmed) return;

        try {
            await desactivarCliente(id);
            alertaExito('Cliente desactivado correctamente');
            cargarClientes();
        } catch (error) {
            alertaError('Error al desactivar');
        }
    };

    const imprimirReporteCliente = (cliente) => {
        const doc = new jsPDF();
        doc.setFontSize(18);
        doc.text('REPORTE DE CLIENTE', 14, 20);
        doc.setFontSize(12);
        doc.text(`Cédula: ${cliente.cedula}`, 14, 35);
        doc.text(`Nombre: ${cliente.nombre}`, 14, 45);
        doc.text(`Teléfono: ${cliente.telefono || 'N/A'}`, 14, 55);
        doc.text(`Email: ${cliente.email || 'N/A'}`, 14, 65);
        doc.text(`Saldo Actual: ${formatCurrency(round2(cliente.saldo || 0))}`, 14, 75);
        doc.text(`Estado: ${cliente.activo ? 'ACTIVO' : 'INACTIVO'}`, 14, 85);
        doc.save(`Reporte_${cliente.cedula}.pdf`);
    };

    const imprimirTodosLosClientes = () => {
        const doc = new jsPDF();
        doc.text('LISTADO GENERAL DE CLIENTES', 14, 15);
        const filas = clientesFiltrados.map(c => [
            c.cedula,
            c.nombre,
            c.telefono || '—',
            formatCurrency(round2(c.saldo || 0)),
            c.activo ? 'Activo' : 'Inactivo'
        ]);
        autoTable(doc, {
            startY: 25,
            head: [['Cédula', 'Nombre', 'Teléfono', 'Saldo', 'Estado']],
            body: filas
        });
        doc.save('Listado_Clientes.pdf');
    };

    return (
        <div className="module-container">
            {/* HEADER DE DOS FILAS */}
            <header className="module-header">
                <div className="header-title">
                    <h1>Gestión de Clientes</h1>
                    <p>Directorio de clientes y estados de cuenta</p>
                </div>
                <div className="header-actions-row">
                    <div className="search-box">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            placeholder="Buscar por cédula o nombre..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <button
                        className="btn-print"
                        onClick={imprimirTodosLosClientes}
                        title="Reporte General"
                    >
                        🖨️ PDF
                    </button>
                    {tienePermiso && (
                        <button
                            className="btn-primary-compact"
                            onClick={handleNuevo}
                        >
                            + Nuevo Cliente
                        </button>
                    )}
                </div>
            </header>

            <div className="table-card">
                <div className="table-responsive">
                    <table className="custom-table">
                        <thead>
                        <tr>
                            <th>Cédula</th>
                            <th>Nombre</th>
                            <th>Teléfono</th>
                            <th>Saldo</th>
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
                                    <td><div className="skeleton-cell" /></td>
                                    <td><div className="skeleton-cell" /></td>
                                </tr>
                            ))
                        ) : (
                            paginatedClientes.map((c, idx) => (
                                <tr key={c.id} className={`fade-in-row ${!c.activo ? 'row-inactive' : ''}`} style={{ animationDelay: `${idx * 0.05}s` }}>
                                    <td className="font-bold">{c.cedula}</td>
                                    <td>{c.nombre}</td>
                                    <td>{c.telefono || '—'}</td>
                                    <td className="font-semibold">{formatCurrency(round2(c.saldo || 0))}</td>
                                    <td>
                                        <span className={`status-pill ${c.activo ? 'active' : 'inactive'}`}>
                                            {c.activo ? 'Activo' : 'Inactivo'}
                                        </span>
                                    </td>
                                    <td className="text-center">
                                        <div className="action-buttons-group">
                                            <button
                                                className="btn-edit-icon"
                                                onClick={() => imprimirReporteCliente(c)}
                                                title="Ficha Cliente"
                                            >
                                                📄
                                            </button>
                                            {tienePermiso && c.activo && (
                                                <>
                                                    <button
                                                        className="btn-edit-icon"
                                                        onClick={() => handleEditar(c)}
                                                        title="Editar"
                                                    >
                                                        ✏️
                                                    </button>
                                                    <button
                                                        className="btn-delete-icon"
                                                        onClick={() => handleDesactivar(c.id)}
                                                        title="Desactivar"
                                                    >
                                                        🗑️
                                                    </button>
                                                </>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                    {!loading && clientesFiltrados.length === 0 && (
                        <div className="empty-state">No se encontraron clientes.</div>
                    )}
                </div>

                {/* Paginación */}
                {!loading && clientesFiltrados.length > 0 && (
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

            {/* DRAWER */}
            {drawerOpen && (
                <div className="drawer-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="drawer-panel" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header-compact">
                            <h2>{editMode ? 'Actualizar Cliente' : 'Registro de Cliente'}</h2>
                            <button className="close-btn-round" onClick={() => setDrawerOpen(false)}>×</button>
                        </div>
                        <form onSubmit={handleSubmit} className="drawer-body-scrollable">
                            <h4 className="section-divider">Datos Personales</h4>
                            <div className="field-group">
                                <label>Cédula / Identificación *</label>
                                <input
                                    type="text"
                                    value={formData.cedula}
                                    onChange={e => setFormData({...formData, cedula: e.target.value})}
                                    disabled={editMode}
                                    required
                                />
                            </div>
                            <div className="field-group">
                                <label>Nombre Completo *</label>
                                <input
                                    type="text"
                                    value={formData.nombre}
                                    onChange={e => setFormData({...formData, nombre: e.target.value})}
                                    required
                                />
                            </div>
                            <h4 className="section-divider">Contacto y Cuenta</h4>
                            <div className="field-grid-2">
                                <div className="field-group">
                                    <label>Teléfono</label>
                                    <input
                                        type="text"
                                        value={formData.telefono}
                                        onChange={e => setFormData({...formData, telefono: e.target.value})}
                                    />
                                </div>
                                <div className="field-group">
                                    <label>Saldo Inicial (C$)</label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        value={formData.saldo}
                                        onChange={e => setFormData({...formData, saldo: parseFloat(e.target.value) || 0})}
                                    />
                                </div>
                            </div>
                            <div className="field-group">
                                <label>Correo Electrónico</label>
                                <input
                                    type="email"
                                    value={formData.email}
                                    onChange={e => setFormData({...formData, email: e.target.value})}
                                />
                            </div>
                            <div className="drawer-footer-fixed">
                                <button type="button" className="btn-cancel" onClick={() => setDrawerOpen(false)}>Cancelar</button>
                                <button type="submit" className="btn-save-final" disabled={loading}>
                                    {loading ? 'Guardando...' : (editMode ? 'Guardar Cambios' : 'Registrar Cliente')}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}