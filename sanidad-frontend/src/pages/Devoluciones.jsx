import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { listarDevoluciones, solicitarDevolucion, aprobarDevolucion } from '../services/devoluciones';
import { listarVentas, obtenerVenta } from '../services/ventas';
import './Devoluciones.css';

export default function Devoluciones() {
    const { user } = useAuth();
    const esAdmin = user?.rol === 'ADMIN';
    const usuarioId = user?.id;

    // Función auxiliar para redondear a 2 decimales
    const round2 = (num) => Math.round((num + Number.EPSILON) * 100) / 100;

    // Datos
    const [devoluciones, setDevoluciones] = useState([]);
    const [loading, setLoading] = useState(false);

    // Filtros
    const [searchTerm, setSearchTerm] = useState('');

    // Paginación
    const [currentPage, setCurrentPage] = useState(1);
    const rowsPerPage = 15;

    // Estados del Drawer
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [ventas, setVentas] = useState([]);
    const [ventaSeleccionada, setVentaSeleccionada] = useState(null);
    const [itemsDevolucion, setItemsDevolucion] = useState([]);
    const [motivo, setMotivo] = useState('');
    const [busquedaVenta, setBusquedaVenta] = useState('');

    // Estado para la Factura de Impresión
    const [ticketPrint, setTicketPrint] = useState(null);

    useEffect(() => { cargarDevoluciones(); }, []);

    const cargarDevoluciones = async () => {
        setLoading(true);
        try {
            const res = await listarDevoluciones();
            // Ordenar por ID descendente (último registrado primero)
            const sorted = (res.data || []).sort((a, b) => b.id - a.id);
            setDevoluciones(sorted);
        } catch (error) { console.error(error); }
        finally { setLoading(false); }
    };

    // Filtrado por número de devolución o factura
    const devolucionesFiltradas = useMemo(() => {
        const term = searchTerm.toLowerCase().trim();
        return devoluciones.filter(d =>
            !term ||
            (d.numeroDevolucion && d.numeroDevolucion.toLowerCase().includes(term)) ||
            (d.numeroFactura && d.numeroFactura.toLowerCase().includes(term))
        );
    }, [devoluciones, searchTerm]);

    // Resetear página al cambiar filtro
    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm]);

    // Paginación
    const totalPages = Math.ceil(devolucionesFiltradas.length / rowsPerPage);
    const paginatedDevoluciones = useMemo(() => {
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        return devolucionesFiltradas.slice(start, end);
    }, [devolucionesFiltradas, currentPage]);

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

    const handleAprobarAccion = async (devolucionId, aprobado) => {
        let motivoRechazo = null;
        if (!aprobado) {
            motivoRechazo = prompt('Ingrese el motivo del rechazo:');
            if (!motivoRechazo) return;
        } else {
            if (!window.confirm('¿Aprobar devolución y retornar productos al inventario?')) return;
        }

        try {
            await aprobarDevolucion({ devolucionId, aprobadoPorId: usuarioId, aprobada: aprobado, motivoRechazo });
            cargarDevoluciones();
        } catch (error) { alert('Error al procesar'); }
    };

    const handleImprimir = (devolucion) => {
        setTicketPrint(devolucion);
        setTimeout(() => {
            window.print();
        }, 500);
    };

    const handleNuevaDevolucion = async () => {
        setVentaSeleccionada(null);
        setItemsDevolucion([]);
        setMotivo('');
        setBusquedaVenta('');
        try {
            const res = await listarVentas();
            setVentas(res.data);
            setDrawerOpen(true);
        } catch (error) { alert('Error al cargar ventas'); }
    };

    const handleSeleccionarVenta = async (venta) => {
        try {
            const res = await obtenerVenta(venta.id);
            const inicial = res.data.detalles.map(d => ({
                ventaDetalleId: d.id,
                producto: d.medicamentoNombre,
                cantidadMax: d.cantidad,
                cantidadDevuelta: 0,
                precioUnitario: d.precioUnitario
            }));
            setItemsDevolucion(inicial);
            setVentaSeleccionada(res.data);
        } catch (error) { alert('Error al cargar detalles'); }
    };

    const actualizarCantidad = (id, val) => {
        setItemsDevolucion(prev => prev.map(item =>
            item.ventaDetalleId === id ? { ...item, cantidadDevuelta: Math.min(val, item.cantidadMax) } : item
        ));
    };

    const handleSolicitar = async () => {
        const detalles = itemsDevolucion.filter(i => i.cantidadDevuelta > 0)
            .map(i => ({ ventaDetalleId: i.ventaDetalleId, cantidadDevuelta: i.cantidadDevuelta }));

        if (detalles.length === 0 || !motivo.trim()) return alert('Complete los datos');

        try {
            await solicitarDevolucion({ ventaId: ventaSeleccionada.id, solicitadoPorId: usuarioId, motivo, detalles });
            setDrawerOpen(false);
            cargarDevoluciones();
        } catch (error) { alert('Error al procesar'); }
    };

    const ventasFiltradas = useMemo(() => {
        return ventas.filter(v => v.numeroFactura.toLowerCase().includes(busquedaVenta.toLowerCase()));
    }, [ventas, busquedaVenta]);

    return (
        <div className="module-container">
            {/* HEADER DE DOS FILAS */}
            <header className="module-header">
                <div className="header-title">
                    <h1>Devoluciones</h1>
                    <p className="vendedor-name">{esAdmin ? 'Panel de Control Administrativo' : 'Gestión de Solicitudes'}</p>
                </div>
                <div className="header-actions-row">
                    <div className="search-bar-container">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            className="search-input-main"
                            placeholder="Buscar devolución o factura..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <button className="btn-add-venta" onClick={handleNuevaDevolucion}>
                        ＋ Nueva Solicitud
                    </button>
                </div>
            </header>

            <div className="table-wrapper">
                <div className="table-responsive">
                    <table className="modern-table">
                        <thead>
                        <tr>
                            <th>N° Devolución</th>
                            <th>Factura Original</th>
                            <th>Solicitante</th>
                            <th>Estado</th>
                            <th>Total Reembolso</th>
                            <th>Acciones</th>
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
                            paginatedDevoluciones.map((d, idx) => (
                                <tr key={d.id} className="fade-in-row" style={{ animationDelay: `${idx * 0.05}s` }}>
                                    <td className="bold">{d.numeroDevolucion || '---'}</td>
                                    <td>{d.numeroFactura}</td>
                                    <td><span className="user-tag">{d.usuarioSolicitanteNombre}</span></td>
                                    <td><span className={`status-pill ${d.estado.toLowerCase()}`}>{d.estado}</span></td>
                                    <td className="price-text">${d.totalDevuelto?.toFixed(2)}</td>
                                    <td className="actions-cell">
                                        {esAdmin && d.estado === 'PENDIENTE' && (
                                            <>
                                                <button className="btn-action approve" onClick={() => handleAprobarAccion(d.id, true)}>✅</button>
                                                <button className="btn-action reject" onClick={() => handleAprobarAccion(d.id, false)}>❌</button>
                                            </>
                                        )}
                                        <button className="btn-circle-print" onClick={() => handleImprimir(d)}>📄</button>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                    {!loading && devolucionesFiltradas.length === 0 && (
                        <div className="empty-state">No se encontraron devoluciones.</div>
                    )}
                </div>

                {/* Paginación */}
                {!loading && devolucionesFiltradas.length > 0 && (
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

            {/* DRAWER (Nueva Devolución) */}
            {drawerOpen && (
                <div className="glass-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="pos-drawer" onClick={e => e.stopPropagation()}>
                        <div className="drawer-nav">
                            <button className="close-drawer-btn" onClick={() => setDrawerOpen(false)}>×</button>
                            <h2>Nueva Devolución</h2>
                        </div>

                        <div className="drawer-scrollable-content">
                            {!ventaSeleccionada ? (
                                <div className="pos-section">
                                    <label className="section-label">Seleccionar Factura de Venta</label>
                                    <input
                                        className="pos-input-sm"
                                        placeholder="Buscar por número..."
                                        value={busquedaVenta}
                                        onChange={e => setBusquedaVenta(e.target.value)}
                                    />
                                    <div className="results-grid">
                                        {ventasFiltradas.map(v => (
                                            <div key={v.id} className="result-card" onClick={() => handleSeleccionarVenta(v)}>
                                                <div><strong>{v.numeroFactura}</strong><p>{v.fecha}</p></div>
                                                <span>${round2(v.total).toFixed(2)}</span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ) : (
                                <>
                                    <div className="selection-active-card">
                                        <strong>{ventaSeleccionada.numeroFactura}</strong>
                                        <button className="btn-reset-sm" onClick={() => setVentaSeleccionada(null)}>Cambiar</button>
                                    </div>
                                    <div className="pos-section">
                                        <label className="section-label">Productos disponibles para retorno</label>
                                        {itemsDevolucion.map(item => (
                                            <div key={item.ventaDetalleId} className="cart-row-sm">
                                                <div>
                                                    <p><strong>{item.producto}</strong></p>
                                                    <small>Original: {item.cantidadMax} unidades</small>
                                                </div>
                                                <input
                                                    type="number"
                                                    className="qty-input-sm"
                                                    value={item.cantidadDevuelta}
                                                    onChange={e => actualizarCantidad(item.ventaDetalleId, parseInt(e.target.value) || 0)}
                                                    min="0"
                                                    max={item.cantidadMax}
                                                />
                                            </div>
                                        ))}
                                    </div>
                                    <div className="pos-section">
                                        <label className="section-label">Motivo de la solicitud</label>
                                        <textarea
                                            className="pos-input-sm"
                                            rows="3"
                                            value={motivo}
                                            onChange={e => setMotivo(e.target.value)}
                                            placeholder="Ej: Vencimiento, Empaque dañado..."
                                        />
                                    </div>
                                </>
                            )}
                        </div>

                        {ventaSeleccionada && (
                            <div className="drawer-footer-fixed">
                                <button className="btn-cancel" onClick={() => setDrawerOpen(false)}>Cancelar</button>
                                <button className="btn-save-final" onClick={handleSolicitar}>Crear Solicitud</button>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* Ticket de impresión (oculto en pantalla) */}
            {ticketPrint && (
                <div id="ticket-devolucion" className="print-invoice-container">
                    <div className="ticket-header">
                        <h2>FARMASYSTEM</h2>
                        <p>Nit: 900.123.456-1</p>
                        <p><strong>RECIBO DE DEVOLUCIÓN</strong></p>
                    </div>
                    <div className="ticket-info">
                        <p>Devolución: {ticketPrint.numeroDevolucion || 'PENDIENTE'}</p>
                        <p>Factura Ref: {ticketPrint.numeroFactura}</p>
                        <p>Fecha: {new Date().toLocaleDateString()}</p>
                        <p>Atiende: {ticketPrint.usuarioSolicitanteNombre}</p>
                    </div>
                    <div className="ticket-divider">--------------------------------</div>
                    <table className="ticket-table">
                        <thead><tr><th>Prod.</th><th>Cant.</th><th>Total</th></tr></thead>
                        <tbody>
                        {ticketPrint.detalles?.map((det, i) => (
                            <tr key={i}>
                                <td>{det.productoNombre}</td>
                                <td>{det.cantidadDevuelta}</td>
                                <td>${(det.cantidadDevuelta * det.precioUnitario).toFixed(2)}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                    <div className="ticket-divider">--------------------------------</div>
                    <div className="ticket-summary">
                        <p>Subtotal Original: ${ticketPrint.totalVentaOriginal?.toFixed(2)}</p>
                        <p className="total-label">TOTAL REEMBOLSO: ${ticketPrint.totalDevuelto?.toFixed(2)}</p>
                    </div>
                    <div className="ticket-footer">
                        <p>Motivo: {ticketPrint.motivo}</p>
                        <br /><br />
                        <p>__________________________</p>
                        <p>Firma Cliente / Recibido</p>
                    </div>
                </div>
            )}
        </div>
    );
}