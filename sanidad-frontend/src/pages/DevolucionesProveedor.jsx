import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { listarDevolucionesProveedor, solicitarDevolucionProveedor, aprobarDevolucionProveedor } from '../services/devolucionesProveedor';
import { listarLotes, obtenerLote } from '../services/lotes';
import { listarProveedores } from '../services/proveedores';
import { listarMedicamentos } from '../services/medicamentos'; // 🔥 NUEVO
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import './DevolucionesProveedor.css';
import {
    alertaConfirmacion,
    alertaExito,
    alertaError,
    alertaInput
} from '../alertas';

export default function DevolucionesProveedor() {
    const { user } = useAuth();
    const esAdmin = user?.rol === 'ADMIN';
    const usuarioId = user?.id;

    const [devoluciones, setDevoluciones] = useState([]);
    const [medicamentos, setMedicamentos] = useState([]); // 🔥 NUEVO
    const [loading, setLoading] = useState(false);
    const [proveedores, setProveedores] = useState([]);

    // Estados para filtro y paginación
    const [searchTerm, setSearchTerm] = useState('');
    const [estadoFiltro, setEstadoFiltro] = useState('TODOS'); // 🔥 NUEVO
    const [currentPage, setCurrentPage] = useState(1);
    const rowsPerPage = 15;

    // Estados para el drawer
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [lotes, setLotes] = useState([]);
    const [loteSeleccionado, setLoteSeleccionado] = useState(null);
    const [itemsDevolucion, setItemsDevolucion] = useState([]);
    const [motivo, setMotivo] = useState('');
    const [busquedaLote, setBusquedaLote] = useState('');

    // Cargar datos iniciales: devoluciones, medicamentos, proveedores y lotes
    useEffect(() => {
        const cargarDatosIniciales = async () => {
            setLoading(true);
            try {
                const [resDev, resMed, resProv, resLot] = await Promise.all([
                    listarDevolucionesProveedor(),
                    listarMedicamentos(),
                    listarProveedores(),
                    listarLotes()
                ]);
                const sorted = (resDev.data || []).sort((a, b) => b.id - a.id);
                setDevoluciones(sorted);
                setMedicamentos(resMed.data || []);
                setProveedores(resProv.data || []);
                setLotes(resLot.data || []);
            } catch (error) {
                console.error(error);
            } finally {
                setLoading(false);
            }
        };
        cargarDatosIniciales();
    }, []);

    const cargarDevoluciones = async () => {
        setLoading(true);
        try {
            const res = await listarDevolucionesProveedor();
            const sorted = (res.data || []).sort((a, b) => b.id - a.id);
            setDevoluciones(sorted);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    // Filtrado combinado (texto + estado)
    const devolucionesFiltradas = useMemo(() => {
        let filtradas = devoluciones;
        const term = searchTerm.toLowerCase().trim();
        if (term) {
            filtradas = filtradas.filter(d =>
                (d.numeroDevolucion && d.numeroDevolucion.toLowerCase().includes(term)) ||
                (d.numeroFacturaLote && d.numeroFacturaLote.toLowerCase().includes(term)) ||
                (d.proveedorNombre && d.proveedorNombre.toLowerCase().includes(term))
            );
        }
        if (estadoFiltro !== 'TODOS') {
            filtradas = filtradas.filter(d => d.estado === estadoFiltro);
        }
        return filtradas;
    }, [devoluciones, searchTerm, estadoFiltro]);

    // Paginación
    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm, estadoFiltro]);

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

    // Función para obtener el medicamento a partir de un detalle de devolución
    const obtenerMedicamentoDesdeDetalle = (det) => {
        return medicamentos.find(m => m.id === det.medicamentoId);
    };

    // 📄 Función para imprimir PDF de una devolución existente (con imágenes)
    const imprimirDevolucion = (devolucion) => {
        const doc = new jsPDF();
        const margin = 14;

        doc.setFontSize(18);
        doc.setTextColor(37, 99, 235);
        doc.text("FarmaSystem - Devolución a Proveedor", margin, 22);

        doc.setFontSize(10);
        doc.setTextColor(100);
        doc.text(`Fecha: ${new Date().toLocaleString()}`, margin, 30);
        doc.text(`Solicitado por: ${devolucion.usuarioSolicitanteNombre || 'Desconocido'}`, margin, 35);

        doc.setDrawColor(226, 232, 240);
        doc.line(margin, 40, 196, 40);

        doc.setFontSize(11);
        doc.setTextColor(15);
        doc.text(`Proveedor: ${devolucion.proveedorNombre}`, margin, 50);
        doc.text(`Factura Lote: ${devolucion.numeroFacturaLote}`, margin, 56);
        doc.text(`N° Solicitud: ${devolucion.numeroDevolucion || 'Pendiente'}`, margin, 62);
        doc.text(`Estado: ${devolucion.estado}`, margin, 68);

        // Preparar datos para autoTable con imágenes
        const tableData = devolucion.detalles?.map(det => {
            const med = obtenerMedicamentoDesdeDetalle(det);
            return {
                nombre: med?.nombre || det.medicamentoNombre || 'S/N',
                cantidad: det.cantidadDevuelta,
                imagenUrl: med?.imagen ? `http://localhost:8080/${med.imagen.replace(/\\/g, '/')}` : null
            };
        }) || [];

        // Usar autoTable con didDrawCell para dibujar imágenes
        autoTable(doc, {
            startY: 75,
            head: [['Medicamento', 'Cantidad Devuelta']],
            body: tableData.map(item => [item.nombre, item.cantidad]),
            headStyles: { fillColor: [37, 99, 235], textColor: [255, 255, 255] },
            alternateRowStyles: { fillColor: [248, 250, 252] },
            didDrawCell: (data) => {
                // Dibujar imagen en la columna 0 (medicamento) si existe
                if (data.column.index === 0 && tableData[data.row.index]?.imagenUrl) {
                    const imgUrl = tableData[data.row.index].imagenUrl;
                    const img = new Image();
                    img.src = imgUrl;
                    img.onload = () => {
                        // Posición dentro de la celda: x + 2, y + 2, ancho 20, alto 20
                        doc.addImage(img, 'JPEG', data.cell.x + 2, data.cell.y + 2, 20, 20);
                        // Ajustar el texto para que no se superponga (opcional)
                        const textX = data.cell.x + 24;
                        const textY = data.cell.y + data.cell.height / 2;
                        doc.setFontSize(10);
                        doc.text(tableData[data.row.index].nombre, textX, textY);
                    };
                }
            }
        });

        const finalY = doc.lastAutoTable.finalY + 15;
        doc.setFontSize(10);
        doc.text("Motivo:", margin, finalY);
        doc.setFontSize(11);
        doc.setTextColor(60);
        doc.text(devolucion.motivo || "No especificado", margin, finalY + 7, { maxWidth: 180 });

        doc.save(`Devolucion_${devolucion.numeroFacturaLote}.pdf`);
    };

    // Función para enviar por WhatsApp (genera PDF y abre WhatsApp)
    const enviarWhatsApp = (devolucion) => {
        const proveedor = proveedores.find(p => p.nombre === devolucion.proveedorNombre);
        const telefono = proveedor?.telefono || '';

        imprimirDevolucion(devolucion);

        const productosTexto = devolucion.detalles?.map(det => {
            const med = obtenerMedicamentoDesdeDetalle(det);
            return `- ${med?.nombre || det.medicamentoNombre}: ${det.cantidadDevuelta}`;
        }).join('\n') || '';

        const mensaje = `*HOLA, REPORTE DE DEVOLUCIÓN*\n\n` +
            `*N° Solicitud:* ${devolucion.numeroDevolucion || 'Pendiente'}\n` +
            `*Factura Lote:* ${devolucion.numeroFacturaLote}\n` +
            `*Proveedor:* ${devolucion.proveedorNombre}\n` +
            `*Estado:* ${devolucion.estado}\n` +
            `*Motivo:* ${devolucion.motivo || 'No especificado'}\n\n` +
            `*Productos devueltos:*\n${productosTexto}\n\n` +
            `_Se ha generado un PDF con los detalles completos._`;

        if (telefono) {
            const url = `https://wa.me/+505${telefono}?text=${encodeURIComponent(mensaje)}`;
            window.open(url, '_blank');
        } else {
            alertaError('No se encontró el número de teléfono del proveedor.');
        }
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
        } catch (error) {
            alertaError('Error al cargar datos');
        }
    };

    const handleSeleccionarLote = async (lote) => {
        try {
            const res = await obtenerLote(lote.id);
            const detalles = res.data.detalles.map(d => {
                const med = medicamentos.find(m => m.id === d.medicamentoId);
                return {
                    loteDetalleId: d.id,
                    medicamentoNombre: med?.nombre || d.medicamentoNombre,
                    cantidadDisponible: d.cantidad,
                    cantidadDevuelta: 0,
                    imagen: med?.imagen
                };
            });
            setItemsDevolucion(detalles);
            setLoteSeleccionado(lote);
        } catch (error) {
            alertaError('Error al cargar detalles del lote');
        }
    };

    const actualizarCantidad = (id, val) => {
        setItemsDevolucion(prev => prev.map(item =>
            item.loteDetalleId === id ? { ...item, cantidadDevuelta: Math.max(0, Math.min(val, item.cantidadDisponible)) } : item
        ));
    };

    // 📄 Generar PDF para la nueva solicitud (con imágenes)
    const generarPDF = (datos) => {
        const doc = new jsPDF();
        const margin = 14;

        doc.setFontSize(18);
        doc.setTextColor(37, 99, 235);
        doc.text("FarmaSystem - Gestión de Devoluciones", margin, 22);

        doc.setFontSize(10);
        doc.setTextColor(100);
        doc.text(`Fecha de Solicitud: ${new Date().toLocaleString()}`, margin, 30);
        doc.text(`Solicitado por: ${user?.nombre || 'Personal Farmacia'}`, margin, 35);

        doc.setDrawColor(226, 232, 240);
        doc.line(margin, 40, 196, 40);

        doc.setFontSize(11);
        doc.setTextColor(15);
        doc.text(`Proveedor: ${datos.proveedor}`, margin, 50);
        doc.text(`Factura Referencia: ${datos.factura}`, margin, 56);

        // Preparar datos para la tabla con imágenes
        const tableData = datos.productos.map(p => ({
            nombre: p.nombre,
            cantidad: p.cantidad,
            imagenUrl: p.imagenUrl
        }));

        autoTable(doc, {
            startY: 65,
            head: [['Medicamento', 'Cantidad a Devolver']],
            body: tableData.map(item => [item.nombre, item.cantidad]),
            headStyles: { fillColor: [37, 99, 235], textColor: [255, 255, 255] },
            alternateRowStyles: { fillColor: [248, 250, 252] },
            didDrawCell: (data) => {
                if (data.column.index === 0 && tableData[data.row.index]?.imagenUrl) {
                    const imgUrl = tableData[data.row.index].imagenUrl;
                    const img = new Image();
                    img.src = imgUrl;
                    img.onload = () => {
                        doc.addImage(img, 'JPEG', data.cell.x + 2, data.cell.y + 2, 20, 20);
                        const textX = data.cell.x + 24;
                        const textY = data.cell.y + data.cell.height / 2;
                        doc.setFontSize(10);
                        doc.text(tableData[data.row.index].nombre, textX, textY);
                    };
                }
            }
        });

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

        if (productosParaDevolver.length === 0) {
            alertaError('Seleccione cantidades válidas');
            return;
        }

        const result = await alertaConfirmacion({
            titulo: 'Crear devolución',
            texto: '¿Desea registrar esta solicitud de devolución a proveedor?',
            confirmar: 'Crear solicitud',
            cancelar: 'Cancelar',
            icono: 'question'
        });

        if (!result.isConfirmed) return;

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

            const provObj = proveedores.find(p => p.id === loteSeleccionado.proveedorId);
            const datosExport = {
                proveedor: provObj?.nombre,
                factura: loteSeleccionado.factura,
                productos: productosParaDevolver.map(p => ({
                    nombre: p.medicamentoNombre,
                    cantidad: p.cantidadDevuelta,
                    imagenUrl: p.imagen ? `http://localhost:8080/${p.imagen.replace(/\\/g, '/')}` : null
                })),
                motivo: motivo
            };

            generarPDF(datosExport);

            const mensaje = `*HOLA, SOLICITUD DE DEVOLUCIÓN*\n\nSe ha generado una solicitud para el lote *${loteSeleccionado.factura}*.\n\n*Detalles:* \n${datosExport.productos.map(p => `- ${p.nombre}: ${p.cantidad}`).join('\n')}\n\n*Motivo:* ${motivo || 'Ver PDF'}\n\n_He adjuntado el comprobante en PDF a este chat._`;

            const url = `https://wa.me/+505${provObj?.telefono}?text=${encodeURIComponent(mensaje)}`;
            window.open(url, '_blank');

            setDrawerOpen(false);

            await cargarDevoluciones();

            alertaExito('Solicitud de devolución creada');

        } catch (error) {
            alertaError('Error al crear la solicitud');
        }
    };

    const handleAprobar = async (id) => {
        const result = await alertaConfirmacion({
            titulo: '¿Aprobar devolución?',
            texto: 'Confirma la aprobación física de esta devolución.',
            confirmar: 'Sí, aprobar',
            cancelar: 'Cancelar',
            icono: 'question'
        });

        if (!result.isConfirmed) return;

        try {
            await aprobarDevolucionProveedor({
                devolucionId: id,
                aprobadoPorId: usuarioId,
                aprobada: true
            });

            await cargarDevoluciones();

            alertaExito('Devolución aprobada correctamente');

        } catch (error) {
            alertaError('Error al aprobar la devolución');
        }
    };

    const handleRechazar = async (id) => {
        const motivo = await alertaInput({
            titulo: 'Motivo del rechazo',
            placeholder: 'Escriba el motivo...',
            confirmar: 'Rechazar',
            cancelar: 'Cancelar'
        });

        if (!motivo.isConfirmed) return;

        if (!motivo.value?.trim()) {
            alertaError('Debe ingresar un motivo de rechazo');
            return;
        }

        try {
            await aprobarDevolucionProveedor({
                devolucionId: id,
                aprobadoPorId: usuarioId,
                aprobada: false,
                motivoRechazo: motivo.value
            });

            await cargarDevoluciones();

            alertaExito('Devolución rechazada');

        } catch (error) {
            alertaError('Error al rechazar la devolución');
        }
    };

    const lotesFiltrados = useMemo(() => {
        return lotes.filter(l => l.factura.toLowerCase().includes(busquedaLote.toLowerCase()));
    }, [lotes, busquedaLote]);

    return (
        <div className="module-container">
            <header className="module-header">
                <div className="header-title">
                    <h1>Devoluciones a Proveedores</h1>
                    <p>Envíe solicitudes y PDF vía WhatsApp al instante</p>
                </div>
                <div className="header-actions-row">
                    <div className="search-bar-container">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            className="search-input-main"
                            placeholder="Buscar solicitud, factura o proveedor..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    {/* 🔥 Filtro por estado */}
                    <div className="status-filter-group">
                        <button
                            className={`status-filter-btn ${estadoFiltro === 'TODOS' ? 'active' : ''}`}
                            onClick={() => setEstadoFiltro('TODOS')}
                        >
                            Todos
                        </button>
                        <button
                            className={`status-filter-btn ${estadoFiltro === 'PENDIENTE' ? 'active' : ''}`}
                            onClick={() => setEstadoFiltro('PENDIENTE')}
                        >
                            Pendientes
                        </button>
                        <button
                            className={`status-filter-btn ${estadoFiltro === 'APROBADA' ? 'active' : ''}`}
                            onClick={() => setEstadoFiltro('APROBADA')}
                        >
                            Aprobadas
                        </button>
                        <button
                            className={`status-filter-btn ${estadoFiltro === 'RECHAZADA' ? 'active' : ''}`}
                            onClick={() => setEstadoFiltro('RECHAZADA')}
                        >
                            Rechazadas
                        </button>
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
                            <th>N° Solicitud</th>
                            <th>Factura Lote</th>
                            <th>Proveedor</th>
                            <th>Productos</th> {/* 🔥 NUEVA COLUMNA */}
                            <th>Estado</th>
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
                                    <td><div className="skeleton-cell" /></td>
                                </tr>
                            ))
                        ) : (
                            paginatedDevoluciones.map((d, idx) => (
                                <tr key={d.id} className="fade-in-row" style={{ animationDelay: `${idx * 0.05}s` }}>
                                    <td className="font-bold">{d.numeroDevolucion || 'Pendiente'}</td>
                                    <td>{d.numeroFacturaLote}</td>
                                    <td><span className="user-tag">{d.proveedorNombre}</span></td>
                                    {/* 🔥 COLUMNA DE PRODUCTOS CON IMÁGENES */}
                                    <td>
                                        <div className="items-chip-container">
                                            {d.detalles?.map((det, i) => {
                                                const med = obtenerMedicamentoDesdeDetalle(det);
                                                return (
                                                    <div key={i} className="med-chip-with-img">
                                                        {med?.imagen && (
                                                            <img
                                                                src={`http://localhost:8080/${med.imagen.replace(/\\/g, '/')}`}
                                                                alt="med"
                                                            />
                                                        )}
                                                        <span>
                                                                {med?.nombre || det.medicamentoNombre || 'S/N'}
                                                            <small> x{det.cantidadDevuelta}</small>
                                                            </span>
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    </td>
                                    <td>
                                            <span className={`status-pill ${d.estado.toLowerCase()}`}>
                                                {d.estado}
                                            </span>
                                    </td>
                                    <td className="text-center">
                                        <div className="action-buttons-group">
                                            {d.estado === 'PENDIENTE' && esAdmin && (
                                                <>
                                                    <button className="btn-action approve" onClick={() => handleAprobar(d.id)} title="Aprobar">✓</button>
                                                    <button className="btn-action reject" onClick={() => handleRechazar(d.id)} title="Rechazar">✗</button>
                                                </>
                                            )}
                                            <button className="btn-circle-print" onClick={() => imprimirDevolucion(d)} title="Imprimir PDF">📄</button>
                                            <button className="btn-whatsapp" onClick={() => enviarWhatsApp(d)} title="Enviar por WhatsApp">📱</button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                    {!loading && devolucionesFiltradas.length === 0 && (
                        <div className="empty-state">No se encontraron solicitudes de devolución.</div>
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

            {/* Drawer para nueva solicitud */}
            {drawerOpen && (
                <div className="glass-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="pos-drawer" onClick={e => e.stopPropagation()}>
                        <div className="drawer-nav">
                            <button className="close-drawer-btn" onClick={() => setDrawerOpen(false)}>×</button>
                            <h2>Nueva Devolución a Proveedor</h2>
                        </div>

                        <div className="drawer-scrollable-content">
                            {!loteSeleccionado ? (
                                <div className="selection-container">
                                    <div className="field-group">
                                        <label className="section-label">Buscar Factura de Lote</label>
                                        <input
                                            className="pos-input-sm"
                                            type="text"
                                            placeholder="Escriba número de factura..."
                                            value={busquedaLote}
                                            onChange={e => setBusquedaLote(e.target.value)}
                                        />
                                    </div>
                                    <div className="results-grid">
                                        {lotesFiltrados.map(l => (
                                            <div key={l.id} className="result-card" onClick={() => handleSeleccionarLote(l)}>
                                                <div>
                                                    <strong>{l.factura}</strong>
                                                    <p className="text-muted">{proveedores.find(p => p.id === l.proveedorId)?.nombre}</p>
                                                </div>
                                                <span className="select-arrow">→</span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ) : (
                                <>
                                    <div className="selection-active-card">
                                        <div>
                                            <small>Lote Seleccionado</small>
                                            <strong>{loteSeleccionado.factura}</strong>
                                        </div>
                                        <button className="btn-reset-sm" onClick={() => setLoteSeleccionado(null)}>Cambiar</button>
                                    </div>

                                    <h4 className="section-divider">Cantidades a Devolver</h4>
                                    {itemsDevolucion.map(item => (
                                        <div key={item.loteDetalleId} className="cart-row-sm">
                                            {item.imagen && (
                                                <img
                                                    src={`http://localhost:8080/${item.imagen.replace(/\\/g, '/')}`}
                                                    alt="med"
                                                    style={{ width: '40px', height: '40px', objectFit: 'cover', borderRadius: '6px', marginRight: '10px' }}
                                                />
                                            )}
                                            <div className="cart-info-sm">
                                                <strong>{item.medicamentoNombre}</strong>
                                                <small>Stock: {item.cantidadDisponible}</small>
                                            </div>
                                            <div className="cart-ctrls-sm">
                                                <input
                                                    type="number"
                                                    className="qty-input-sm"
                                                    value={item.cantidadDevuelta}
                                                    onChange={e => actualizarCantidad(item.loteDetalleId, parseInt(e.target.value) || 0)}
                                                    min="0"
                                                    max={item.cantidadDisponible}
                                                />
                                            </div>
                                        </div>
                                    ))}

                                    <div className="field-group" style={{ marginTop: '20px' }}>
                                        <label className="section-label">Motivo de la devolución</label>
                                        <textarea
                                            className="pos-input-sm"
                                            rows="3"
                                            value={motivo}
                                            onChange={e => setMotivo(e.target.value)}
                                            placeholder="Ej: Producto vencido, empaque dañado..."
                                        />
                                    </div>
                                </>
                            )}
                        </div>

                        {loteSeleccionado && (
                            <div className="drawer-footer-fixed">
                                <button className="btn-cancel" onClick={() => setDrawerOpen(false)}>Cancelar</button>
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