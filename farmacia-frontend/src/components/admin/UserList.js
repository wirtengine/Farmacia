import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { FiUserPlus } from 'react-icons/fi';

const UserList = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        fetchUsers();
    }, []);

    const fetchUsers = async () => {
        try {
            setLoading(true);
            // Necesitamos un endpoint para obtener usuarios.
            // Si no existe, lo crearemos en el backend.
            const response = await axios.get('http://localhost:8080/api/usuarios');
            setUsers(response.data);
        } catch (err) {
            setError('Error al cargar usuarios');
            console.error(err);
        } finally {
            setLoading(false);
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
                        <FiUserPlus /> Nuevo Usuario
                    </button>
                </div>

                <div style={{ overflowX: 'auto', marginTop: 20 }}>
                    <table className="table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Username</th>
                            <th>Nombre</th>
                            <th>Apellido</th>
                            <th>Rol</th>
                        </tr>
                        </thead>
                        <tbody>
                        {users.map(user => (
                            <tr key={user.id}>
                                <td>{user.id}</td>
                                <td>{user.username}</td>
                                <td>{user.nombre}</td>
                                <td>{user.apellido}</td>
                                <td>
                    <span className={`badge ${user.rol === 'ADMIN' ? 'badge-success' : 'badge-warning'}`}>
                      {user.rol}
                    </span>
                                </td>
                            </tr>
                        ))}
                        {users.length === 0 && (
                            <tr>
                                <td colSpan="5" style={{ textAlign: 'center' }}>
                                    No hay usuarios registrados
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

export default UserList;