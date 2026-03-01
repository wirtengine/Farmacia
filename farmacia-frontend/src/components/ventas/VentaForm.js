import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import axios from '../../services/axiosConfig';
import { useNavigate } from 'react-router-dom';
import { FiTrash2 } from 'react-icons/fi';

const VentaForm = ({ modo = 'normal' }) => {
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const basePath = user?.rol === 'ADMIN' ? '/admin' : '/vendedor';

    // Cliente
    const [clientes, setClientes] = useState([]);
    const [clienteSeleccionado, setClienteSeleccionado] = useState(null);

    // Medicamentos
    const [medicamentos, setMedicamentos] = useState([]);
    const [medicamentoSeleccionado, setMedicamentoSeleccionado] = useState(null);
    const [cantidad, setCantidad] = useState(1);
    const [precioUnitario, setPrecioUnitario] = useState(0);

    // Productos en la venta
    const [productos, setProductos] = useState([]);

    // Totales
    const [subtotal, setSubtotal] = useState(0);
    const [descuentoGlobal, setDescuentoGlobal] = useState(0);
    const [impuesto, setImpuesto] = useState(0);
    const [total, setTotal] = useState(0);

    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    // Cargar datos al inicio
    useEffect(() => {
        cargarClientes();
        cargarMedicamentos();
    }, []);

    const cargarClientes = async () => {
        try {
            const res = await axios.get('/api/clientes');
            setClientes(res.data);
        } catch (err) {
            console.error('Error cargando clientes', err);
        }
    };

    const cargarMedicamentos = async () => {
        try {
            const res = await axios.get('/api/medicamentos');
            setMedicamentos(res.data);
        } catch (err) {
            console.error('Error cargando medicamentos', err);
        }
    };

    // Al seleccionar medicamento, establecer precio
    useEffect(() => {
        if (medicamentoSeleccionado) {
            const precio = medicamentoSeleccionado.precioVenta;
            setPrecioUnitario(typeof precio === 'number' ? precio : Number(precio));
        } else {
            setPrecioUnitario(0);
        }
    }, [medicamentoSeleccionado]);

    const agregarProducto = () => {
        if (!medicamentoSeleccionado || cantidad <= 0) {
            setError('Seleccione un medicamento y cantidad válida');
            return;
        }

        const nuevoProducto = {
            id: medicamentoSeleccionado.id,
            nombre: medicamentoSeleccionado.nombre,
            presentacion: medicamentoSeleccionado.presentacion,
            cantidad,
            precioUnitario,
        };

        setProductos([...productos, nuevoProducto]);

        // Resetear selección
        setMedicamentoSeleccionado(null);
        setCantidad(1);
        setError('');
    };

    const actualizarCantidad = (index, nuevaCantidad) => {
        if (nuevaCantidad < 1) return;
        const nuevos = [...productos];
        nuevos[index].cantidad = nuevaCantidad;
        setProductos(nuevos);
    };

    const eliminarProducto = (index) => {
        const nuevos = [...productos];
        nuevos.splice(index, 1);
        setProductos(nuevos);
    };

    // Calcular totales
    useEffect(() => {
        const sub = productos.reduce((acc, p) => acc + p.cantidad * p.precioUnitario, 0);
        setSubtotal(sub);
        const base = sub - descuentoGlobal;
        const iva = base * 0.15;
        setImpuesto(iva);
        setTotal(base + iva);
    }, [productos, descuentoGlobal]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (productos.length === 0) {
            setError('Debe agregar al menos un producto');
            return;
        }

        // Construir detalles SIN loteId (el backend asignará lote automáticamente)
        const ventaData = {
            clienteId: clienteSeleccionado?.id || null,
            descuento: descuentoGlobal,
            detalles: productos.map(p => ({
                medicamentoId: p.id,
                cantidad: p.cantidad,
                precioUnitario: p.precioUnitario, // opcional, el backend puede usar el precio actual
                // No enviamos loteId
            })),
        };

        try {
            const res = await axios.post('/api/ventas', ventaData);
            setSuccess('Venta creada correctamente');
            setTimeout(() => navigate(`${basePath}/ventas/${res.data.id}`), 1500);
        } catch (err) {
            setError(err.response?.data?.message || 'Error al crear venta');
        }
    };

    return (
        <div className="container">
            <div className="card" style={{ maxWidth: '1000px', margin: '0 auto' }}>
                <h2>{modo === 'rapida' ? '🧾 Venta Rápida' : '🛒 Nueva Venta'}</h2>

                {error && <div style={{ color: 'red', marginBottom: '15px' }}>{error}</div>}
                {success && <div style={{ color: 'green', marginBottom: '15px' }}>{success}</div>}

                <form onSubmit={handleSubmit}>
                    {/* Cliente (solo si no es rápida) */}
                    {modo !== 'rapida' && (
                        <div style={{ marginBottom: '20px' }}>
                            <label>Cliente</label>
                            <select
                                className="input-search"
                                value={clienteSeleccionado?.id || ''}
                                onChange={(e) => {
                                    const id = e.target.value;
                                    if (id) {
                                        const cliente = clientes.find(c => c.id === parseInt(id));
                                        setClienteSeleccionado(cliente);
                                    } else {
                                        setClienteSeleccionado(null);
                                    }
                                }}
                            >
                                <option value="">-- Seleccione un cliente --</option>
                                {clientes.map(c => (
                                    <option key={c.id} value={c.id}>
                                        {c.nombreCompleto} - {c.identificacion}
                                    </option>
                                ))}
                            </select>
                        </div>
                    )}

                    {/* Productos */}
                    <div style={{ marginBottom: '20px' }}>
                        <h4>Agregar Productos</h4>
                        <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
                            <select
                                className="input-search"
                                style={{ flex: 2 }}
                                value={medicamentoSeleccionado?.id || ''}
                                onChange={(e) => {
                                    const id = e.target.value;
                                    if (id) {
                                        const med = medicamentos.find(m => m.id === parseInt(id));
                                        setMedicamentoSeleccionado(med);
                                    } else {
                                        setMedicamentoSeleccionado(null);
                                    }
                                }}
                            >
                                <option value="">-- Seleccione medicamento --</option>
                                {medicamentos.map(m => (
                                    <option key={m.id} value={m.id}>
                                        {m.nombre} - {m.presentacion} (C${Number(m.precioVenta).toFixed(2)})
                                    </option>
                                ))}
                            </select>

                            <input
                                type="number"
                                min="1"
                                value={cantidad}
                                onChange={(e) => setCantidad(parseInt(e.target.value) || 1)}
                                className="input-search"
                                style={{ width: '100px' }}
                            />

                            <button type="button" className="btn btn-primary" onClick={agregarProducto}>
                                Agregar
                            </button>
                        </div>

                        {productos.length > 0 && (
                            <div style={{ marginTop: '20px', overflowX: 'auto' }}>
                                <table className="table">
                                    <thead>
                                    <tr>
                                        <th>Producto</th>
                                        <th>Cantidad</th>
                                        <th>Precio Unit.</th>
                                        <th>Subtotal</th>
                                        <th></th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {productos.map((p, index) => (
                                        <tr key={index}>
                                            <td>{p.nombre} ({p.presentacion})</td>
                                            <td>
                                                <input
                                                    type="number"
                                                    min="1"
                                                    value={p.cantidad}
                                                    onChange={(e) => actualizarCantidad(index, parseInt(e.target.value) || 1)}
                                                    style={{ width: '70px', padding: '4px' }}
                                                />
                                            </td>
                                            <td>C$ {p.precioUnitario.toFixed(2)}</td>
                                            <td>C$ {(p.cantidad * p.precioUnitario).toFixed(2)}</td>
                                            <td>
                                                <button
                                                    type="button"
                                                    className="btn btn-danger btn-sm"
                                                    onClick={() => eliminarProducto(index)}
                                                >
                                                    <FiTrash2 />
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>

                    {/* Totales */}
                    <div style={{ textAlign: 'right' }}>
                        <div><strong>Subtotal:</strong> C$ {subtotal.toFixed(2)}</div>
                        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '10px' }}>
                            <strong>Descuento global:</strong>
                            <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={descuentoGlobal}
                                onChange={(e) => setDescuentoGlobal(parseFloat(e.target.value) || 0)}
                                style={{ width: '100px', padding: '5px' }}
                            />
                        </div>
                        <div><strong>IVA (15%):</strong> C$ {impuesto.toFixed(2)}</div>
                        <div style={{ fontSize: '1.2em' }}><strong>TOTAL:</strong> C$ {total.toFixed(2)}</div>
                    </div>

                    {/* Botones */}
                    <div className="flex" style={{ justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                        <button type="button" className="btn" onClick={() => navigate(`${basePath}/ventas`)}>
                            Cancelar
                        </button>
                        <button type="submit" className="btn btn-primary">
                            Confirmar Venta
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default VentaForm;