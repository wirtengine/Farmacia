import React, { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import axios from '../../services/axiosConfig';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { QRCodeCanvas } from 'qrcode.react'; // Cambio aquí
import { FiDownload, FiPrinter } from 'react-icons/fi';

const VentaDetalle = () => {
    const { id } = useParams();
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const [venta, setVenta] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

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

    const generarPDF = () => {
        if (!venta) return;

        const doc = new jsPDF();

        // Título
        doc.setFontSize(18);
        doc.text('Farmacia - Comprobante de Venta', 14, 22);

        // Información de la venta
        doc.setFontSize(12);
        doc.text(`Factura: ${venta.numeroFactura}`, 14, 32);
        doc.text(`Fecha: ${new Date(venta.fecha).toLocaleDateString()}`, 14, 38);
        doc.text(`Vendedor: ${venta.vendedorNombre}`, 14, 44);
        if (venta.clienteNombre) {
            doc.text(`Cliente: ${venta.clienteNombre}`, 14, 50);
        } else {
            doc.text(`Cliente: Consumidor Final`, 14, 50);
        }

        // Tabla de productos
        const tableColumn = ['Producto', 'Cantidad', 'Precio Unit.', 'Subtotal'];
        const tableRows = venta.detalles.map(det => [
            `${det.medicamentoNombre} (${det.presentacion})`,
            det.cantidad,
            `C$${det.precioUnitario.toFixed(2)}`,
            `C$${det.subtotal.toFixed(2)}`,
        ]);

        autoTable(doc, {
            startY: 60,
            head: [tableColumn],
            body: tableRows,
        });

        const finalY = doc.lastAutoTable.finalY + 10;
        doc.text(`Subtotal: C$${venta.subtotal.toFixed(2)}`, 14, finalY);
        doc.text(`Descuento: C$${venta.descuento.toFixed(2)}`, 14, finalY + 6);
        doc.text(`IVA (15%): C$${venta.impuesto.toFixed(2)}`, 14, finalY + 12);
        doc.text(`Total: C$${venta.total.toFixed(2)}`, 14, finalY + 18);

        doc.save(`factura_${venta.numeroFactura}.pdf`);
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
                    <QRCodeCanvas
                        value={`Factura: ${venta.numeroFactura}\nTotal: C$${venta.total.toFixed(2)}`}
                        size={128}
                    />
                </div>

                <div className="flex" style={{ gap: '10px' }}>
                    <button className="btn btn-primary" onClick={generarPDF}>
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