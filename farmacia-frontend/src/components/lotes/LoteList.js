import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import axios from '../../services/axiosConfig';
import { useNavigate } from 'react-router-dom';
import { FiEdit, FiTrash2, FiPlus } from 'react-icons/fi';

const LoteList = () => {
    const { user } = useContext(AuthContext);
    const [lotes, setLotes] = useState([]);
    const [filteredLotes, setFilteredLotes] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterEstado, setFilterEstado] = useState('todos');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const basePath = user?.rol === 'ADMIN' ? '/admin' : '/vendedor';

    useEffect(() => {
        fetchLotes();
    }, []);

    const fetchLotes = async () => {
        try {
            setLoading(true);
            const response = await axios.get('/api/lotes');
            setLotes(response.data);
            setFilteredLotes(response.data);
        } catch (err) {
            setError('Error al cargar lotes');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('¿Estás seguro de desactivar este lote?')) return;
        try {
            await axios.delete(`/api/lotes/${id}`);
            fetchLotes();
        } catch (err) {
            alert('Error al desactivar lote');
        }
    };

    // Filtrado
    useEffect(() => {
        let filtered = lotes;

        if (searchTerm) {
            filtered = filtered.filter(l =>
                l.numeroLote?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                l.medicamentoNombre?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                l.fabricante?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                l.proveedor?.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        if (filterEstado !== 'todos') {
            filtered = filtered.filter(l => l.estado === filterEstado);
        }

        setFilteredLotes(filtered);
    }, [searchTerm, filterEstado, lotes]);

    if (loading) return <div className="container">Cargando...</div>;
    if (error) return <div className="container" style={{ color: 'red' }}>{error}</div>;

    return (
        <div className="container">
            <div className="card">
                <div className="flex-between">
                    <h2>Gestión de Lotes</h2>
                    {user?.rol === 'ADMIN' && (
                        <button
                            className="btn btn-primary flex"
                            onClick={() => navigate(`${basePath}/lotes/nuevo`)}
                        >
                            <FiPlus /> Nuevo Lote
                        </button>
                    )}
                </div>

                <div className="flex" style={{ marginTop: 20, flexWrap: 'wrap' }}>
                    <input
                        type="text"
                        placeholder="Buscar por lote, medicamento, fabricante..."
                        className="input-search"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        style={{ flex: 2 }}
                    />
                    <select
                        value={filterEstado}
                        onChange={(e) => setFilterEstado(e.target.value)}
                        className="input-search"
                        style={{ flex: 1, marginLeft: 0 }}
                    >
                        <option value="todos">Todos los estados</option>
                        <option value="VIGENTE">Vigente</option>
                        <option value="PRÓXIMO A VENCER">Próximo a vencer</option>
                        <option value="VENCIDO">Vencido</option>
                    </select>
                </div>

                <div style={{ overflowX: 'auto' }}>
                    <table className="table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>N° Lote</th>
                            <th>Medicamento</th>
                            <th>F. Fabricación</th>
                            <th>F. Vencimiento</th>
                            <th>Stock</th>
                            <th>Fabricante</th>
                            <th>Proveedor</th>
                            <th>Estado</th>
                            {user?.rol === 'ADMIN' && <th>Acciones</th>}
                        </tr>
                        </thead>
                        <tbody>
                        {filteredLotes.map(lote => {
                            // Determinar clase del badge según estado
                            let badgeClass = '';
                            if (lote.estado === 'VIGENTE') badgeClass = 'badge-success';
                            else if (lote.estado === 'PRÓXIMO A VENCER') badgeClass = 'badge-warning';
                            else if (lote.estado === 'VENCIDO') badgeClass = 'badge-danger';

                            return (
                                <tr key={lote.id}>
                                    <td>{lote.id}</td>
                                    <td>{lote.numeroLote}</td>
                                    <td>{lote.medicamentoNombre} ({lote.medicamentoPresentacion})</td>
                                    <td>{new Date(lote.fechaFabricacion).toLocaleDateString()}</td>
                                    <td>{new Date(lote.fechaVencimiento).toLocaleDateString()}</td>
                                    <td>{lote.cantidadActual}</td>
                                    <td>{lote.fabricante}</td>
                                    <td>{lote.proveedor}</td>
                                    <td>
                                            <span className={`badge ${badgeClass}`}>
                                                {lote.estado}
                                            </span>
                                    </td>
                                    {user?.rol === 'ADMIN' && (
                                        <td>
                                            <div className="flex">
                                                <button
                                                    className="btn btn-warning"
                                                    style={{ padding: '4px 8px', marginRight: 5 }}
                                                    onClick={() => navigate(`${basePath}/lotes/editar/${lote.id}`)}
                                                    disabled={!lote.activo}
                                                >
                                                    <FiEdit />
                                                </button>
                                                <button
                                                    className="btn btn-danger"
                                                    style={{ padding: '4px 8px' }}
                                                    onClick={() => handleDelete(lote.id)}
                                                    disabled={!lote.activo}
                                                >
                                                    <FiTrash2 />
                                                </button>
                                            </div>
                                        </td>
                                    )}
                                </tr>
                            );
                        })}
                        {filteredLotes.length === 0 && (
                            <tr>
                                <td colSpan={user?.rol === 'ADMIN' ? 10 : 9} style={{ textAlign: 'center' }}>
                                    No se encontraron lotes
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

export default LoteList;