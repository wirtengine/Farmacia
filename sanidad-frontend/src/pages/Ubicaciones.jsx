import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { listarRacks, obtenerRack, crearRack, eliminarRack } from '../services/racks';
import { listarUbicacionesPorRack, asignarUbicacion, eliminarUbicacion, listarTodasUbicaciones } from '../services/ubicaciones';
import { listarLotes } from '../services/lotes';
import { listarMedicamentos } from '../services/medicamentos';
import RackVisualization from '../components/RackVisualization';
import './Ubicaciones.css';
import {
    alertaConfirmacion,
    alertaExito,
    alertaError
} from '../alertas';
export default function Ubicaciones() {
    const { user } = useAuth();
    const esAdmin = user?.rol === 'ADMIN';

    // --- ESTADOS ---
    const [racks, setRacks] = useState([]);
    const [rackSeleccionado, setRackSeleccionado] = useState(null);
    const [ubicaciones, setUbicaciones] = useState([]);
    const [lotes, setLotes] = useState([]);
    const [medicamentos, setMedicamentos] = useState([]);
    const [todasUbicaciones, setTodasUbicaciones] = useState([]);

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ text: '', type: '' });
    const [drawerNuevoOpen, setDrawerNuevoOpen] = useState(false);
    const [drawerAsignarOpen, setDrawerAsignarOpen] = useState(false);
    const [drawerMoverOpen, setDrawerMoverOpen] = useState(false);

    const [nuevoRack, setNuevoRack] = useState({ nombre: '', ancho: 4, alto: 4, profundidad: 2 });
    const [searchTerm, setSearchTerm] = useState('');
    const [celdaSeleccionada, setCeldaSeleccionada] = useState(null);
    const [detalleSeleccionado, setDetalleSeleccionado] = useState(null);
    const [cantidad, setCantidad] = useState(1);
    const [ubicacionAMover, setUbicacionAMover] = useState(null);

    // --- ESTADOS PARA MOVIMIENTO CON EL MOUSE ---
    const [modoMovimiento, setModoMovimiento] = useState(false);
    const [origenMovimiento, setOrigenMovimiento] = useState(null);

    const resetearSeleccion = useCallback(() => {
        setCeldaSeleccionada(null);
        setDetalleSeleccionado(null);
        setCantidad(1);
        setSearchTerm('');
        setDrawerAsignarOpen(false);
        setDrawerNuevoOpen(false);
        setDrawerMoverOpen(false);
        setUbicacionAMover(null);
        setModoMovimiento(false);
        setOrigenMovimiento(null);
    }, []);

    const cargarDatosBase = useCallback(async () => {
        setLoading(true);
        try {
            const [racksRes, lotesRes, medsRes, todasUbicRes] = await Promise.all([
                listarRacks(),
                listarLotes(),
                listarMedicamentos(),
                listarTodasUbicaciones()
            ]);
            setRacks(racksRes.data || []);
            setLotes(lotesRes.data || []);
            setMedicamentos(medsRes.data || []);
            setTodasUbicaciones(todasUbicRes.data || []);
        } catch (err) {
            setMessage({ text: 'Error de conexión con el servidor', type: 'error' });
        } finally { setLoading(false); }
    }, []);

    useEffect(() => {
        cargarDatosBase();
    }, [cargarDatosBase]);

    const handleSeleccionarRack = async (rackId) => {
        setLoading(true);
        try {
            const [rackRes, ubicRes] = await Promise.all([
                obtenerRack(rackId),
                listarUbicacionesPorRack(rackId)
            ]);
            setRackSeleccionado(rackRes.data);
            setUbicaciones(ubicRes.data);
            resetearSeleccion();
        } catch {
            setMessage({ text: 'Error al cargar estante', type: 'error' });
        } finally { setLoading(false); }
    };

    // Calcular stock disponible total (considerando todas las ubicaciones)
    const obtenerStockDisponible = (loteDetalleId) => {
        const detalle = lotes.flatMap(l => l.detalles).find(d => d.id === loteDetalleId);
        if (!detalle) return 0;
        const yaUbicadoTotal = todasUbicaciones.reduce((acc, u) => u.loteDetalleId === loteDetalleId ? acc + u.cantidad : acc, 0);
        return detalle.cantidad - yaUbicadoTotal;
    };

    const lotesConStockReal = useMemo(() => {
        const term = searchTerm.toLowerCase().trim();
        return lotes.map(lote => {
            const detallesCalculados = lote.detalles?.map(det => {
                const stockDisp = obtenerStockDisponible(det.id);
                return { ...det, stockDisponible: stockDisp };
            }).filter(d => d.stockDisponible > 0) || [];
            return { ...lote, detallesCalculados };
        }).filter(lote => {
            if (!lote.activo || lote.detallesCalculados.length === 0) return false;
            const match = lote.factura?.toLowerCase().includes(term) ||
                lote.detallesCalculados.some(d => medicamentos.find(m => m.id === d.medicamentoId)?.nombre.toLowerCase().includes(term));
            return !term || match;
        });
    }, [lotes, todasUbicaciones, medicamentos, searchTerm]);

    // --- LÓGICA DE ASIGNACIÓN EN CASCADA ---
    const handleAsignarCascada = async () => {
        if (!detalleSeleccionado || !celdaSeleccionada || !rackSeleccionado) return;

        const stockDisp = obtenerStockDisponible(detalleSeleccionado.id);
        if (cantidad > stockDisp) {
            setMessage({ text: `Stock insuficiente. Solo hay ${stockDisp} unidades disponibles.`, type: 'error' });
            return;
        }

        setLoading(true);
        let n = parseInt(celdaSeleccionada.nivel);
        let c = parseInt(celdaSeleccionada.columna);
        let p = parseInt(celdaSeleccionada.profundidadIndex);
        let restantes = cantidad;
        const slotsParaEnviar = [];

        const totalCeldas = rackSeleccionado.alto * rackSeleccionado.ancho * rackSeleccionado.profundidad;
        let intentos = 0;

        while (restantes > 0 && intentos < totalCeldas) {
            const ocupada = ubicaciones.some(u =>
                u.nivel === n && u.columna === c && u.profundidadIndex === p && u.activo !== false
            );

            if (!ocupada) {
                slotsParaEnviar.push({
                    loteDetalleId: parseInt(detalleSeleccionado.id),
                    rackId: parseInt(rackSeleccionado.id),
                    nivel: n,
                    columna: c,
                    profundidadIndex: p,
                    cantidad: 1
                });
                restantes--;
            }

            p++;
            if (p >= rackSeleccionado.profundidad) { p = 0; c++; }
            if (c >= rackSeleccionado.ancho) { c = 0; n++; }
            if (n >= rackSeleccionado.alto) { n = 0; }

            intentos++;
        }

        if (restantes > 0) {
            setMessage({ text: `Espacio insuficiente. Faltaron ${restantes} unidades por ubicar.`, type: 'error' });
        }

        try {
            await Promise.all(slotsParaEnviar.map(slot => asignarUbicacion(slot)));
            await cargarDatosBase();
            const resUbic = await listarUbicacionesPorRack(rackSeleccionado.id);
            setUbicaciones(resUbic.data);
            resetearSeleccion();
            setMessage({ text: `${slotsParaEnviar.length} unidades ubicadas con éxito.`, type: 'success' });
        } catch (err) {
            console.error(err);
            setMessage({ text: 'Error al procesar la ubicación', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    // --- ELIMINAR UBICACIÓN ---
    const confirmarEliminarUbicacion = async (ubicacion) => {
        if (!window.confirm(`¿Eliminar este producto de la celda?`)) return;
        try {
            await eliminarUbicacion(ubicacion.id);
            await cargarDatosBase();
            if (rackSeleccionado) {
                const resUbic = await listarUbicacionesPorRack(rackSeleccionado.id);
                setUbicaciones(resUbic.data);
            }
            setMessage({ text: 'Producto eliminado del estante', type: 'success' });
        } catch (err) {
            console.error(err);
            setMessage({ text: 'Error al eliminar', type: 'error' });
        }
    };

    // --- MOVER UBICACIÓN (mediante drawer) ---
    const iniciarMover = (ubicacion) => {
        setUbicacionAMover(ubicacion);
        setDrawerMoverOpen(true);
        const detalle = lotes.flatMap(l => l.detalles).find(d => d.id === ubicacion.loteDetalleId);
        setDetalleSeleccionado(detalle);
        setCantidad(ubicacion.cantidad);
        setDrawerAsignarOpen(false);
        setDrawerNuevoOpen(false);
    };

    const handleMover = async () => {
        if (!ubicacionAMover || !celdaSeleccionada) {
            setMessage({ text: 'Seleccione una celda destino', type: 'error' });
            return;
        }
        const ocupada = ubicaciones.some(u =>
            u.nivel === celdaSeleccionada.nivel &&
            u.columna === celdaSeleccionada.columna &&
            u.profundidadIndex === celdaSeleccionada.profundidadIndex &&
            u.activo !== false && u.id !== ubicacionAMover.id
        );
        if (ocupada) {
            setMessage({ text: 'La celda destino ya está ocupada', type: 'error' });
            return;
        }

        setLoading(true);
        try {
            await asignarUbicacion({
                loteDetalleId: ubicacionAMover.loteDetalleId,
                rackId: rackSeleccionado.id,
                nivel: celdaSeleccionada.nivel,
                columna: celdaSeleccionada.columna,
                profundidadIndex: celdaSeleccionada.profundidadIndex,
                cantidad: ubicacionAMover.cantidad
            });
            await eliminarUbicacion(ubicacionAMover.id);
            await cargarDatosBase();
            const resUbic = await listarUbicacionesPorRack(rackSeleccionado.id);
            setUbicaciones(resUbic.data);
            resetearSeleccion();
            setMessage({ text: 'Producto movido con éxito', type: 'success' });
        } catch (err) {
            console.error(err);
            setMessage({ text: 'Error al mover el producto', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    // --- MOVIMIENTO CON EL MOUSE ---
    const iniciarMovimiento = (ubicacion, nivel, columna, profundidadIndex) => {
        if (!esAdmin) return;
        setDrawerAsignarOpen(false);
        setDrawerMoverOpen(false);
        setDrawerNuevoOpen(false);
        setModoMovimiento(true);
        setOrigenMovimiento({
            ubicacionId: ubicacion.id,
            loteDetalleId: ubicacion.loteDetalleId,
            cantidad: ubicacion.cantidad,
            nivel,
            columna,
            profundidadIndex,
            medicamentoNombre: (() => {
                const lote = lotes.find(l => l.detalles?.some(d => d.id === ubicacion.loteDetalleId));
                const det = lote?.detalles.find(d => d.id === ubicacion.loteDetalleId);
                const med = medicamentos.find(m => m.id === det?.medicamentoId);
                return med?.nombre || 'Producto';
            })()
        });
        setMessage({ text: 'Modo movimiento activado. Haz clic en una celda libre para mover el producto.', type: 'info' });
    };

    const cancelarMovimiento = () => {
        setModoMovimiento(false);
        setOrigenMovimiento(null);
        setMessage({ text: 'Movimiento cancelado', type: 'info' });
    };

    // 🔥 FUNCIÓN PRINCIPAL QUE MANEJA EL CLICK EN LA CELDA (CORREGIDA)
    const handleCeldaClick = async (nivel, columna, profundidadIndex) => {
        // MODO MOVIMIENTO: intentar mover
        if (modoMovimiento && origenMovimiento) {
            // Si es la misma celda, cancelar
            if (origenMovimiento.nivel === nivel && origenMovimiento.columna === columna && origenMovimiento.profundidadIndex === profundidadIndex) {
                cancelarMovimiento();
                return;
            }

            if (!rackSeleccionado) {
                cancelarMovimiento();
                return;
            }

            try {
                // 1. Obtener ubicaciones actualizadas desde el servidor
                const freshUbicRes = await listarUbicacionesPorRack(rackSeleccionado.id);
                const freshUbicaciones = freshUbicRes.data;

                // 2. Verificar si la celda destino está ocupada
                const ocupadaDestino = freshUbicaciones.some(u =>
                    u.nivel === nivel && u.columna === columna && u.profundidadIndex === profundidadIndex && u.activo !== false
                );

                if (ocupadaDestino) {
                    setMessage({ text: 'La celda destino está ocupada. Movimiento cancelado.', type: 'error' });
                    cancelarMovimiento();
                    return;
                }

                setLoading(true);

                // 3. Eliminar la ubicación origen PRIMERO
                await eliminarUbicacion(origenMovimiento.ubicacionId);

                // 4. Asignar la nueva ubicación
                await asignarUbicacion({
                    loteDetalleId: origenMovimiento.loteDetalleId,
                    rackId: rackSeleccionado.id,
                    nivel,
                    columna,
                    profundidadIndex,
                    cantidad: origenMovimiento.cantidad
                });

                // 5. Recargar datos y actualizar el estado
                await cargarDatosBase();
                const finalUbicRes = await listarUbicacionesPorRack(rackSeleccionado.id);
                setUbicaciones(finalUbicRes.data);
                setMessage({ text: 'Producto movido con éxito', type: 'success' });
            } catch (err) {
                console.error(err);
                setMessage({ text: err.response?.data?.message || 'Error al mover el producto', type: 'error' });
            } finally {
                cancelarMovimiento();
                setLoading(false);
            }
            return;
        }

        // MODO NORMAL: seleccionar celda y abrir drawer
        setCeldaSeleccionada({ nivel, columna, profundidadIndex });
        setDrawerAsignarOpen(true);
    };

    // --- INFO CELDA ---
    const infoCelda = useMemo(() => {
        if (!celdaSeleccionada) return null;
        const u = ubicaciones.find(u =>
            u.nivel === celdaSeleccionada.nivel &&
            u.columna === celdaSeleccionada.columna &&
            u.profundidadIndex === celdaSeleccionada.profundidadIndex
        );
        if (!u) return null;
        const lote = lotes.find(l => l.detalles?.some(d => d.id === u.loteDetalleId));
        const det = lote?.detalles.find(d => d.id === u.loteDetalleId);
        const med = medicamentos.find(m => m.id === det?.medicamentoId);
        return { nombre: med?.nombre, factura: lote?.factura, vence: lote?.fechaVencimiento, ubicacionId: u.id, loteDetalleId: u.loteDetalleId };
    }, [celdaSeleccionada, ubicaciones, lotes, medicamentos]);

    // --- ELIMINAR RACK ---
    const eliminarRackHandler = async (rackId) => {

        const result = await alertaConfirmacion({
            titulo: 'Eliminar estante',
            texto: '¿Está seguro de eliminar este estante? Se perderán todas las ubicaciones asociadas.',
            confirmar: 'Eliminar',
            cancelar: 'Cancelar',
            icono: 'warning'
        });

        if (!result.isConfirmed) return;

        try {

            await eliminarRack(rackId);

            await cargarDatosBase();

            if (rackSeleccionado?.id === rackId) {
                setRackSeleccionado(null);
                setUbicaciones([]);
            }

            alertaExito('Estante eliminado');

        } catch (err) {

            alertaError('Error al eliminar estante');

        }
    };

    // --- CREAR RACK ---
    const handleCrearRack = async () => {
        if (!nuevoRack.nombre.trim()) {
            setMessage({ text: 'El nombre es obligatorio', type: 'error' });
            return;
        }
        setLoading(true);
        try {
            const request = {
                nombre: nuevoRack.nombre,
                ancho: parseInt(nuevoRack.ancho),
                alto: parseInt(nuevoRack.alto),
                profundidad: parseInt(nuevoRack.profundidad),
                descripcion: ""
            };
            await crearRack(request);
            await cargarDatosBase();
            resetearSeleccion();
            setMessage({ text: 'Estante creado correctamente', type: 'success' });
        } catch (error) {
            setMessage({ text: 'Error al crear estante', type: 'error' });
        } finally { setLoading(false); }
    };

    return (
        <div className="module-container">
            {(drawerNuevoOpen || drawerAsignarOpen || drawerMoverOpen) && (
                <div className="drawer-overlay" onClick={resetearSeleccion}></div>
            )}

            <header className="module-header">
                <div className="title-group">
                    <h1>Gestión de Ubicaciones</h1>
                    <span className="badge-blue">{racks.length} Estantes</span>
                </div>
                <div className="header-actions">
                    <button className="btn-primary-compact" onClick={cargarDatosBase} style={{ marginRight: '10px' }}>
                        ↻ Recargar
                    </button>
                    {esAdmin && (
                        <button className="btn-save-final" style={{ width: 'auto', padding: '10px 20px' }} onClick={() => setDrawerNuevoOpen(true)}>
                            + Nuevo Estante
                        </button>
                    )}
                </div>
            </header>

            {message.text && (
                <div className={`alert-banner ${message.type}`} onClick={() => setMessage({ text: '', type: '' })}>
                    {message.text}
                </div>
            )}

            <div className="main-layout">
                <aside className="side-panel">
                    <div className="racks-list">
                        {racks.map(r => (
                            <div key={r.id} className={`rack-card ${rackSeleccionado?.id === r.id ? 'active' : ''}`}>
                                <div className="rack-info-mini" onClick={() => handleSeleccionarRack(r.id)}>
                                    <strong>{r.nombre}</strong>
                                    <small>{r.ancho}x{r.alto}x{r.profundidad}</small>
                                </div>
                                {esAdmin && (
                                    <button
                                        className="btn-delete-icon-small"
                                        onClick={(e) => { e.stopPropagation(); eliminarRackHandler(r.id); }}
                                        title="Eliminar estante"
                                    >
                                        🗑️
                                    </button>
                                )}
                            </div>
                        ))}
                    </div>
                </aside>

                <main className="content-area">
                    {rackSeleccionado ? (
                        <div className="view-container fade-in">
                            <RackVisualization
                                rack={rackSeleccionado}
                                ubicaciones={ubicaciones}
                                lotes={lotes}
                                medicamentos={medicamentos}
                                onSeleccionarCelda={handleCeldaClick}
                                seleccionActual={celdaSeleccionada}
                                modoMovimiento={modoMovimiento}
                                origenMovimiento={origenMovimiento}
                                onIniciarMovimiento={iniciarMovimiento}
                            />
                        </div>
                    ) : (
                        <div className="empty-state-large">Seleccione un estante del panel izquierdo</div>
                    )}
                </main>
            </div>

            {/* DRAWER ASIGNAR NUEVO MEDICAMENTO */}
            <aside className={`drawer-right ${drawerAsignarOpen ? 'open' : ''}`}>
                <div className="drawer-header">
                    <div>
                        <h4>Asignar producto</h4>
                        <span className="target-cell-badge">
                            Nivel {celdaSeleccionada ? celdaSeleccionada.nivel + 1 : '-'} ·
                            Col {celdaSeleccionada ? celdaSeleccionada.columna + 1 : '-'}
                        </span>
                    </div>
                    <button className="close-btn" onClick={resetearSeleccion}>&times;</button>
                </div>
                <div className="drawer-body">
                    {infoCelda ? (
                        <div className="info-ocupada-card">
                            <div style={{ fontSize: '3rem' }}>📦</div>
                            <h4>{infoCelda.nombre}</h4>
                            <p><strong>Lote:</strong> {infoCelda.factura}</p>
                            <p><strong>Vence:</strong> {infoCelda.vence}</p>
                            {esAdmin && (
                                <div className="action-buttons" style={{ marginTop: '15px', display: 'flex', gap: '10px' }}>
                                    <button
                                        className="btn-primary-compact"
                                        onClick={() => {
                                            const ubic = ubicaciones.find(u =>
                                                u.nivel === celdaSeleccionada.nivel &&
                                                u.columna === celdaSeleccionada.columna &&
                                                u.profundidadIndex === celdaSeleccionada.profundidadIndex
                                            );
                                            if (ubic) iniciarMovimiento(ubic, celdaSeleccionada.nivel, celdaSeleccionada.columna, celdaSeleccionada.profundidadIndex);
                                        }}
                                    >
                                        Mover a otra celda
                                    </button>
                                    <button className="btn-danger-compact" onClick={() => confirmarEliminarUbicacion({ id: infoCelda.ubicacionId })}>
                                        Eliminar
                                    </button>
                                </div>
                            )}
                        </div>
                    ) : (
                        <>
                            <input className="search-input" placeholder="Buscar medicina..." value={searchTerm} onChange={e => setSearchTerm(e.target.value)} />
                            <div className="lot-scroll-container">
                                {lotesConStockReal.map(lote => (
                                    <div key={lote.id} className="lot-group-card">
                                        <div className="lot-group-header">LOTE: {lote.factura}</div>
                                        {lote.detallesCalculados.map(det => (
                                            <div
                                                key={det.id}
                                                className={`med-item-row ${detalleSeleccionado?.id === det.id ? 'selected' : ''}`}
                                                onClick={() => { setDetalleSeleccionado(det); setCantidad(1); }}
                                            >
                                                <div className="med-info">
                                                    <span className="med-name">{medicamentos.find(m => m.id === det.medicamentoId)?.nombre}</span>
                                                    <span className="qty-tag">{det.stockDisponible} disponibles</span>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                ))}
                            </div>
                        </>
                    )}
                </div>
                {!infoCelda && detalleSeleccionado && (
                    <div className="drawer-footer">
                        <div className="stepper">
                            <button onClick={() => setCantidad(Math.max(1, cantidad - 1))}>-</button>
                            <span className="qty-val">{cantidad}</span>
                            <button onClick={() => setCantidad(Math.min(detalleSeleccionado.stockDisponible, cantidad + 1))}>+</button>
                        </div>
                        <button className="btn-save-final" onClick={handleAsignarCascada} disabled={loading}>
                            {loading ? 'Procesando...' : `Ubicar ${cantidad} uds.`}
                        </button>
                    </div>
                )}
            </aside>

            {/* DRAWER MOVER PRODUCTO (legado) */}
            <aside className={`drawer-right ${drawerMoverOpen ? 'open' : ''}`}>
                <div className="drawer-header">
                    <h4>Mover producto</h4>
                    <button className="close-btn" onClick={resetearSeleccion}>&times;</button>
                </div>
                <div className="drawer-body">
                    {ubicacionAMover && (
                        <>
                            <div className="info-producto-mover">
                                <p><strong>Producto:</strong> {medicamentos.find(m => m.id === detalleSeleccionado?.medicamentoId)?.nombre}</p>
                                <p><strong>Lote:</strong> {lotes.find(l => l.detalles?.some(d => d.id === ubicacionAMover.loteDetalleId))?.factura}</p>
                                <p><strong>Cantidad:</strong> {ubicacionAMover.cantidad} unidades</p>
                            </div>
                            <div className="info-celda-destino">
                                <p>Haz clic en una celda <strong>libre</strong> del estante para elegir destino.</p>
                                {celdaSeleccionada && (
                                    <div className="destino-seleccionado">
                                        Destino: Nivel {celdaSeleccionada.nivel + 1}, Columna {celdaSeleccionada.columna + 1}, Profundidad {celdaSeleccionada.profundidadIndex + 1}
                                    </div>
                                )}
                            </div>
                        </>
                    )}
                </div>
                <div className="drawer-footer">
                    <button className="btn-save-final" onClick={handleMover} disabled={!celdaSeleccionada || loading}>
                        Confirmar Mover
                    </button>
                    <button className="btn-cancel" onClick={resetearSeleccion}>Cancelar</button>
                </div>
            </aside>

            {/* DRAWER NUEVO RACK */}
            <aside className={`drawer-right ${drawerNuevoOpen ? 'open' : ''}`}>
                <div className="drawer-header">
                    <h3>Nuevo Rack</h3>
                    <button className="close-btn" onClick={resetearSeleccion}>&times;</button>
                </div>
                <div className="drawer-body">
                    <div className="form-group" style={{ marginBottom: '20px' }}>
                        <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>Nombre del Estante</label>
                        <input
                            className="search-input"
                            placeholder="Ej: Pasillo A - Estante 1"
                            value={nuevoRack.nombre}
                            onChange={e => setNuevoRack({...nuevoRack, nombre: e.target.value})}
                        />
                    </div>

                    <div className="dimensions-grid" style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                        {['alto', 'ancho', 'profundidad'].map(dim => (
                            <div key={dim} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px', background: '#f8f9fa', borderRadius: '8px' }}>
                                <span style={{ textTransform: 'capitalize', fontWeight: '500' }}>{dim === 'alto' ? 'Niveles' : dim === 'ancho' ? 'Columnas' : 'Fondo'}</span>
                                <div className="stepper" style={{ background: 'white' }}>
                                    <button onClick={() => setNuevoRack({...nuevoRack, [dim]: Math.max(1, nuevoRack[dim] - 1)})}>-</button>
                                    <span style={{ minWidth: '30px', textAlign: 'center', fontWeight: 'bold' }}>{nuevoRack[dim]}</span>
                                    <button onClick={() => setNuevoRack({...nuevoRack, [dim]: nuevoRack[dim] + 1})}>+</button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
                <div className="drawer-footer">
                    <button className="btn-save-final" onClick={handleCrearRack} disabled={loading}>
                        {loading ? 'Guardando...' : 'Guardar Estante'}
                    </button>
                </div>
            </aside>

            {/* BOTÓN FLOTANTE PARA CANCELAR MOVIMIENTO */}
            {modoMovimiento && (
                <div className="move-cancel-banner">
                    <span>✋ Modo movimiento activo</span>
                    <button onClick={cancelarMovimiento}>Cancelar</button>
                </div>
            )}
        </div>
    );
}