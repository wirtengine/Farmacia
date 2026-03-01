import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from '../../services/axiosConfig';

const DevolucionAprobacion = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [devolucion, setDevolucion] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [observacion, setObservacion] = useState('');
    const [procesando, setProcesando] = useState(false);

    const fetchDevolucion = useCallback(async () => {
        try {
            const res = await axios.get(`/api/devoluciones/${id}`);
            setDevolucion(res.data);
        } catch (err) {
            setError('Error al cargar la devolución');
        } finally {
            setLoading(false);
        }
    }, [id]);

    useEffect(() => {
        fetchDevolucion();
    }, [fetchDevolucion]);

    const handleProcesar = async (accion) => {
        if (accion === 'RECHAZAR' && !observacion.trim()) {
            alert('Debe ingresar una observación para rechazar');
            return;
        }
        setProcesando(true);
        try {
            await axios.post('/api/devoluciones/procesar', {
                devolucionId: parseInt(id),
                accion,
                observacion,
            });
            // Redirigir a la lista de ventas con estado de recarga
            navigate('/admin/ventas', { state: { recargar: true } });
        } catch (err) {
            alert('Error al procesar la devolución');
        } finally {
            setProcesando(false);
        }
    };

    if (loading) return <div className="container">Cargando...</div>;
    if (error) return <div className="container" style={{ color: 'red' }}>{error}</div>;

    return (
        <div className="container">
            <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
                <h2>Procesar Devolución</h2>
                <div style={{ marginBottom: '20px' }}>
                    <p><strong>Factura:</strong> {devolucion.numeroFactura}</p>
                    <p><strong>Vendedor:</strong> {devolucion.vendedorNombre}</p>
                    <p><strong>Producto:</strong> {devolucion.medicamentoNombre || 'Toda la venta'}</p>
                    <p><strong>Cantidad:</strong> {devolucion.cantidad}</p>
                    <p><strong>Motivo:</strong> {devolucion.motivo}</p>
                    <p><strong>Fecha solicitud:</strong> {new Date(devolucion.fechaSolicitud).toLocaleString()}</p>
                </div>

                <div style={{ marginBottom: '15px' }}>
                    <label>Observación (obligatorio para rechazar):</label>
                    <textarea
                        value={observacion}
                        onChange={(e) => setObservacion(e.target.value)}
                        className="input-search"
                        rows="3"
                        placeholder="Ingrese una observación..."
                    />
                </div>

                <div className="flex" style={{ justifyContent: 'flex-end', gap: '10px' }}>
                    <button
                        className="btn"
                        onClick={() => navigate('/admin/devoluciones')}
                        disabled={procesando}
                    >
                        Cancelar
                    </button>
                    <button
                        className="btn btn-success"
                        onClick={() => handleProcesar('APROBAR')}
                        disabled={procesando}
                    >
                        Aprobar
                    </button>
                    <button
                        className="btn btn-danger"
                        onClick={() => handleProcesar('RECHAZAR')}
                        disabled={procesando}
                    >
                        Rechazar
                    </button>
                </div>
            </div>
        </div>
    );
};

export default DevolucionAprobacion;