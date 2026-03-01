import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import axios from '../../services/axiosConfig';
import { useNavigate } from 'react-router-dom';

const DevolucionList = () => {
    const { user } = useContext(AuthContext);
    const [devoluciones, setDevoluciones] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const navigate = useNavigate();
    const basePath = user?.rol === 'ADMIN' ? '/admin' : '/vendedor';

    useEffect(() => {
        fetchDevoluciones();
    }, []);

    const fetchDevoluciones = async () => {
        try {
            const url = user?.rol === 'ADMIN' ? '/api/devoluciones' : '/api/devoluciones/mis-solicitudes';
            const res = await axios.get(url);
            setDevoluciones(res.data);
        } catch (err) {
            setError('Error al cargar devoluciones');
        } finally {
            setLoading(false);
        }
    };

    const getBadgeClass = (estado) => {
        switch (estado) {
            case 'PENDIENTE': return 'badge-warning';
            case 'APROBADA': return 'badge-success';
            case 'RECHAZADA': return 'badge-danger';
            default: return '';
        }
    };

    if (loading) return <div className="container">Cargando...</div>;
    if (error) return <div className="container" style={{ color: 'red' }}>{error}</div>;

    return (
        <div className="container">
            <div className="card">
                <div className="flex-between">
                    <h2>Devoluciones</h2>
                    {/* El botón de solicitar devolución aparece para ADMIN y VENDEDOR */}
                    {user && (user.rol === 'ADMIN' || user.rol === 'VENDEDOR') && (
                        <button className="btn btn-primary" onClick={() => navigate(`${basePath}/devoluciones/nueva`)}>
                            Solicitar Devolución
                        </button>
                    )}
                </div>
                <div style={{ overflowX: 'auto' }}>
                    <table className="table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Factura</th>
                            <th>Vendedor</th>
                            <th>Producto</th>
                            <th>Cantidad</th>
                            <th>Motivo</th>
                            <th>Estado</th>
                            <th>Fecha Solicitud</th>
                            {user?.rol === 'ADMIN' && <th>Acciones</th>}
                        </tr>
                        </thead>
                        <tbody>
                        {devoluciones.map(d => (
                            <tr key={d.id}>
                                <td>{d.id}</td>
                                <td>{d.numeroFactura}</td>
                                <td>{d.vendedorNombre}</td>
                                <td>{d.medicamentoNombre || 'Toda la venta'}</td>
                                <td>{d.cantidad}</td>
                                <td>{d.motivo}</td>
                                <td>
                                        <span className={`badge ${getBadgeClass(d.estado)}`}>
                                            {d.estado}
                                        </span>
                                </td>
                                <td>{new Date(d.fechaSolicitud).toLocaleDateString()}</td>
                                {user?.rol === 'ADMIN' && d.estado === 'PENDIENTE' && (
                                    <td>
                                        <button
                                            className="btn btn-warning btn-sm"
                                            onClick={() => navigate(`/admin/devoluciones/${d.id}/aprobar`)}
                                        >
                                            Procesar
                                        </button>
                                    </td>
                                )}
                            </tr>
                        ))}
                        {devoluciones.length === 0 && (
                            <tr>
                                <td colSpan={user?.rol === 'ADMIN' ? 9 : 8} style={{ textAlign: 'center' }}>
                                    No hay devoluciones
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default DevolucionList;