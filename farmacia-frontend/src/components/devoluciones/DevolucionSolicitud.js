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
    const [detalleSeleccionado, setDetalleSeleccionado] = useState(null);
    const [cantidad, setCantidad] = useState(1);
    const [motivo, setMotivo] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

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
            setDetalleSeleccionado(null);
            setCantidad(1);
        } else {
            const venta = ventas.find(v => v.id === parseInt(id));
            setVentaSeleccionada(venta);
            setDetalleSeleccionado(null);
            setCantidad(1);
        }
    };

    const handleDetalleChange = (e) => {
        const id = e.target.value;
        if (!id) {
            setDetalleSeleccionado(null);
            setCantidad(1);
        } else {
            const det = ventaSeleccionada.detalles.find(d => d.id === parseInt(id));
            setDetalleSeleccionado(det);
            setCantidad(det.cantidad);
        }
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

        const payload = {
            ventaId: ventaSeleccionada.id,
            detalleId: detalleSeleccionado?.id || null,
            cantidad,
            motivo,
        };

        try {
            await axios.post('/api/devoluciones/solicitar', payload);
            setSuccess('Solicitud enviada correctamente');
            setTimeout(() => navigate(`${basePath}/devoluciones`), 2000);
        } catch (err) {
            setError(err.response?.data?.message || 'Error al solicitar devolución');
        }
    };

    if (ventasLoading) return <div className="container">Cargando ventas...</div>;

    return (
        <div className="container">
            <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
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
                            <div style={{ marginBottom: '15px' }}>
                                <label>Producto a devolver:</label>
                                <select
                                    value={detalleSeleccionado?.id || ''}
                                    onChange={handleDetalleChange}
                                    className="input-search"
                                >
                                    <option value="">-- Devolución total de la venta --</option>
                                    {ventaSeleccionada.detalles.map(d => (
                                        <option key={d.id} value={d.id}>
                                            {d.medicamentoNombre} (x{d.cantidad}) - C${d.subtotal?.toFixed(2)}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div style={{ marginBottom: '15px' }}>
                                <label>Cantidad a devolver:</label>
                                <input
                                    type="number"
                                    min="1"
                                    max={detalleSeleccionado ? detalleSeleccionado.cantidad : ventaSeleccionada.detalles.reduce((acc, d) => acc + d.cantidad, 0)}
                                    value={cantidad}
                                    onChange={(e) => setCantidad(parseInt(e.target.value) || 1)}
                                    className="input-search"
                                    required
                                />
                            </div>

                            <div style={{ marginBottom: '15px' }}>
                                <label>Motivo de la devolución:</label>
                                <textarea
                                    value={motivo}
                                    onChange={(e) => setMotivo(e.target.value)}
                                    className="input-search"
                                    rows="3"
                                    required
                                    placeholder="Ej: Producto dañado, fecha de vencimiento próxima, etc."
                                />
                            </div>
                        </>
                    )}

                    <div className="flex" style={{ justifyContent: 'flex-end', gap: '10px' }}>
                        <button type="button" className="btn" onClick={() => navigate(`${basePath}/devoluciones`)}>
                            Cancelar
                        </button>
                        <button type="submit" className="btn btn-primary">
                            Enviar solicitud
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default DevolucionSolicitud;