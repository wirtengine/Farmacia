import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import axios from '../../services/axiosConfig';
import { useNavigate } from 'react-router-dom';
import { FiEye } from 'react-icons/fi'; // Instalar: npm install react-icons

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

    const handleVerPdf = async (id) => {
        try {
            const response = await axios.get(`/api/devoluciones/${id}/pdf`, {
                responseType: 'blob',
            });

            // Verificar si la respuesta es un PDF
            const contentType = response.headers['content-type'];
            if (contentType !== 'application/pdf') {
                console.error('Error: el servidor no devolvió un PDF', response.data);
                alert('Error al descargar el PDF: el servidor no devolvió un PDF');
                return;
            }

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `devolucion-${id}.pdf`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (err) {
            console.error('Error al descargar PDF:', err);
            alert('Error al descargar el PDF. Consulta la consola para más detalles.');
        }
    };

    if (loading) return <div className="container">Cargando...</div>;
    if (error) return <div className="container" style={{ color: 'red' }}>{error}</div>;

    return (
        <div className="container">
            <div className="card">
                <div className="flex-between">
                    <h2>Devoluciones</h2>
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
                            <th>Acciones</th>
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
                                <td>
                                    <button
                                        className="btn btn-info btn-sm"
                                        onClick={() => handleVerPdf(d.id)}
                                        style={{ marginRight: '5px' }}
                                    >
                                        <FiEye style={{ marginRight: '4px' }} /> Ver
                                    </button>
                                    {user?.rol === 'ADMIN' && d.estado === 'PENDIENTE' && (
                                        <button
                                            className="btn btn-warning btn-sm"
                                            onClick={() => navigate(`/admin/devoluciones/${d.id}/aprobar`)}
                                        >
                                            Procesar
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        {devoluciones.length === 0 && (
                            <tr>
                                <td colSpan="9" style={{ textAlign: 'center' }}>
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