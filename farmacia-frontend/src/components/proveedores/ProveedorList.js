import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { FiEdit, FiTrash2, FiPlus } from 'react-icons/fi';
import axios from '../../services/axiosConfig';

const ProveedorList = () => {
    const { user } = useContext(AuthContext);
    const [proveedores, setProveedores] = useState([]);
    const [filteredProveedores, setFilteredProveedores] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterActivo, setFilterActivo] = useState('todos');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const navigate = useNavigate();

    useEffect(() => {
        fetchProveedores();
    }, []);

    const fetchProveedores = async () => {
        try {
            setLoading(true);
            const response = await axios.get('/api/proveedores');
            setProveedores(response.data);
            setFilteredProveedores(response.data);
        } catch (err) {
            console.error(err);
            setError('Error al cargar proveedores');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        let filtered = proveedores;

        if (searchTerm) {
            filtered = filtered.filter((prov) =>
                prov.nombre?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                prov.ruc?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                prov.telefono?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                prov.email?.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        if (filterActivo === 'activos') {
            filtered = filtered.filter((prov) => prov.activo);
        } else if (filterActivo === 'inactivos') {
            filtered = filtered.filter((prov) => !prov.activo);
        }

        setFilteredProveedores(filtered);
    }, [searchTerm, filterActivo, proveedores]);

    const handleDelete = async (id) => {
        if (!window.confirm('¿Estás seguro de desactivar este proveedor?')) return;

        try {
            await axios.delete(`/api/proveedores/${id}`);
            fetchProveedores();
        } catch (err) {
            alert('Error al desactivar proveedor');
        }
    };

    if (loading) return <div className="container">Cargando...</div>;
    if (error) return <div className="container" style={{ color: 'red' }}>{error}</div>;

    return (
        <div className="container">
            <div className="card">

                {/* Título + botón */}
                <div className="flex-between">
                    <h2>Gestión de Proveedores</h2>
                    {user?.rol === 'ADMIN' && (
                        <button
                            className="btn btn-primary flex"
                            onClick={() => navigate('/admin/proveedores/nuevo')}
                        >
                            <FiPlus /> Nuevo Proveedor
                        </button>
                    )}
                </div>

                {/* Buscador + Filtro */}
                <div className="flex" style={{ marginTop: 20, flexWrap: 'wrap' }}>
                    <input
                        type="text"
                        placeholder="Buscar por nombre, RUC, teléfono o email..."
                        className="input-search"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        style={{ flex: 2 }}
                    />

                    <select
                        value={filterActivo}
                        onChange={(e) => setFilterActivo(e.target.value)}
                        className="input-search"
                        style={{ flex: 1 }}
                    >
                        <option value="todos">Todos</option>
                        <option value="activos">Activos</option>
                        <option value="inactivos">Inactivos</option>
                    </select>
                </div>

                {/* Tabla */}
                <div style={{ overflowX: 'auto' }}>
                    <table className="table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>RUC</th>
                            <th>Nombre</th>
                            <th>Teléfono</th>
                            <th>Email</th>
                            <th>Dirección</th>
                            <th>Contacto</th>
                            <th>Estado</th>
                            {user?.rol === 'ADMIN' && <th>Acciones</th>}
                        </tr>
                        </thead>

                        <tbody>
                        {filteredProveedores.map((prov) => (
                            <tr key={prov.id}>
                                <td>{prov.id}</td>
                                <td>{prov.ruc}</td>
                                <td>{prov.nombre}</td>
                                <td>{prov.telefono}</td>
                                <td>{prov.email}</td>
                                <td>{prov.direccion}</td>
                                <td>{prov.contacto}</td>

                                <td>
                                    <span
                                        className={`badge ${
                                            prov.activo ? 'badge-success' : 'badge-danger'
                                        }`}
                                    >
                                        {prov.activo ? 'Activo' : 'Inactivo'}
                                    </span>
                                </td>

                                {user?.rol === 'ADMIN' && (
                                    <td>
                                        <div className="flex">
                                            <button
                                                className="btn btn-warning"
                                                style={{ padding: '4px 8px', marginRight: 5 }}
                                                onClick={() => navigate(`/admin/proveedores/editar/${prov.id}`)}
                                                disabled={!prov.activo}
                                            >
                                                <FiEdit />
                                            </button>

                                            <button
                                                className="btn btn-danger"
                                                style={{ padding: '4px 8px' }}
                                                onClick={() => handleDelete(prov.id)}
                                                disabled={!prov.activo}
                                            >
                                                <FiTrash2 />
                                            </button>
                                        </div>
                                    </td>
                                )}
                            </tr>
                        ))}

                        {filteredProveedores.length === 0 && (
                            <tr>
                                <td
                                    colSpan={user?.rol === 'ADMIN' ? 9 : 8}
                                    style={{ textAlign: 'center' }}
                                >
                                    No se encontraron proveedores
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

export default ProveedorList;