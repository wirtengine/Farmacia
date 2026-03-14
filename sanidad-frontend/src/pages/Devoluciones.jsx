import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { listarDevoluciones, solicitarDevolucion, aprobarDevolucion } from '../services/devoluciones';
import { listarVentas, obtenerVenta } from '../services/ventas';
import './Devoluciones.css';

export default function Devoluciones() {
    const { user } = useAuth();
    const esAdmin = user?.rol === 'ADMIN';
    const usuarioId = user?.id;

    const [devoluciones, setDevoluciones] = useState([]);
    const [loading, setLoading] = useState(false);

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
            setDevoluciones(res.data);
        } catch (error) { console.error(error); }
        finally { setLoading(false); }
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
        // Pequeño delay para asegurar que el DOM se renderice antes de abrir el diálogo de impresión
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
            setVentaSeleccionada(res.data); // Usamos la data completa con detalles
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
            <header className="main-header">
                <div>
                    <h1>Devoluciones</h1>
                    <p className="vendedor-name">{esAdmin ? 'Panel de Control Administrativo' : 'Gestión de Solicitudes'}</p>
                </div>
                <button className="btn-add-venta" onClick={handleNuevaDevolucion}>+ Nueva Solicitud</button>
            </header>

            <div className="table-wrapper">
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
                    {devoluciones.map(d => (
                        <tr key={d.id}>
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
                    ))}
                    </tbody>
                </table>
            </div>

            {/* --- COMPONENTE DE FACTURA DETALLADA (SOLO VISIBLE AL IMPRIMIR) --- */}
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
                        <thead>
                        <tr>
                            <th>Prod.</th>
                            <th>Cant.</th>
                            <th>Total</th>
                        </tr>
                        </thead>
                        <tbody>
                        {/* Se asume que el objeto trae los detalles mapeados */}
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

            {/* --- DRAWER FORMULARIO --- */}
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
                                    <input className="pos-input-sm" placeholder="Buscar por número..." value={busquedaVenta} onChange={e => setBusquedaVenta(e.target.value)} />
                                    <div className="results-grid">
                                        {ventasFiltradas.map(v => (
                                            <div key={v.id} className="result-card" onClick={() => handleSeleccionarVenta(v)}>
                                                <div><strong>{v.numeroFactura}</strong><p>{v.fecha}</p></div>
                                                <span>${v.total}</span>
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
                                                <input type="number" className="qty-input-sm" value={item.cantidadDevuelta} onChange={e => actualizarCantidad(item.ventaDetalleId, parseInt(e.target.value) || 0)} />
                                            </div>
                                        ))}
                                    </div>
                                    <div className="pos-section">
                                        <label className="section-label">Motivo de la solicitud</label>
                                        <textarea className="pos-input-sm" rows="3" value={motivo} onChange={e => setMotivo(e.target.value)} placeholder="Ej: Vencimiento, Empaque dañado..."/>
                                    </div>
                                </>
                            )}
                        </div>

                        {ventaSeleccionada && (
                            <div className="pos-fixed-footer">
                                <button className="btn-confirm-final" onClick={handleSolicitar}>Crear Solicitud de Devolución</button>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}