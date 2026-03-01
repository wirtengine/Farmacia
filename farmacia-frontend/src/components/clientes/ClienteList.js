import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import axios from '../../services/axiosConfig';
import { useNavigate } from 'react-router-dom';
import { FiEdit, FiTrash2, FiPlus } from 'react-icons/fi';

const ClienteList = () => {
    const { user } = useContext(AuthContext);
    const [clientes, setClientes] = useState([]);
    const [filteredClientes, setFilteredClientes] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterActivo, setFilterActivo] = useState('todos');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const basePath = user?.rol === 'ADMIN' ? '/admin' : '/vendedor';

    useEffect(() => {
        fetchClientes();
    }, []);

    const fetchClientes = async () => {
        try {
            setLoading(true);
            const response = await axios.get('/api/clientes');
            setClientes(response.data);
            setFilteredClientes(response.data);
        } catch (err) {
            setError('Error al cargar clientes');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        let filtered = clientes;

        if (searchTerm) {
            filtered = filtered.filter(cli =>
                cli.nombreCompleto?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                cli.identificacion?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                cli.telefono?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                cli.email?.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        if (filterActivo === 'activos') {
            filtered = filtered.filter(cli => cli.activo);
        } else if (filterActivo === 'inactivos') {
            filtered = filtered.filter(cli => !cli.activo);
        }

        setFilteredClientes(filtered);
    }, [searchTerm, filterActivo, clientes]);

    const handleDelete = async (id) => {
        if (!window.confirm('¿Estás seguro de desactivar este cliente?')) return;
        try {
            await axios.delete(`/api/clientes/${id}`);
            fetchClientes();
        } catch (err) {
            alert('Error al desactivar cliente');
        }
    };

    if (loading) return <div className="container">Cargando...</div>;
    if (error) return <div className="container" style={{ color: 'red' }}>{error}</div>;

    return (
        <div className="container">
            <div className="card">
                <div className="flex-between">
                    <h2>Gestión de Clientes</h2>
                    <button
                        className="btn btn-primary flex"
                        onClick={() => navigate(`${basePath}/clientes/nuevo`)}
                    >
                        <FiPlus /> Nuevo Cliente
                    </button>
                </div>

                <div className="flex" style={{ marginTop: 20, flexWrap: 'wrap' }}>
                    <input
                        type="text"
                        placeholder="Buscar por nombre, identificación, teléfono o email..."
                        className="input-search"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        style={{ flex: 2 }}
                    />
                    <select
                        value={filterActivo}
                        onChange={(e) => setFilterActivo(e.target.value)}
                        className="input-search"
                        style={{ flex: 1, marginLeft: 0 }}
                    >
                        <option value="todos">Todos</option>
                        <option value="activos">Activos</option>
                        <option value="inactivos">Inactivos</option>
                    </select>
                </div>

                <div style={{ overflowX: 'auto' }}>
                    <table className="table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Identificación</th>
                            <th>Nombre Completo</th>
                            <th>Teléfono</th>
                            <th>Email</th>
                            <th>Dirección</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredClientes.map(cli => (
                            <tr key={cli.id}>
                                <td>{cli.id}</td>
                                <td>{cli.identificacion}</td>
                                <td>{cli.nombreCompleto}</td>
                                <td>{cli.telefono}</td>
                                <td>{cli.email}</td>
                                <td>{cli.direccion}</td>
                                <td>
                                        <span className={`badge ${cli.activo ? 'badge-success' : 'badge-danger'}`}>
                                            {cli.activo ? 'Activo' : 'Inactivo'}
                                        </span>
                                </td>
                                <td>
                                    <div className="flex">
                                        <button
                                            className="btn btn-warning"
                                            style={{ padding: '4px 8px', marginRight: 5 }}
                                            onClick={() => navigate(`${basePath}/clientes/editar/${cli.id}`)}
                                            disabled={!cli.activo}
                                        >
                                            <FiEdit />
                                        </button>
                                        <button
                                            className="btn btn-danger"
                                            style={{ padding: '4px 8px' }}
                                            onClick={() => handleDelete(cli.id)}
                                            disabled={!cli.activo}
                                        >
                                            <FiTrash2 />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        {filteredClientes.length === 0 && (
                            <tr>
                                <td colSpan="8" style={{ textAlign: 'center' }}>
                                    No se encontraron clientes
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

export default ClienteList;