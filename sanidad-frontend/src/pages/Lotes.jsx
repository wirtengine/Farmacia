import { useState, useEffect, useMemo } from 'react';
import { listarLotes, crearLote, desactivarLote } from '../services/lotes';
import { listarMedicamentos } from '../services/medicamentos';
import { listarProveedores } from '../services/proveedores';
import { useAuth } from '../context/AuthContext';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import './Lotes.css';

export default function Lotes() {
    const { user } = useAuth();
    const isAdmin = user?.rol === 'ADMIN';

    // Estados de Datos
    const [lotes, setLotes] = useState([]);
    const [medicamentos, setMedicamentos] = useState([]);
    const [proveedores, setProveedores] = useState([]);

    // Estados de Filtros
    const [searchTerm, setSearchTerm] = useState('');
    const [filtroStock, setFiltroStock] = useState('todos');

    // Estados de UI
    const [message, setMessage] = useState({ text: '', type: '' });
    const [loading, setLoading] = useState(false);
    const [drawerOpen, setDrawerOpen] = useState(false);

    // Estados para paginación
    const [currentPage, setCurrentPage] = useState(1);
    const rowsPerPage = 15; // Mismo número que en medicamentos

    const [formData, setFormData] = useState({
        fechaFabricacion: '',
        fechaVencimiento: '',
        proveedorId: '',
        factura: '',
        detalles: []
    });

    useEffect(() => {
        cargarDatos();
    }, []);

    const cargarDatos = async () => {
        setLoading(true);
        try {
            const [resLotes, resMeds, resProvs] = await Promise.all([
                listarLotes(),
                listarMedicamentos(),
                listarProveedores()
            ]);
            // Ordenar lotes por ID descendente (último ingresado primero)
            const sortedLotes = (resLotes.data || []).sort((a, b) => b.id - a.id);
            setLotes(sortedLotes);
            setMedicamentos(resMeds.data || []);
            setProveedores(resProvs.data || []);
        } catch (error) {
            setMessage({ text: 'Error al cargar datos del sistema', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    // Filtrado y búsqueda
    const lotesFiltrados = useMemo(() => {
        const term = searchTerm.toLowerCase().trim();

        return lotes.filter(l => {
            // Solo mostrar activos para usuarios no admin
            if (!isAdmin && !l.activo) return false;

            const proveedor = proveedores.find(p => p.id === l.proveedorId);
            const nombreProveedor = proveedor?.nombre.toLowerCase() || '';
            const facturaMatch = l.factura?.toLowerCase().includes(term);
            const proveedorMatch = nombreProveedor.includes(term);

            // Buscar en medicamentos del detalle
            const medicamentoMatch = l.detalles?.some(d => {
                const med = medicamentos.find(m => m.id === d.medicamentoId);
                return med?.nombre.toLowerCase().includes(term);
            });

            const coincideBusqueda = !term || facturaMatch || proveedorMatch || medicamentoMatch;

            const totalStock = l.detalles?.reduce((acc, d) => acc + (d.cantidad || 0), 0) || 0;

            if (filtroStock === 'stock') {
                return coincideBusqueda && totalStock > 0 && l.activo;
            }
            if (filtroStock === 'agotado') {
                return coincideBusqueda && totalStock === 0 && l.activo;
            }
            // 'todos' - mostramos activos (con o sin stock)
            return coincideBusqueda && l.activo;
        });
    }, [lotes, searchTerm, proveedores, medicamentos, filtroStock, isAdmin]);

    // Resetear página al cambiar filtros
    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm, filtroStock]);

    // Paginación
    const totalPages = Math.ceil(lotesFiltrados.length / rowsPerPage);
    const paginatedLotes = useMemo(() => {
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        return lotesFiltrados.slice(start, end);
    }, [lotesFiltrados, currentPage]);

    const goToPage = (page) => {
        if (page >= 1 && page <= totalPages) {
            setCurrentPage(page);
        }
    };

    // Renderizado de números de página con elipsis
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

    const handleDesactivar = async (id) => {
        if (!window.confirm('¿Está seguro de desactivar este lote?')) return;
        setLoading(true);
        try {
            await desactivarLote(id);
            setMessage({ text: 'Lote desactivado correctamente', type: 'success' });
            cargarDatos();
        } catch (error) {
            setMessage({ text: 'Error al desactivar el lote', type: 'error' });
        } finally {
            setLoading(false);
            setTimeout(() => setMessage({ text: '', type: '' }), 3000);
        }
    };

    const generarCodigoFactura = () => {
        const fecha = new Date();
        const year = fecha.getFullYear();
        const mes = String(fecha.getMonth() + 1).padStart(2, '0');
        const random = Math.floor(1000 + Math.random() * 9000);
        return `FAC-${year}${mes}-${random}`;
    };

    const handleNuevo = () => {
        setFormData({
            fechaFabricacion: new Date().toISOString().split('T')[0],
            fechaVencimiento: '',
            proveedorId: '',
            factura: generarCodigoFactura(),
            detalles: [{ medicamentoId: '', cantidad: 1 }]
        });
        setDrawerOpen(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        // Validaciones básicas
        if (!formData.proveedorId) {
            alert('Seleccione un proveedor');
            return;
        }
        if (!formData.fechaVencimiento) {
            alert('Ingrese fecha de vencimiento');
            return;
        }
        if (formData.detalles.length === 0 || formData.detalles.some(d => !d.medicamentoId || d.cantidad < 1)) {
            alert('Complete al menos un medicamento con cantidad válida');
            return;
        }
        setLoading(true);
        try {
            await crearLote(formData);
            setDrawerOpen(false);
            setMessage({ text: 'Lote registrado correctamente', type: 'success' });
            cargarDatos();
        } catch (error) {
            alert('Error al guardar lote');
        } finally {
            setLoading(false);
        }
    };

    const imprimirLote = async (lote) => {
        const doc = new jsPDF();
        const proveedor = proveedores.find(p => p.id === lote.proveedorId)?.nombre || 'Desconocido';
        doc.setFontSize(16);
        doc.text(`Comprobante de Lote: ${lote.factura}`, 14, 20);
        doc.setFontSize(10);
        doc.text(`Proveedor: ${proveedor}`, 14, 30);
        doc.text(`Fecha de vencimiento: ${lote.fechaVencimiento}`, 14, 38);

        const filas = lote.detalles.map(d => {
            const med = medicamentos.find(m => m.id === d.medicamentoId);
            return [med?.nombre || 'S/N', d.cantidad];
        });
        autoTable(doc, {
            startY: 45,
            head: [['Medicamento', 'Cantidad']],
            body: filas,
            theme: 'striped',
            headStyles: { fillColor: [37, 99, 235] }
        });
        doc.save(`Lote_${lote.factura}.pdf`);
    };

    return (
        <div className="module-container">
            <header className="module-header">
                {/* Primera fila: título y subtítulo */}
                <div className="header-title">
                    <h1>Inventario de Lotes</h1>
                    <p>Filtra por factura, proveedor o medicamento</p>
                </div>

                {/* Segunda fila: acciones (filtros, buscador, botón) */}
                <div className="header-actions-row">
                    <div className="stock-filter-group">
                        <button
                            className={`btn-filter ${filtroStock === 'todos' ? 'active' : ''}`}
                            onClick={() => setFiltroStock('todos')}
                        >
                            Todos
                        </button>
                        <button
                            className={`btn-filter ${filtroStock === 'stock' ? 'active' : ''}`}
                            onClick={() => setFiltroStock('stock')}
                        >
                            En Stock
                        </button>
                        <button
                            className={`btn-filter ${filtroStock === 'agotado' ? 'active' : ''}`}
                            onClick={() => setFiltroStock('agotado')}
                        >
                            Agotados
                        </button>
                    </div>

                    <div className="search-box">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            placeholder="Buscar factura, proveedor o producto..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    {isAdmin && (
                        <button className="btn-primary-compact" onClick={handleNuevo}>
                            ＋ Registrar Entrada
                        </button>
                    )}
                </div>
            </header>

            {message.text && (
                <div className={`alert-banner ${message.type}`}>
                    {message.text}
                    <button onClick={() => setMessage({ text: '', type: '' })}>×</button>
                </div>
            )}

            <div className="table-card">
                <div className="table-responsive">
                    <table className="custom-table">
                        <thead>
                        <tr>
                            <th>Factura Ref.</th>
                            <th>Proveedor</th>
                            <th>Medicamentos</th>
                            <th>Vencimiento</th>
                            <th>Estado</th>
                            <th className="text-center">Acciones</th>
                        </tr>
                        </thead>
                        <tbody>
                        {loading ? (
                            // Skeleton loading
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
                            paginatedLotes.map((l, idx) => {
                                const totalStock = l.detalles?.reduce((acc, d) => acc + (d.cantidad || 0), 0) || 0;
                                const tieneStock = totalStock > 0;
                                const vencido = new Date(l.fechaVencimiento) < new Date();
                                return (
                                    <tr key={l.id} className={`fade-in-row ${!l.activo ? 'row-inactive' : ''}`} style={{ animationDelay: `${idx * 0.05}s` }}>
                                        <td className="font-bold">{l.factura}</td>
                                        <td>{proveedores.find(p => p.id === l.proveedorId)?.nombre || '—'}</td>
                                        <td>
                                            <div className="items-chip-container">
                                                {l.detalles?.map((det, i) => (
                                                    <span key={i} className="med-chip">
                                                            {medicamentos.find(m => m.id === det.medicamentoId)?.nombre || 'S/N'}
                                                        <small>x{det.cantidad}</small>
                                                        </span>
                                                ))}
                                            </div>
                                        </td>
                                        <td className={vencido ? 'text-danger' : ''}>
                                            {l.fechaVencimiento}
                                            {vencido && <span className="vencido-badge"> Vencido</span>}
                                        </td>
                                        <td>
                                                <span className={`status-pill ${l.activo ? (tieneStock ? 'active' : 'agotado') : 'inactive'}`}>
                                                    {l.activo ? (tieneStock ? 'En Stock' : 'Agotado') : 'Inactivo'}
                                                </span>
                                        </td>
                                        <td className="text-center">
                                            <div className="action-buttons-group">
                                                <button className="btn-edit-icon" title="Imprimir" onClick={() => imprimirLote(l)}>📄</button>
                                                {isAdmin && l.activo && (
                                                    <button className="btn-delete-icon" title="Desactivar" onClick={() => handleDesactivar(l.id)}>🗑️</button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })
                        )}
                        </tbody>
                    </table>
                    {!loading && lotesFiltrados.length === 0 && (
                        <div className="empty-state">No se encontraron lotes que coincidan con los criterios.</div>
                    )}
                </div>

                {/* Paginación */}
                {!loading && lotesFiltrados.length > 0 && (
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

            {/* Drawer */}
            {drawerOpen && (
                <div className="drawer-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="drawer-panel" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header-compact">
                            <h2>Nuevo Lote</h2>
                            <button className="close-btn-round" onClick={() => setDrawerOpen(false)}>×</button>
                        </div>
                        <form onSubmit={handleSubmit} className="drawer-body-scrollable">
                            <div className="field-group">
                                <label>Factura</label>
                                <input type="text" value={formData.factura} readOnly className="input-readonly" />
                            </div>
                            <div className="field-group">
                                <label>Proveedor *</label>
                                <select value={formData.proveedorId} onChange={e => setFormData({...formData, proveedorId: e.target.value})} required>
                                    <option value="">Seleccione...</option>
                                    {proveedores.map(p => <option key={p.id} value={p.id}>{p.nombre}</option>)}
                                </select>
                            </div>
                            <div className="field-grid-2">
                                <div className="field-group">
                                    <label>Fabricación</label>
                                    <input type="date" value={formData.fechaFabricacion} onChange={e => setFormData({...formData, fechaFabricacion: e.target.value})} />
                                </div>
                                <div className="field-group">
                                    <label>Vencimiento *</label>
                                    <input type="date" value={formData.fechaVencimiento} onChange={e => setFormData({...formData, fechaVencimiento: e.target.value})} required />
                                </div>
                            </div>

                            <h4 className="section-divider">Productos</h4>
                            {formData.detalles.map((det, index) => (
                                <div className="item-entry-row" key={index}>
                                    <select
                                        className="flex-2"
                                        value={det.medicamentoId}
                                        onChange={e => {
                                            const newDet = [...formData.detalles];
                                            newDet[index].medicamentoId = e.target.value;
                                            setFormData({...formData, detalles: newDet});
                                        }}
                                        required
                                    >
                                        <option value="">Medicamento...</option>
                                        {medicamentos.map(m => <option key={m.id} value={m.id}>{m.nombre}</option>)}
                                    </select>
                                    <input
                                        type="number"
                                        className="flex-1"
                                        value={det.cantidad}
                                        onChange={e => {
                                            const newDet = [...formData.detalles];
                                            newDet[index].cantidad = parseInt(e.target.value) || 0;
                                            setFormData({...formData, detalles: newDet});
                                        }}
                                        min="1"
                                        required
                                    />
                                    <button
                                        type="button"
                                        className="btn-delete-small"
                                        onClick={() => setFormData({...formData, detalles: formData.detalles.filter((_, i) => i !== index)})}
                                    >
                                        ×
                                    </button>
                                </div>
                            ))}
                            <button
                                type="button"
                                className="btn-add-item"
                                onClick={() => setFormData({...formData, detalles: [...formData.detalles, {medicamentoId: '', cantidad: 1}]})}
                            >
                                + Añadir Medicamento
                            </button>

                            <div className="drawer-footer-fixed">
                                <button type="button" className="btn-cancel" onClick={() => setDrawerOpen(false)}>Cancelar</button>
                                <button type="submit" className="btn-save-final" disabled={loading}>
                                    {loading ? 'Guardando...' : 'Guardar Entrada'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}