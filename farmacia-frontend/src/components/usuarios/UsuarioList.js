import React, { useState, useEffect } from 'react';
import axios from '../../services/axiosConfig';
import { useNavigate } from 'react-router-dom';
import { FiEdit, FiTrash2, FiPlus } from 'react-icons/fi';

const UsuarioList = () => {
    const [usuarios, setUsuarios] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        fetchUsuarios();
    }, []);

    const fetchUsuarios = async () => {
        try {
            setLoading(true);
            const response = await axios.get('/api/usuarios');
            setUsuarios(response.data);
        } catch {
            setError('Error al cargar usuarios');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('¿Estás seguro de desactivar este usuario?')) return;

        try {
            await axios.delete(`/api/usuarios/${id}`);
            fetchUsuarios();
        } catch {
            alert('Error al desactivar usuario');
        }
    };

    if (loading) return <div className="container">Cargando...</div>;
    if (error) return <div className="container" style={{ color: 'red' }}>{error}</div>;

    return (
        <div className="container">
            <div className="card">
                <div className="flex-between">
                    <h2>Gestión de Usuarios</h2>

                    <button
                        className="btn btn-primary flex"
                        onClick={() => navigate('/admin/usuarios/nuevo')}
                    >
                        <FiPlus /> Nuevo Usuario
                    </button>
                </div>

                <div style={{ overflowX: 'auto', marginTop: '20px' }}>
                    <table className="table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Username</th>
                            <th>Nombre</th>
                            <th>Apellido</th>
                            <th>Rol</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                        </thead>

                        <tbody>
                        {usuarios.map(usuario => (
                            <tr key={usuario.id}>
                                <td>{usuario.id}</td>
                                <td>{usuario.username}</td>
                                <td>{usuario.nombre}</td>
                                <td>{usuario.apellido}</td>

                                <td>
                                        <span
                                            className={`badge ${
                                                usuario.rol === 'ADMIN' ? 'badge-success' : 'badge-warning'
                                            }`}
                                        >
                                            {usuario.rol}
                                        </span>
                                </td>

                                <td>
                                        <span
                                            className={`badge ${
                                                usuario.activo ? 'badge-success' : 'badge-danger'
                                            }`}
                                        >
                                            {usuario.activo ? 'Activo' : 'Inactivo'}
                                        </span>
                                </td>

                                <td>
                                    <div className="flex">
                                        <button
                                            className="btn btn-warning"
                                            style={{ padding: '4px 8px', marginRight: 5 }}
                                            onClick={() => navigate(`/admin/usuarios/editar/${usuario.id}`)}
                                        >
                                            <FiEdit />
                                        </button>

                                        <button
                                            className="btn btn-danger"
                                            style={{ padding: '4px 8px' }}
                                            onClick={() => handleDelete(usuario.id)}
                                            disabled={!usuario.activo}
                                        >
                                            <FiTrash2 />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}

                        {usuarios.length === 0 && (
                            <tr>
                                <td colSpan="7" style={{ textAlign: 'center' }}>No hay usuarios registrados</td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default UsuarioList;