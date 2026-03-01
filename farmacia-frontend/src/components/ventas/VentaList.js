import React, { useState, useEffect, useContext, useCallback } from 'react';
import { AuthContext } from '../../context/AuthContext';
import axios from '../../services/axiosConfig';
import { useNavigate, useLocation } from 'react-router-dom';
import { FiEye, FiPlus, FiZap } from 'react-icons/fi';

const VentaList = () => {
    const { user } = useContext(AuthContext);
    const [ventas, setVentas] = useState([]);
    const [filteredVentas, setFilteredVentas] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [vendedorFilter, setVendedorFilter] = useState('');
    const [vendedores, setVendedores] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const location = useLocation();

    const basePath = user?.rol === 'ADMIN' ? '/admin' : '/vendedor';

    const fetchVentas = useCallback(async () => {
        try {
            setLoading(true);
            const url = user?.rol === 'ADMIN' ? '/api/ventas' : '/api/ventas/mis-ventas';
            const res = await axios.get(url);
            setVentas(res.data);
            setFilteredVentas(res.data);
        } catch (err) {
            console.error('Error cargando ventas', err);
        } finally {
            setLoading(false);
        }
    }, [user?.rol]);

    const fetchVendedores = useCallback(async () => {
        try {
            const res = await axios.get('/api/usuarios');
            setVendedores(res.data);
        } catch (err) {
            console.error('Error cargando usuarios', err);
        }
    }, []);

    useEffect(() => {
        fetchVentas();
        if (user?.rol === 'ADMIN') {
            fetchVendedores();
        }
    }, [fetchVentas, fetchVendedores, user?.rol, location.key, location.state?.recargar]);

    useEffect(() => {
        let filtered = ventas;
        if (searchTerm) {
            filtered = filtered.filter(v =>
                v.numeroFactura.toLowerCase().includes(searchTerm.toLowerCase()) ||
                (v.clienteNombre && v.clienteNombre.toLowerCase().includes(searchTerm.toLowerCase()))
            );
        }
        if (vendedorFilter && user?.rol === 'ADMIN') {
            filtered = filtered.filter(v => v.vendedorId === parseInt(vendedorFilter));
        }
        setFilteredVentas(filtered);
    }, [searchTerm, vendedorFilter, ventas, user]);

    const mostrarBotonesCrear = user?.rol === 'ADMIN';

    return (
        <div className="container">
            <div className="card">
                <div className="flex-between">
                    <h2>{user?.rol === 'ADMIN' ? 'Historial de Ventas' : 'Mis Ventas'}</h2>
                    {mostrarBotonesCrear && (
                        <div className="flex" style={{ gap: '10px' }}>
                            <button
                                className="btn btn-primary flex"
                                onClick={() => navigate(`${basePath}/ventas/nueva`)}
                            >
                                <FiPlus /> Nueva Venta
                            </button>
                            <button
                                className="btn btn-success flex"
                                onClick={() => navigate(`${basePath}/ventas/rapida`)}
                            >
                                <FiZap /> Venta Rápida
                            </button>
                        </div>
                    )}
                </div>
                <div className="flex" style={{ marginTop: 20, flexWrap: 'wrap' }}>
                    <input
                        type="text"
                        placeholder="Buscar por factura o cliente..."
                        className="input-search"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        style={{ flex: 2 }}
                    />
                    {user?.rol === 'ADMIN' && (
                        <select
                            value={vendedorFilter}
                            onChange={(e) => setVendedorFilter(e.target.value)}
                            className="input-search"
                            style={{ flex: 1, marginLeft: 0 }}
                        >
                            <option value="">Todos los vendedores</option>
                            {vendedores.map(v => (
                                <option key={v.id} value={v.id}>
                                    {v.nombre} {v.apellido} ({v.rol})
                                </option>
                            ))}
                        </select>
                    )}
                </div>
                <div style={{ overflowX: 'auto' }}>
                    <table className="table">
                        <thead>
                        <tr>
                            <th>Factura</th>
                            <th>Fecha</th>
                            <th>Cliente</th>
                            <th>Vendedor</th>
                            <th>Total</th>
                            <th>Estado</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredVentas.map(v => (
                            <tr key={v.id}>
                                <td>{v.numeroFactura}</td>
                                <td>{new Date(v.fecha).toLocaleDateString()}</td>
                                <td>{v.clienteNombre || 'Consumidor Final'}</td>
                                <td>{v.vendedorNombre}</td>
                                <td>C$ {v.total?.toFixed(2)}</td>
                                <td>
                                    <span className={`badge ${
                                        v.estado === 'CONFIRMADA' ? 'badge-success' :
                                            v.estado === 'ANULADA' ? 'badge-danger' :
                                                v.estado === 'DEVUELTA_PARCIAL' ? 'badge-warning' : 'badge-secondary'
                                    }`}>
                                        {v.estado}
                                    </span>
                                </td>
                                <td>
                                    <button
                                        className="btn btn-info"
                                        onClick={() => navigate(`${basePath}/ventas/${v.id}`)}
                                    >
                                        <FiEye /> Ver
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {filteredVentas.length === 0 && (
                            <tr>
                                <td colSpan="7" style={{ textAlign: 'center' }}>
                                    {loading ? 'Cargando...' : 'No hay ventas'}
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

export default VentaList;