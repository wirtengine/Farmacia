import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

const UserForm = () => {
    const [formData, setFormData] = useState({
        username: '',
        password: '',
        nombre: '',
        apellido: '',
        rol: 'VENDEDOR'
    });
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage('');
        setError('');

        try {
            await axios.post('http://localhost:8080/api/auth/register', formData);
            setMessage('Usuario creado exitosamente');
            setFormData({
                username: '',
                password: '',
                nombre: '',
                apellido: '',
                rol: 'VENDEDOR'
            });
            // Opcional: redirigir después de 2 segundos
            setTimeout(() => navigate('/admin/usuarios'), 2000);
        } catch (err) {
            setError(err.response?.data || 'Error al crear usuario');
        }
    };

    return (
        <div className="container">
            <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
                <h2>Registrar Nuevo Usuario</h2>
                {message && <div style={{ color: 'green', marginBottom: '10px', padding: '10px', backgroundColor: '#d4edda', borderRadius: '4px' }}>{message}</div>}
                {error && <div style={{ color: 'red', marginBottom: '10px', padding: '10px', backgroundColor: '#f8d7da', borderRadius: '4px' }}>{error}</div>}
                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '15px' }}>
                        <label>Username:</label>
                        <input
                            type="text"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            className="input-search"
                            required
                        />
                    </div>
                    <div style={{ marginBottom: '15px' }}>
                        <label>Contraseña:</label>
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            className="input-search"
                            required
                        />
                    </div>
                    <div style={{ marginBottom: '15px' }}>
                        <label>Nombre:</label>
                        <input
                            type="text"
                            name="nombre"
                            value={formData.nombre}
                            onChange={handleChange}
                            className="input-search"
                            required
                        />
                    </div>
                    <div style={{ marginBottom: '15px' }}>
                        <label>Apellido:</label>
                        <input
                            type="text"
                            name="apellido"
                            value={formData.apellido}
                            onChange={handleChange}
                            className="input-search"
                            required
                        />
                    </div>
                    <div style={{ marginBottom: '15px' }}>
                        <label>Rol:</label>
                        <select
                            name="rol"
                            value={formData.rol}
                            onChange={handleChange}
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
                            Crear Usuario
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default UserForm;