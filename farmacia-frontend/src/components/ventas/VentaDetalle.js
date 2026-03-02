import React, { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import axios from '../../services/axiosConfig';
import { QRCodeCanvas } from 'qrcode.react';
import { FiDownload, FiPrinter } from 'react-icons/fi';

const VentaDetalle = () => {
    const { id } = useParams();
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const [venta, setVenta] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // URL base del backend (ajústala según tu entorno)
    const baseURL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

    useEffect(() => {
        fetchVenta();
    }, [id]);

    const fetchVenta = async () => {
        try {
            const res = await axios.get(`/api/ventas/${id}`);
            setVenta(res.data);
        } catch (err) {
            setError('Error al cargar la venta');
        } finally {
            setLoading(false);
        }
    };

    const descargarPDF = () => {
        // Abre el PDF en una nueva pestaña (o descarga directa)
        window.open(`${baseURL}/api/ventas/${id}/pdf`, '_blank');
    };

    if (loading) return <div className="container">Cargando...</div>;
    if (error) return <div className="container" style={{ color: 'red' }}>{error}</div>;

    return (
        <div className="container">
            <div className="card">
                <h2>Detalle de Venta</h2>
                <p><strong>Factura:</strong> {venta.numeroFactura}</p>
                <p><strong>Fecha:</strong> {new Date(venta.fecha).toLocaleDateString()}</p>
                <p><strong>Vendedor:</strong> {venta.vendedorNombre}</p>
                {venta.clienteNombre && <p><strong>Cliente:</strong> {venta.clienteNombre}</p>}
                <p><strong>Total:</strong> C${venta.total.toFixed(2)}</p>

                <div style={{ marginBottom: '20px' }}>
                    {/* QR con la URL del PDF */}
                    <QRCodeCanvas
                        value={`${baseURL}/api/ventas/${id}/pdf`}
                        size={128}
                    />
                </div>

                <div className="flex" style={{ gap: '10px' }}>
                    <button className="btn btn-primary" onClick={descargarPDF}>
                        <FiDownload /> Descargar PDF
                    </button>
                    <button className="btn btn-secondary" onClick={() => window.print()}>
                        <FiPrinter /> Imprimir
                    </button>
                </div>

                <h3>Detalle de productos</h3>
                <table className="table">
                    <thead>
                    <tr>
                        <th>Producto</th>
                        <th>Lote</th>
                        <th>Cantidad</th>
                        <th>Precio Unit.</th>
                        <th>Subtotal</th>
                    </tr>
                    </thead>
                    <tbody>
                    {venta.detalles.map((det, idx) => (
                        <tr key={idx}>
                            <td>{det.medicamentoNombre} ({det.presentacion})</td>
                            <td>{det.numeroLote}</td>
                            <td>{det.cantidad}</td>
                            <td>C${det.precioUnitario.toFixed(2)}</td>
                            <td>C${det.subtotal.toFixed(2)}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default VentaDetalle;