import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import axios from '../../services/axiosConfig';
import { useNavigate } from 'react-router-dom';

const DevolucionSolicitud = () => {
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const basePath = user?.rol === 'ADMIN' ? '/admin' : '/vendedor';

    const [ventas, setVentas] = useState([]);
    const [ventasLoading, setVentasLoading] = useState(true);
    const [ventaSeleccionada, setVentaSeleccionada] = useState(null);
    const [devolucionTotal, setDevolucionTotal] = useState(false);
    const [productosSeleccionados, setProductosSeleccionados] = useState([]); // array de { detalleId, max, cantidad, nombre }
    const [motivo, setMotivo] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [enviando, setEnviando] = useState(false);

    useEffect(() => {
        const fetchVentas = async () => {
            try {
                const url = user?.rol === 'ADMIN' ? '/api/ventas' : '/api/ventas/mis-ventas';
                const res = await axios.get(url);
                setVentas(res.data);
            } catch (err) {
                setError('Error al cargar las ventas');
            } finally {
                setVentasLoading(false);
            }
        };
        fetchVentas();
    }, [user]);

    const handleVentaChange = (e) => {
        const id = e.target.value;
        if (!id) {
            setVentaSeleccionada(null);
            setDevolucionTotal(false);
            setProductosSeleccionados([]);
            setMotivo('');
        } else {
            const venta = ventas.find(v => v.id === parseInt(id));
            setVentaSeleccionada(venta);
            setDevolucionTotal(false);
            setProductosSeleccionados([]);
            setMotivo('');
        }
    };

    const handleTotalChange = (e) => {
        const checked = e.target.checked;
        setDevolucionTotal(checked);
        if (checked) {
            setProductosSeleccionados([]); // limpiar selección parcial
        }
    };

    const handleProductoSeleccionado = (detalleId, checked) => {
        if (checked) {
            const detalle = ventaSeleccionada.detalles.find(d => d.id === detalleId);
            setProductosSeleccionados(prev => [
                ...prev,
                {
                    detalleId,
                    max: detalle.cantidad,
                    cantidad: detalle.cantidad,
                    nombre: detalle.medicamentoNombre
                }
            ]);
        } else {
            setProductosSeleccionados(prev => prev.filter(p => p.detalleId !== detalleId));
        }
    };

    const handleCantidadChange = (detalleId, nuevaCantidad) => {
        setProductosSeleccionados(prev =>
            prev.map(p =>
                p.detalleId === detalleId
                    ? { ...p, cantidad: Math.min(nuevaCantidad, p.max) }
                    : p
            )
        );
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!ventaSeleccionada) {
            setError('Debe seleccionar una venta');
            return;
        }
        if (!motivo.trim()) {
            setError('Debe ingresar un motivo');
            return;
        }
        if (!devolucionTotal && productosSeleccionados.length === 0) {
            setError('Debe seleccionar al menos un producto o marcar devolución total');
            return;
        }

        setEnviando(true);
        setError('');
        setSuccess('');

        try {
            if (devolucionTotal) {
                // Una sola solicitud con detalleId = null
                const payload = {
                    ventaId: ventaSeleccionada.id,
                    detalleId: null,
                    cantidad: 1, // el backend no usa esta cantidad en total, pero podemos poner cualquier cosa
                    motivo,
                };
                console.log('🔍 Enviando devolución total:', payload);
                await axios.post('/api/devoluciones/solicitar', payload);
                setSuccess('Solicitud de devolución total enviada correctamente');
            } else {
                // Múltiples solicitudes, una por producto seleccionado
                let exitosas = 0;
                for (const prod of productosSeleccionados) {
                    const payload = {
                        ventaId: ventaSeleccionada.id,
                        detalleId: prod.detalleId,
                        cantidad: prod.cantidad,
                        motivo,
                    };
                    console.log('🔍 Enviando devolución parcial:', payload);
                    await axios.post('/api/devoluciones/solicitar', payload);
                    exitosas++;
                }
                setSuccess(`${exitosas} solicitud(es) de devolución enviada(s) correctamente`);
            }

            setTimeout(() => navigate(`${basePath}/devoluciones`), 2000);
        } catch (err) {
            setError(err.response?.data?.message || 'Error al enviar solicitud(es)');
        } finally {
            setEnviando(false);
        }
    };

    if (ventasLoading) return <div className="container">Cargando ventas...</div>;

    return (
        <div className="container">
            <div className="card" style={{ maxWidth: '700px', margin: '0 auto' }}>
                <h2>Solicitar Devolución</h2>
                {error && <div style={{ color: 'red', marginBottom: '10px' }}>{error}</div>}
                {success && <div style={{ color: 'green', marginBottom: '10px' }}>{success}</div>}

                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '15px' }}>
                        <label>Seleccionar venta:</label>
                        <select
                            value={ventaSeleccionada?.id || ''}
                            onChange={handleVentaChange}
                            className="input-search"
                            required
                            disabled={enviando}
                        >
                            <option value="">-- Seleccione una venta --</option>
                            {ventas.map(v => (
                                <option key={v.id} value={v.id}>
                                    {v.numeroFactura} - {new Date(v.fecha).toLocaleDateString()} - {v.clienteNombre || 'Consumidor Final'} (C${v.total?.toFixed(2)})
                                </option>
                            ))}
                        </select>
                    </div>

                    {ventaSeleccionada && (
                        <>
                            <div style={{ marginBottom: '15px', padding: '10px', background: '#f9f9f9', borderRadius: '4px' }}>
                                <label style={{ fontWeight: 'bold' }}>
                                    <input
                                        type="checkbox"
                                        checked={devolucionTotal}
                                        onChange={handleTotalChange}
                                        disabled={enviando}
                                    /> Devolución total de la venta (se anulará la venta)
                                </label>
                                {devolucionTotal && (
                                    <p style={{ margin: '5px 0 0 20px', color: '#666', fontSize: '0.9em' }}>
                                        Al aprobar, se devolverá el stock de todos los productos y la venta quedará anulada.
                                    </p>
                                )}
                            </div>

                            {!devolucionTotal && (
                                <div style={{ marginBottom: '15px' }}>
                                    <h4>Seleccione productos a devolver:</h4>
                                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                        <thead>
                                        <tr>
                                            <th style={{ textAlign: 'left' }}>Producto</th>
                                            <th style={{ textAlign: 'center' }}>Cantidad máxima</th>
                                            <th style={{ textAlign: 'center' }}>Cantidad a devolver</th>
                                            <th style={{ textAlign: 'center' }}>Seleccionar</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {ventaSeleccionada.detalles.map(det => {
                                            const seleccionado = productosSeleccionados.find(p => p.detalleId === det.id);
                                            return (
                                                <tr key={det.id} style={{ borderBottom: '1px solid #ddd' }}>
                                                    <td>{det.medicamentoNombre}</td>
                                                    <td style={{ textAlign: 'center' }}>{det.cantidad}</td>
                                                    <td style={{ textAlign: 'center' }}>
                                                        {seleccionado ? (
                                                            <input
                                                                type="number"
                                                                min="1"
                                                                max={det.cantidad}
                                                                value={seleccionado.cantidad}
                                                                onChange={(e) => handleCantidadChange(det.id, parseInt(e.target.value) || 1)}
                                                                style={{ width: '80px' }}
                                                                disabled={enviando}
                                                            />
                                                        ) : (
                                                            <span>—</span>
                                                        )}
                                                    </td>
                                                    <td style={{ textAlign: 'center' }}>
                                                        <input
                                                            type="checkbox"
                                                            checked={!!seleccionado}
                                                            onChange={(e) => handleProductoSeleccionado(det.id, e.target.checked)}
                                                            disabled={enviando}
                                                        />
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                        </tbody>
                                    </table>
                                </div>
                            )}

                            <div style={{ marginBottom: '15px' }}>
                                <label>Motivo de la devolución:</label>
                                <textarea
                                    value={motivo}
                                    onChange={(e) => setMotivo(e.target.value)}
                                    className="input-search"
                                    rows="3"
                                    required
                                    placeholder="Ej: Producto dañado, fecha de vencimiento próxima, etc."
                                    disabled={enviando}
                                />
                            </div>

                            <div className="flex" style={{ justifyContent: 'flex-end', gap: '10px' }}>
                                <button
                                    type="button"
                                    className="btn"
                                    onClick={() => navigate(`${basePath}/devoluciones`)}
                                    disabled={enviando}
                                >
                                    Cancelar
                                </button>
                                <button type="submit" className="btn btn-primary" disabled={enviando}>
                                    {enviando ? 'Enviando...' : 'Enviar solicitud(es)'}
                                </button>
                            </div>
                        </>
                    )}
                </form>
            </div>
        </div>
    );
};

export default DevolucionSolicitud;