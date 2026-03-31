import { useState, useEffect, useMemo } from "react";
import { useAuth } from "../context/AuthContext";
import { listarVentas, crearVenta } from "../services/ventas";
import { listarClientes } from "../services/clientes";
import { listarLotes } from "../services/lotes";
import { listarMedicamentos } from "../services/medicamentos";
import { getLoteFIFO, getComplementarios, getVentaGuiada } from "../services/ventaInteligencia";
import { jsPDF } from "jspdf";
import autoTable from "jspdf-autotable";
import "./Ventas.css";

export default function Ventas() {
    const { user } = useAuth();
    const esAdmin = user?.rol === 'ADMIN';
    const usuarioId = user?.id;
    const nombreVendedor = user?.username || "Vendedor";

    // Función para redondear a 2 decimales
    const round2 = (num) => Math.round((num + Number.EPSILON) * 100) / 100;

    // Formato de moneda en Córdobas
    const formatCurrency = (value) => `C$ ${value.toFixed(2)}`;

    // Datos
    const [ventas, setVentas] = useState([]);
    const [clientes, setClientes] = useState([]);
    const [lotes, setLotes] = useState([]);      // datos originales
    const [medicamentos, setMedicamentos] = useState([]);
    const [loading, setLoading] = useState(false);

    // Filtros
    const [filtroTexto, setFiltroTexto] = useState("");
    const [empleadoFiltrado, setEmpleadoFiltrado] = useState(null);
    const [showEmpleadoPanel, setShowEmpleadoPanel] = useState(false);

    // Paginación
    const [currentPage, setCurrentPage] = useState(1);
    const rowsPerPage = 15;

    // UI
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [tipoVenta, setTipoVenta] = useState("rapida");

    // Formulario de Nueva Venta
    const [clienteSeleccionado, setClienteSeleccionado] = useState(null);
    const [busquedaMedicamento, setBusquedaMedicamento] = useState("");
    const [detallesVenta, setDetallesVenta] = useState([]);
    const [montoUsarSaldo, setMontoUsarSaldo] = useState(0);
    const [efectivoRecibido, setEfectivoRecibido] = useState(0);

    // --- Inteligencia de ventas ---
    const [loteRecomendado, setLoteRecomendado] = useState(null);
    const [complementarios, setComplementarios] = useState([]);
    const [contextoCliente, setContextoCliente] = useState(null);

    // --------------------------------------------------------------
    // Procesar lotes para obtener stock de venta (ubicación + general)
    // --------------------------------------------------------------
    const lotesConStock = useMemo(() => {
        return lotes.map(lote => ({
            ...lote,
            detalles: lote.detalles.map(detalle => {
                const stockUbicacion = detalle.stockUbicacion ?? detalle.cantidad; // fallback
                return {
                    ...detalle,
                    stockUbicacion: stockUbicacion,
                    // stockVenta = si hay ubicación, se descuenta de ahí, sino del stock total
                    stockVenta: stockUbicacion > 0 ? stockUbicacion : detalle.cantidad
                };
            })
        }));
    }, [lotes]);

    // Función auxiliar para obtener stock de venta de un producto por su id
    const getStockVenta = (loteDetalleId) => {
        for (const lote of lotesConStock) {
            const det = lote.detalles.find(d => d.id === loteDetalleId);
            if (det) return det.stockVenta;
        }
        return 0;
    };

    // Cargar datos iniciales (ventas, medicamentos y lotes)
    useEffect(() => {
        const cargarDatosIniciales = async () => {
            setLoading(true);
            try {
                const [resV, resM, resL] = await Promise.all([
                    listarVentas(),
                    listarMedicamentos(),
                    listarLotes()
                ]);
                const sorted = (resV.data || []).sort((a, b) => b.id - a.id);
                setVentas(sorted);
                setMedicamentos(resM.data || []);
                setLotes(resL.data || []);
            } catch (e) {
                console.error("Error cargando datos iniciales", e);
            } finally {
                setLoading(false);
            }
        };
        cargarDatosIniciales();
    }, []);

    const cargarVentas = async () => {
        try {
            const res = await listarVentas();
            const sorted = (res.data || []).sort((a, b) => b.id - a.id);
            setVentas(sorted);
        } catch (e) {
            console.error("Error cargando ventas", e);
        }
    };

    // Lógica de filtrado
    const ventasFiltradas = useMemo(() => {
        let filtradas = ventas;
        if (!esAdmin) {
            filtradas = filtradas.filter(v => v.usuarioId === usuarioId);
        } else if (empleadoFiltrado) {
            filtradas = filtradas.filter(v => v.usuarioUsername === empleadoFiltrado);
        }
        if (filtroTexto) {
            const t = filtroTexto.toLowerCase();
            filtradas = filtradas.filter(v =>
                v.numeroFactura.toLowerCase().includes(t) ||
                (v.clienteNombre && v.clienteNombre.toLowerCase().includes(t))
            );
        }
        return filtradas;
    }, [ventas, esAdmin, usuarioId, empleadoFiltrado, filtroTexto]);

    useEffect(() => {
        setCurrentPage(1);
    }, [filtroTexto, empleadoFiltrado]);

    // Paginación
    const totalPages = Math.ceil(ventasFiltradas.length / rowsPerPage);
    const paginatedVentas = useMemo(() => {
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        return ventasFiltradas.slice(start, end);
    }, [ventasFiltradas, currentPage]);

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

    const listaEmpleados = useMemo(() => [...new Set(ventas.map(v => v.usuarioUsername))], [ventas]);

    const iniciarVenta = async () => {
        try {
            const [resL, resC, resM] = await Promise.all([
                listarLotes(),
                listarClientes(),
                listarMedicamentos()
            ]);
            setLotes(resL.data || []);
            setClientes(resC.data || []);
            setMedicamentos(resM.data || []);

            setTipoVenta("rapida");
            setClienteSeleccionado(resC.data?.find(c => c.nombre.toLowerCase().includes("consumidor")));
            setDetallesVenta([]);
            setMontoUsarSaldo(0);
            setEfectivoRecibido(0);
            // Reiniciar inteligencia
            setLoteRecomendado(null);
            setComplementarios([]);
            setContextoCliente(null);
            setDrawerOpen(true);
        } catch (err) { alert("Error al cargar datos"); }
    };

    // Al cambiar cliente, obtener su contexto
    useEffect(() => {
        if (!drawerOpen) return;
        if (tipoVenta === 'cliente' && clienteSeleccionado?.id) {
            getVentaGuiada(clienteSeleccionado.id, null)
                .then(res => setContextoCliente(res.data))
                .catch(err => console.error(err));
        } else {
            setContextoCliente(null);
        }
    }, [clienteSeleccionado, tipoVenta, drawerOpen]);

    // Búsqueda de medicamento con inteligencia
    const handleBuscarMedicamento = async (query) => {
        setBusquedaMedicamento(query);
        if (!query || query.length < 2) {
            setLoteRecomendado(null);
            setComplementarios([]);
            return;
        }
        // Buscar el medicamento que coincida exactamente o contenga
        const medicamentoEncontrado = medicamentos.find(m =>
            m.nombre.toLowerCase().includes(query.toLowerCase())
        );
        if (medicamentoEncontrado) {
            try {
                const [fifoRes, compRes] = await Promise.all([
                    getLoteFIFO(medicamentoEncontrado.id),
                    getComplementarios(medicamentoEncontrado.id)
                ]);
                setLoteRecomendado(fifoRes.data);
                setComplementarios(compRes.data || []);
            } catch (err) {
                console.error("Error cargando recomendaciones", err);
            }
        } else {
            setLoteRecomendado(null);
            setComplementarios([]);
        }
    };

    // Agregar medicamento (con soporte para lote recomendado)
    const agregarMedicamento = (med, loteId = med.id) => {
        const stockVenta = getStockVenta(loteId);
        if (stockVenta < 1) {
            alert("Producto sin stock disponible.");
            return;
        }
        setDetallesVenta(prev => {
            const existente = prev.find(p => p.loteDetalleId === loteId);
            let nuevaCantidad = existente ? existente.cantidad + 1 : 1;
            if (nuevaCantidad > stockVenta) {
                alert(`No hay suficiente stock disponible (máximo: ${stockVenta})`);
                return prev;
            }
            const precio = med.precioUnitario;
            if (existente) {
                return prev.map(p =>
                    p.loteDetalleId === loteId
                        ? { ...p, cantidad: nuevaCantidad, subtotal: nuevaCantidad * precio }
                        : p
                );
            } else {
                return [...prev, {
                    loteDetalleId: loteId,
                    medicamentoNombre: med.medicamentoNombre || med.nombre,
                    precioUnitario: precio,
                    cantidad: 1,
                    subtotal: precio
                }];
            }
        });
        setBusquedaMedicamento("");
        setLoteRecomendado(null);
        setComplementarios([]);
    };

    // Agregar producto complementario
    const agregarComplementario = (prod) => {
        const loteDetalle = lotesConStock
            .flatMap(l => l.detalles)
            .find(d => d.medicamentoId === prod.medicamentoId && d.stockVenta > 0);
        if (loteDetalle) {
            const med = medicamentos.find(m => m.id === prod.medicamentoId);
            if (med) {
                agregarMedicamento({ ...med, medicamentoNombre: med.nombre, precioUnitario: med.precioUnitario }, loteDetalle.id);
            }
        } else {
            alert("Producto sin stock disponible");
        }
    };

    const subtotal = detallesVenta.reduce((acc, d) => acc + d.subtotal, 0);
    const total = round2(subtotal * 1.15);
    const cambio = round2(Math.max((parseFloat(efectivoRecibido) + parseFloat(montoUsarSaldo)) - total, 0));

    const finalizarVenta = async () => {
        if (detallesVenta.length === 0) return alert("El carrito está vacío");

        // Verificar nuevamente stock antes de enviar (por si hubo cambios en el backend)
        for (const det of detallesVenta) {
            const stockVenta = getStockVenta(det.loteDetalleId);
            if (det.cantidad > stockVenta) {
                alert(`Stock insuficiente para ${det.medicamentoNombre}. Solo disponible: ${stockVenta}`);
                return;
            }
        }

        const data = {
            clienteId: clienteSeleccionado?.id,
            usuarioId,
            detalles: detallesVenta.map(d => ({ loteDetalleId: d.loteDetalleId, cantidad: d.cantidad })),
            montoUsadoSaldo: montoUsarSaldo,
            montoEfectivo: efectivoRecibido
        };
        try {
            const res = await crearVenta(data);
            generarPDF(res.data, detallesVenta);
            setDrawerOpen(false);
            cargarVentas();
        } catch (e) {
            console.error(e);
            alert("Error al procesar la venta. Verifica el stock o contacta a soporte.");
        }
    };

    const generarPDF = (venta, items) => {
        const doc = new jsPDF({ unit: "mm", format: [80, 160] });
        doc.setFontSize(10).text("FARMACIA SANIDAD", 40, 10, { align: "center" });
        doc.setFontSize(7);
        doc.text(`Factura: ${venta.numeroFactura}`, 5, 18);
        doc.text(`Cliente: ${venta.clienteNombre || "Consumidor Final"}`, 5, 22);
        autoTable(doc, {
            startY: 25,
            head: [["Cant", "Producto", "Sub"]],
            body: items.map(d => {
                const nombreMed = d.medicamentoNombre || medicamentos.find(m => m.id === d.medicamentoId)?.nombre || "Producto";
                return [d.cantidad, nombreMed, formatCurrency(d.subtotal || (d.cantidad * d.precioUnitario))];
            }),
            styles: { fontSize: 6 }
        });
        doc.text(`TOTAL: ${formatCurrency(venta.total || total)}`, 5, doc.lastAutoTable.finalY + 5);
        doc.save(`Venta_${venta.numeroFactura}.pdf`);
    };

    return (
        <div className="module-container">
            <header className="module-header">
                <div className="header-title">
                    <h1>Ventas</h1>
                    <p>Sesión activa: <span className="vendedor-name">{nombreVendedor}</span></p>
                </div>
                <div className="header-actions-row">
                    <div className="search-bar-container">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            className="search-input-main"
                            placeholder="Buscar factura o cliente..."
                            value={filtroTexto}
                            onChange={(e) => setFiltroTexto(e.target.value)}
                        />
                    </div>

                    {esAdmin && (
                        <div className="admin-filter-group">
                            <button
                                className={`btn-filter-user ${empleadoFiltrado ? 'active' : ''}`}
                                onClick={() => setShowEmpleadoPanel(!showEmpleadoPanel)}
                            >
                                👤 {empleadoFiltrado || 'Vendedores'}
                            </button>
                            {showEmpleadoPanel && (
                                <div className="floating-user-panel">
                                    <div className="panel-item" onClick={() => { setEmpleadoFiltrado(null); setShowEmpleadoPanel(false); }}>Ver todos</div>
                                    {listaEmpleados.map(emp => (
                                        <div key={emp} className="panel-item" onClick={() => { setEmpleadoFiltrado(emp); setShowEmpleadoPanel(false); }}>{emp}</div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    <button className="btn-add-venta" onClick={iniciarVenta}>＋ Nueva Venta</button>
                </div>
            </header>

            <div className="table-wrapper">
                <div className="table-responsive">
                    <table className="modern-table">
                        <thead>
                        <tr>
                            <th>Factura</th>
                            <th>Vendedor</th>
                            <th>Cliente</th>
                            <th>Medicamentos</th>
                            <th>Total</th>
                            <th>Acción</th>
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
                            paginatedVentas.map((v, idx) => (
                                <tr key={v.id} className="fade-in-row" style={{ animationDelay: `${idx * 0.05}s` }}>
                                    <td className="bold">#{v.numeroFactura}</td>
                                    <td><span className="user-tag">{v.usuarioUsername}</span></td>
                                    <td>{v.clienteNombre || "Consumidor Final"}</td>
                                    <td>
                                        <div className="items-chip-container">
                                            {v.detalles?.map((det, i) => {
                                                const loteDet = lotesConStock
                                                    .flatMap(l => l.detalles)
                                                    .find(ld => ld.id === det.loteDetalleId);
                                                const med = medicamentos.find(m => m.id === loteDet?.medicamentoId);
                                                return (
                                                    <div key={i} className="med-chip-with-img">
                                                        {med?.imagen && (
                                                            <img
                                                                src={`http://localhost:8080/${med.imagen.replace(/\\/g, "/")}`}
                                                                alt="med"
                                                            />
                                                        )}
                                                        <span>
                                                            {med?.nombre || 'S/N'}
                                                            <small> x{det.cantidad}</small>
                                                        </span>
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    </td>
                                    <td className="price-text">{formatCurrency(v.total || 0)}</td>
                                    <td><button className="btn-circle-print" onClick={() => generarPDF(v, v.detalles)}>🖨️</button></td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                    {!loading && ventasFiltradas.length === 0 && (
                        <div className="empty-state">No se encontraron ventas.</div>
                    )}
                </div>

                {!loading && ventasFiltradas.length > 0 && (
                    <div className="pagination-container">
                        <button className="pagination-btn" onClick={() => goToPage(currentPage - 1)} disabled={currentPage === 1}>← Anterior</button>
                        <div className="pagination-pages">{renderPageNumbers()}</div>
                        <button className="pagination-btn" onClick={() => goToPage(currentPage + 1)} disabled={currentPage === totalPages}>Siguiente →</button>
                    </div>
                )}
            </div>

            {/* DRAWER (POS) con inteligencia */}
            {drawerOpen && (
                <div className="glass-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="pos-drawer" onClick={e => e.stopPropagation()}>
                        <div className="drawer-nav">
                            <div className="mode-switch">
                                <button className={tipoVenta === 'rapida' ? 'active' : ''} onClick={() => { setTipoVenta('rapida'); setClienteSeleccionado(clientes.find(c => c.nombre.toLowerCase().includes("consumidor"))); }}>Rápida</button>
                                <button className={tipoVenta === 'cliente' ? 'active' : ''} onClick={() => { setTipoVenta('cliente'); setClienteSeleccionado(null); }}>A Cliente</button>
                            </div>
                            <button className="close-drawer-btn" onClick={() => setDrawerOpen(false)}>×</button>
                        </div>

                        <div className="drawer-scrollable-content">
                            {tipoVenta === 'cliente' && (
                                <section className="pos-section">
                                    {!clienteSeleccionado ? (
                                        <input className="pos-input-sm" placeholder="Buscar cliente..." onChange={e => {
                                            const c = clientes.find(cli => cli.nombre.toLowerCase().includes(e.target.value.toLowerCase()));
                                            if(c && e.target.value.length > 2) setClienteSeleccionado(c);
                                        }} />
                                    ) : (
                                        <div className="selection-active-card">
                                            <div className="active-details">
                                                <strong>{clienteSeleccionado.nombre}</strong>
                                                <span className="success">Saldo: {formatCurrency(round2(clienteSeleccionado.saldo || 0))}</span>
                                            </div>
                                            <button className="btn-reset-sm" onClick={() => setClienteSeleccionado(null)}>Cambiar</button>
                                        </div>
                                    )}

                                    {/* Contexto del cliente */}
                                    {contextoCliente && (
                                        <div className="cliente-contexto">
                                            <div className="contexto-title">📊 Historial del cliente</div>
                                            <div className="frecuentes">
                                                <div className="subtitle">🔄 Productos más comprados:</div>
                                                <div className="frecuentes-list">
                                                    {contextoCliente.productosFrecuentesCliente?.map((p, i) => (
                                                        <button key={i} className="chip-sugerencia" onClick={() => agregarComplementario(p)}>
                                                            {p.nombre}
                                                        </button>
                                                    ))}
                                                </div>
                                            </div>
                                            <div className="ultimas-compras">
                                                <div className="subtitle">📅 Últimas facturas:</div>
                                                <ul>
                                                    {contextoCliente.ultimasCompras?.map((compra, i) => <li key={i}>{compra}</li>)}
                                                </ul>
                                            </div>
                                        </div>
                                    )}
                                </section>
                            )}

                            <section className="pos-section">
                                <input
                                    className="pos-input-sm"
                                    placeholder="Buscar medicamento..."
                                    value={busquedaMedicamento}
                                    onChange={e => handleBuscarMedicamento(e.target.value)}
                                />
                                {loteRecomendado && (
                                    <div className="sugerencia-fifo">
                                        <div className="sugerencia-title">⚡ Lote recomendado (más próximo a vencer)</div>
                                        <div className="sugerencia-card">
                                            <div className="sugerencia-info">
                                                <strong>{loteRecomendado.nombre}</strong>
                                                <small>Vence: {new Date(loteRecomendado.fechaVencimiento).toLocaleDateString()}</small>
                                                <small>Stock disponible: {loteRecomendado.cantidadSugerida}</small>
                                            </div>
                                            <button className="btn-sugerencia" onClick={() => agregarMedicamento(loteRecomendado)}>
                                                Agregar
                                            </button>
                                        </div>
                                    </div>
                                )}

                                <div className="results-grid">
                                    {lotesConStock.flatMap(l =>
                                        l.detalles.map(d => {
                                            const med = medicamentos.find(m => m.id === d.medicamentoId);
                                            return {
                                                ...d,
                                                loteNum: l.numeroLote,
                                                imagen: med?.imagen,
                                                medicamentoNombre: med?.nombre || d.medicamentoNombre,
                                                stockVenta: d.stockVenta,
                                                tieneUbicacion: d.stockUbicacion > 0,
                                                precioUnitario: med?.precioUnitario
                                            };
                                        })
                                    )
                                        .filter(d => d.medicamentoNombre.toLowerCase().includes(busquedaMedicamento.toLowerCase()) && d.stockVenta > 0)
                                        .slice(0, 6)
                                        .map(m => (
                                            <button key={m.id} className="result-card" onClick={() => agregarMedicamento(m)}>
                                                {m.imagen && (
                                                    <img
                                                        src={`http://localhost:8080/${m.imagen.replace(/\\/g, "/")}`}
                                                        alt="med"
                                                        style={{ width: '45px', height: '45px', objectFit: 'cover', borderRadius: '6px', marginRight: '10px' }}
                                                    />
                                                )}
                                                <div className="card-info">
                                                    <span className="card-title">{m.medicamentoNombre}</span>
                                                    <span className="card-sub">{m.loteNum} | Stock: {m.stockVenta}</span>
                                                    {!m.tieneUbicacion && m.cantidad > 0 && (
                                                        <span className="warning-badge">⚠️ Sin ubicación</span>
                                                    )}
                                                </div>
                                                <div className="card-price">{formatCurrency(m.precioUnitario)}</div>
                                            </button>
                                        ))}
                                </div>

                                {complementarios.length > 0 && (
                                    <div className="complementarios-section">
                                        <div className="complementarios-title">🛒 Productos complementarios</div>
                                        <div className="complementarios-grid">
                                            {complementarios.map((prod, idx) => (
                                                <button key={idx} className="complementario-card" onClick={() => agregarComplementario(prod)}>
                                                    {prod.nombre}
                                                    <small>{prod.mensaje}</small>
                                                </button>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </section>

                            <section className="pos-section">
                                {detallesVenta.map((d, i) => (
                                    <div key={i} className="cart-row-sm">
                                        <div className="cart-info-sm">
                                            <strong>{d.medicamentoNombre}</strong>
                                            <span>{formatCurrency(d.precioUnitario)}</span>
                                        </div>
                                        <div className="cart-ctrls-sm">
                                            <input
                                                type="number"
                                                className="qty-input-sm"
                                                min="1"
                                                value={d.cantidad}
                                                onChange={e => {
                                                    const v = parseInt(e.target.value) || 1;
                                                    const stockVenta = getStockVenta(d.loteDetalleId);
                                                    if (v > stockVenta) {
                                                        alert(`Solo hay ${stockVenta} unidades disponibles.`);
                                                        return;
                                                    }
                                                    setDetallesVenta(prev => prev.map((p, idx) => idx === i ? { ...p, cantidad: v, subtotal: v * p.precioUnitario } : p));
                                                }}
                                            />
                                            <span className="item-total-sm">{formatCurrency(d.subtotal)}</span>
                                            <button className="btn-remove-sm" onClick={() => setDetallesVenta(detallesVenta.filter((_, idx) => idx !== i))}>×</button>
                                        </div>
                                    </div>
                                ))}
                            </section>
                        </div>

                        <div className="pos-fixed-footer">
                            <div className="payment-summary-card">
                                <div className="payment-row main-total"><span>Total Final</span><span>{formatCurrency(total)}</span></div>
                                <div className="payment-grid">
                                    {tipoVenta === 'cliente' && (
                                        <div className="pay-field">
                                            <label>Usar Saldo</label>
                                            <input type="number" className="pay-input" value={montoUsarSaldo} onChange={e => setMontoUsarSaldo(Math.min(parseFloat(e.target.value) || 0, clienteSeleccionado?.saldo || 0, total))} />
                                        </div>
                                    )}
                                    <div className="pay-field"><label>Efectivo</label><input type="number" className="pay-input" value={efectivoRecibido} onChange={e => setEfectivoRecibido(round2(parseFloat(e.target.value) || 0))} /></div>
                                </div>
                                {cambio > 0 && <div className="change-indicator">Cambio: {formatCurrency(cambio)}</div>}
                            </div>
                            <div className="pos-final-actions">
                                <button className="btn-cancel-final" onClick={() => setDrawerOpen(false)}>Cancelar</button>
                                <button className="btn-confirm-final" onClick={finalizarVenta} disabled={detallesVenta.length === 0}>Confirmar y Pagar</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}