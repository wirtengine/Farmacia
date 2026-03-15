import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { listarDevolucionesProveedor, solicitarDevolucionProveedor, aprobarDevolucionProveedor } from '../services/devolucionesProveedor';
import { listarLotes, obtenerLote } from '../services/lotes';
import { listarProveedores } from '../services/proveedores';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import './DevolucionesProveedor.css';

export default function DevolucionesProveedor() {
    const { user } = useAuth();
    const esAdmin = user?.rol === 'ADMIN';
    const usuarioId = user?.id;

    const [devoluciones, setDevoluciones] = useState([]);
    const [loading, setLoading] = useState(false);

    // Estados para el drawer
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [lotes, setLotes] = useState([]);
    const [proveedores, setProveedores] = useState([]);
    const [loteSeleccionado, setLoteSeleccionado] = useState(null);
    const [itemsDevolucion, setItemsDevolucion] = useState([]);
    const [motivo, setMotivo] = useState('');
    const [busquedaLote, setBusquedaLote] = useState('');

    useEffect(() => { cargarDevoluciones(); }, []);

    const cargarDevoluciones = async () => {
        setLoading(true);
        try {
            const res = await listarDevolucionesProveedor();
            setDevoluciones(res.data);
        } catch (error) { console.error(error); }
        finally { setLoading(false); }
    };

    const handleNuevaDevolucion = async () => {
        setLoteSeleccionado(null);
        setItemsDevolucion([]);
        setMotivo('');
        setBusquedaLote('');
        try {
            const [resLotes, resProv] = await Promise.all([listarLotes(), listarProveedores()]);
            setLotes(resLotes.data);
            setProveedores(resProv.data);
            setDrawerOpen(true);
        } catch (error) { alert('Error al cargar datos'); }
    };

    const handleSeleccionarLote = async (lote) => {
        try {
            const res = await obtenerLote(lote.id);
            const detalles = res.data.detalles.map(d => ({
                loteDetalleId: d.id,
                medicamentoNombre: d.medicamentoNombre,
                cantidadDisponible: d.cantidad,
                cantidadDevuelta: 0,
            }));
            setItemsDevolucion(detalles);
            setLoteSeleccionado(lote);
        } catch (error) { alert('Error al cargar detalles del lote'); }
    };

    const actualizarCantidad = (id, val) => {
        setItemsDevolucion(prev => prev.map(item =>
            item.loteDetalleId === id ? { ...item, cantidadDevuelta: Math.max(0, Math.min(val, item.cantidadDisponible)) } : item
        ));
    };

    // --- FUNCIÓN PARA GENERAR EL PDF LIMPIO Y PROFESIONAL ---
    const generarPDF = (datos) => {
        const doc = new jsPDF();
        const margin = 14;

        // Encabezado
        doc.setFontSize(18);
        doc.setTextColor(37, 99, 235); // Azul Primario
        doc.text("FarmaSystem - Gestión de Devoluciones", margin, 22);

        doc.setFontSize(10);
        doc.setTextColor(100);
        doc.text(`Fecha de Solicitud: ${new Date().toLocaleString()}`, margin, 30);
        doc.text(`Solicitado por: ${user?.nombre || 'Personal Farmacia'}`, margin, 35);

        // Información del Lote/Proveedor
        doc.setDrawColor(226, 232, 240);
        doc.line(margin, 40, 196, 40);

        doc.setFontSize(11);
        doc.setTextColor(15);
        doc.text(`Proveedor: ${datos.proveedor}`, margin, 50);
        doc.text(`Factura Referencia: ${datos.factura}`, margin, 56);

        // Tabla de Productos
        autoTable(doc, {
            startY: 65,
            head: [['Medicamento', 'Cantidad a Devolver']],
            body: datos.productos.map(p => [p.nombre, p.cantidad]),
            headStyles: { fillColor: [37, 99, 235], textColor: [255, 255, 255] },
            alternateRowStyles: { fillColor: [248, 250, 252] },
        });

        // Motivo
        const finalY = doc.lastAutoTable.finalY + 15;
        doc.setFontSize(10);
        doc.text("Motivo de la devolución:", margin, finalY);
        doc.setFontSize(11);
        doc.setTextColor(60);
        doc.text(datos.motivo || "No especificado", margin, finalY + 7, { maxWidth: 180 });

        doc.save(`Solicitud_Devolucion_${datos.factura}.pdf`);
    };

    const handleSolicitar = async () => {
        const productosParaDevolver = itemsDevolucion.filter(i => i.cantidadDevuelta > 0);

        if (productosParaDevolver.length === 0) return alert('Seleccione cantidades válidas');

        try {
            const payload = {
                loteId: loteSeleccionado.id,
                solicitadoPorId: usuarioId,
                motivo: motivo || null,
                detalles: productosParaDevolver.map(i => ({
                    loteDetalleId: i.loteDetalleId,
                    cantidadDevuelta: i.cantidadDevuelta
                }))
            };

            await solicitarDevolucionProveedor(payload);

            // 1. Datos para PDF y WhatsApp
            const provObj = proveedores.find(p => p.id === loteSeleccionado.proveedorId);
            const datosExport = {
                proveedor: provObj?.nombre,
                factura: loteSeleccionado.factura,
                productos: productosParaDevolver.map(p => ({ nombre: p.medicamentoNombre, cantidad: p.cantidadDevuelta })),
                motivo: motivo
            };

            // 2. Generar PDF
            generarPDF(datosExport);

            // 3. Abrir WhatsApp
            const mensaje = `*HOLA, SOLICITUD DE DEVOLUCIÓN*\n\nSe ha generado una solicitud para el lote *${loteSeleccionado.factura}*.\n\n*Detalles:* \n${datosExport.productos.map(p => `- ${p.nombre}: ${p.cantidad}`).join('\n')}\n\n*Motivo:* ${motivo || 'Ver PDF'}\n\n_He adjuntado el comprobante en PDF a este chat._`;

            const url = `https://wa.me/+505${provObj?.telefono}?text=${encodeURIComponent(mensaje)}`;
            window.open(url, '_blank');

            setDrawerOpen(false);
            cargarDevoluciones();
        } catch (error) { alert('Error al crear la solicitud'); }
    };

    // Funciones de gestión administrativa
    const handleAprobar = async (id) => {
        if (!window.confirm('¿Confirmar aprobación física de la devolución?')) return;
        try {
            await aprobarDevolucionProveedor({ devolucionId: id, aprobadoPorId: usuarioId, aprobada: true });
            cargarDevoluciones();
        } catch (error) { alert('Error'); }
    };

    const handleRechazar = async (id) => {
        const motivoRechazo = window.prompt('Motivo del rechazo:');
        if (!motivoRechazo) return;
        try {
            await aprobarDevolucionProveedor({ devolucionId: id, aprobadoPorId: usuarioId, aprobada: false, motivoRechazo });
            cargarDevoluciones();
        } catch (error) { alert('Error'); }
    };

    const lotesFiltrados = useMemo(() => {
        return lotes.filter(l => l.factura.toLowerCase().includes(busquedaLote.toLowerCase()));
    }, [lotes, busquedaLote]);

    return (
        <div className="module-container">
            <header className="module-header">
                <div>
                    <h1>Devoluciones a Proveedores</h1>
                    <p>Envíe solicitudes y PDF vía WhatsApp al instante</p>
                </div>
                <button className="btn-primary-compact" onClick={handleNuevaDevolucion}>
                    ＋ Nueva Solicitud
                </button>
            </header>

            <div className="table-card">
                <table className="custom-table">
                    <thead>
                    <tr>
                        <th>N° Solicitud</th>
                        <th>Factura Lote</th>
                        <th>Proveedor</th>
                        <th>Estado</th>
                        <th className="text-center">Acciones</th>
                    </tr>
                    </thead>
                    <tbody>
                    {devoluciones.map(d => (
                        <tr key={d.id}>
                            <td className="font-bold">{d.numeroDevolucion || 'Pendiente'}</td>
                            <td>{d.numeroFacturaLote}</td>
                            <td><span className="text-muted">{d.proveedorNombre}</span></td>
                            <td>
                                    <span className={`status-pill ${d.estado.toLowerCase()}`}>
                                        {d.estado}
                                    </span>
                            </td>
                            <td className="text-center">
                                <div className="action-buttons-group">
                                    {d.estado === 'PENDIENTE' && esAdmin && (
                                        <>
                                            <button className="btn-edit-icon" onClick={() => handleAprobar(d.id)}>✓</button>
                                            <button className="btn-delete-icon" onClick={() => handleRechazar(d.id)}>✗</button>
                                        </>
                                    )}
                                    <button className="btn-edit-icon">📄</button>
                                </div>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {/* Drawer */}
            {drawerOpen && (
                <div className="drawer-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="drawer-panel" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header-compact">
                            <h2>Nueva Devolución</h2>
                            <button className="close-btn-round" onClick={() => setDrawerOpen(false)}>×</button>
                        </div>

                        <div className="drawer-body-scrollable">
                            {!loteSeleccionado ? (
                                <div className="selection-container">
                                    <div className="field-group">
                                        <label>Buscar Factura de Lote</label>
                                        <input
                                            className="search-input-drawer"
                                            type="text"
                                            placeholder="Escriba factura..."
                                            value={busquedaLote}
                                            onChange={e => setBusquedaLote(e.target.value)}
                                        />
                                    </div>
                                    <div className="results-list">
                                        {lotesFiltrados.map(l => (
                                            <div key={l.id} className="item-card-select" onClick={() => handleSeleccionarLote(l)}>
                                                <div>
                                                    <strong>{l.factura}</strong>
                                                    <p>{proveedores.find(p => p.id === l.proveedorId)?.nombre}</p>
                                                </div>
                                                <span>➔</span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ) : (
                                <>
                                    <div className="info-banner-selection">
                                        <div>
                                            <small>Lote Seleccionado</small>
                                            <strong>{loteSeleccionado.factura}</strong>
                                        </div>
                                        <button onClick={() => setLoteSeleccionado(null)}>Cambiar</button>
                                    </div>

                                    <h4 className="section-divider">Cantidades a Devolver</h4>
                                    {itemsDevolucion.map(item => (
                                        <div key={item.loteDetalleId} className="dev-item-row">
                                            <div className="dev-item-info">
                                                <strong>{item.medicamentoNombre}</strong>
                                                <small>Stock: {item.cantidadDisponible}</small>
                                            </div>
                                            <div className="dev-item-qty">
                                                <input
                                                    type="number"
                                                    value={item.cantidadDevuelta}
                                                    onChange={e => actualizarCantidad(item.loteDetalleId, parseInt(e.target.value) || 0)}
                                                />
                                            </div>
                                        </div>
                                    ))}

                                    <div className="field-group" style={{ marginTop: '20px' }}>
                                        <label>Motivo</label>
                                        <textarea
                                            rows="3"
                                            value={motivo}
                                            onChange={e => setMotivo(e.target.value)}
                                            placeholder="¿Por qué se devuelve?"
                                        />
                                    </div>
                                </>
                            )}
                        </div>

                        {loteSeleccionado && (
                            <div className="drawer-footer-fixed">
                                <button className="btn-save-final whatsapp-style" onClick={handleSolicitar}>
                                    🚀 Enviar Solicitud y PDF
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}