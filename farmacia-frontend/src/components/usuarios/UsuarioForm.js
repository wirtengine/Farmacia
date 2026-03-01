import React, { useState, useEffect } from 'react';
import axios from '../../services/axiosConfig';
import { useNavigate, useParams } from 'react-router-dom';

const UsuarioForm = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        username: '',
        password: '',
        nombre: '',
        apellido: '',
        rol: 'VENDEDOR'
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        if (id) fetchUsuario();
    }, [id]);

    const fetchUsuario = async () => {
        try {
            setLoading(true);
            const response = await axios.get(`/api/usuarios/${id}`);
            const usuario = response.data;

            setFormData({
                username: usuario.username,
                password: '',
                nombre: usuario.nombre,
                apellido: usuario.apellido,
                rol: usuario.rol
            });
        } catch {
            setError('Error al cargar usuario');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        try {
            if (id) {
                await axios.put(`/api/usuarios/${id}`, formData);
                setSuccess('Usuario actualizado correctamente');
            } else {
                await axios.post('/api/usuarios', formData);
                setSuccess('Usuario creado correctamente');

                setFormData({
                    username: '',
                    password: '',
                    nombre: '',
                    apellido: '',
                    rol: 'VENDEDOR'
                });
            }
        } catch (err) {
            setError(err.response?.data || 'Error al guardar usuario');
        }
    };

    if (loading) return <div className="container">Cargando...</div>;

    return (
        <div className="container">
            <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
                <h2>{id ? 'Editar Usuario' : 'Nuevo Usuario'}</h2>

                {error && <div style={{ color: 'red', marginBottom: '15px' }}>{error}</div>}
                {success && <div style={{ color: 'green', marginBottom: '15px' }}>{success}</div>}

                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '15px' }}>
                        <label>Username *</label>
                        <input
                            type="text"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            required
                            disabled={!!id}
                            className="input-search"
                        />
                        {id && <small style={{ color: '#666' }}>El username no se puede cambiar</small>}
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>{id ? 'Nueva contraseña (opcional)' : 'Contraseña *'}</label>
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            required={!id}
                            className="input-search"
                        />
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Nombre *</label>
                        <input
                            type="text"
                            name="nombre"
                            value={formData.nombre}
                            onChange={handleChange}
                            required
                            className="input-search"
                        />
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Apellido *</label>
                        <input
                            type="text"
                            name="apellido"
                            value={formData.apellido}
                            onChange={handleChange}
                            required
                            className="input-search"
                        />
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Rol *</label>
                        <select
                            name="rol"
                            value={formData.rol}
                            onChange={handleChange}
                            required
                            className="input-search"
                        >
                            <option value="VENDEDOR">Vendedor</option>
                            <option value="ADMIN">Administrador</option>
                        </select>
                    </div>

                    <div className="flex" style={{ justifyContent: 'flex-end', gap: '10px' }}>
                        <button type="button" className="btn" onClick={() => navigate('/admin/usuarios')}>
                            Cancelar
                        </button>

                        <button type="submit" className="btn btn-primary">
                            {id ? 'Actualizar' : 'Crear Usuario'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default UsuarioForm;