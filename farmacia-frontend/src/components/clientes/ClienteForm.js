import React, { useState, useEffect, useContext } from 'react';
import axios from '../../services/axiosConfig';
import { useNavigate, useParams } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';

const ClienteForm = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const { user } = useContext(AuthContext);

    const basePath = user?.rol === 'ADMIN' ? '/admin' : '/vendedor';

    const [formData, setFormData] = useState({
        identificacion: '',
        nombreCompleto: '',
        telefono: '',
        email: '',
        direccion: ''
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        if (id) fetchCliente();
    }, [id]);

    const fetchCliente = async () => {
        try {
            setLoading(true);
            const response = await axios.get(`/api/clientes/${id}`);
            setFormData(response.data);
        } catch (err) {
            setError('Error al cargar cliente');
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
                await axios.put(`/api/clientes/${id}`, formData);
                setSuccess('Cliente actualizado correctamente');
            } else {
                await axios.post('/api/clientes', formData);
                setSuccess('Cliente creado correctamente');
            }
            setTimeout(() => navigate(`${basePath}/clientes`), 2000);
        } catch (err) {
            console.error(err);
            if (err.response?.data) {
                // Si es un objeto con campo 'message' (puede ser de excepción general)
                if (err.response.data.message) {
                    setError(err.response.data.message);
                }
                // Si es un string plano
                else if (typeof err.response.data === 'string') {
                    setError(err.response.data);
                }
                // Si tiene errores de campo (estructura de Spring Boot)
                else if (err.response.data.errors) {
                    // errors puede ser un array o un objeto
                    const mensajes = Object.values(err.response.data.errors).flat().join(', ');
                    setError(mensajes);
                }
                // Si es un objeto con campo 'error' (a veces)
                else if (err.response.data.error) {
                    setError(err.response.data.error);
                } else {
                    setError('Error al guardar cliente');
                }
            } else {
                setError('Error de conexión con el servidor');
            }
        }
    };

    if (loading) return <div className="container">Cargando...</div>;

    return (
        <div className="container">
            <div className="card" style={{ maxWidth: '700px', margin: '0 auto' }}>
                <h2>{id ? 'Editar Cliente' : 'Nuevo Cliente'}</h2>
                {error && <div style={{ color: 'red', marginBottom: '15px' }}>{error}</div>}
                {success && <div style={{ color: 'green', marginBottom: '15px' }}>{success}</div>}
                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '15px' }}>
                        <label>Identificación *</label>
                        <input
                            type="text"
                            name="identificacion"
                            value={formData.identificacion}
                            onChange={handleChange}
                            required
                            className="input-search"
                            disabled={!!id}
                        />
                        {id && <small style={{ color: '#666' }}>La identificación no se puede modificar</small>}
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Nombre completo *</label>
                        <input
                            type="text"
                            name="nombreCompleto"
                            value={formData.nombreCompleto}
                            onChange={handleChange}
                            required
                            className="input-search"
                        />
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Teléfono</label>
                        <input
                            type="text"
                            name="telefono"
                            value={formData.telefono}
                            onChange={handleChange}
                            className="input-search"
                        />
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Email</label>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            className="input-search"
                        />
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Dirección</label>
                        <input
                            type="text"
                            name="direccion"
                            value={formData.direccion}
                            onChange={handleChange}
                            className="input-search"
                        />
                    </div>

                    <div className="flex" style={{ justifyContent: 'flex-end', gap: '10px' }}>
                        <button type="button" className="btn" onClick={() => navigate(`${basePath}/clientes`)}>
                            Cancelar
                        </button>
                        <button type="submit" className="btn btn-primary">
                            {id ? 'Actualizar' : 'Crear Cliente'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default ClienteForm;