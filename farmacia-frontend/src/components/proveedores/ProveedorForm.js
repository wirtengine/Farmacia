import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from '../../services/axiosConfig';

const ProveedorForm = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        ruc: '',
        nombre: '',
        telefono: '',
        email: '',
        direccion: '',
        contacto: '',
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        if (id) fetchProveedor();
    }, [id]);

    const fetchProveedor = async () => {
        try {
            setLoading(true);
            const response = await axios.get(`/api/proveedores/${id}`);
            setFormData(response.data);
        } catch {
            setError('Error al cargar proveedor');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        try {
            if (id) {
                await axios.put(`/api/proveedores/${id}`, formData);
                setSuccess('Proveedor actualizado correctamente');
            } else {
                await axios.post('/api/proveedores', formData);
                setSuccess('Proveedor creado correctamente');

                setFormData({
                    ruc: '',
                    nombre: '',
                    telefono: '',
                    email: '',
                    direccion: '',
                    contacto: '',
                });
            }
        } catch (err) {
            setError(err.response?.data || 'Error al guardar proveedor');
        }
    };

    if (loading) return <div className="container">Cargando...</div>;

    return (
        <div className="container">
            <div className="card" style={{ maxWidth: '700px', margin: '0 auto' }}>
                <h2>{id ? 'Editar Proveedor' : 'Nuevo Proveedor'}</h2>

                {error && <div style={{ color: 'red', marginBottom: 15 }}>{error}</div>}
                {success && <div style={{ color: 'green', marginBottom: 15 }}>{success}</div>}

                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: 15 }}>
                        <label>RUC *</label>
                        <input
                            type="text"
                            name="ruc"
                            value={formData.ruc}
                            onChange={handleChange}
                            required
                            className="input-search"
                            disabled={!!id}
                        />
                    </div>

                    <div style={{ marginBottom: 15 }}>
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

                    <div style={{ marginBottom: 15 }}>
                        <label>Teléfono</label>
                        <input
                            type="text"
                            name="telefono"
                            value={formData.telefono}
                            onChange={handleChange}
                            className="input-search"
                        />
                    </div>

                    <div style={{ marginBottom: 15 }}>
                        <label>Email</label>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            className="input-search"
                        />
                    </div>

                    <div style={{ marginBottom: 15 }}>
                        <label>Dirección</label>
                        <input
                            type="text"
                            name="direccion"
                            value={formData.direccion}
                            onChange={handleChange}
                            className="input-search"
                        />
                    </div>

                    <div style={{ marginBottom: 15 }}>
                        <label>Contacto</label>
                        <input
                            type="text"
                            name="contacto"
                            value={formData.contacto}
                            onChange={handleChange}
                            className="input-search"
                        />
                    </div>

                    <div className="flex" style={{ justifyContent: 'flex-end', gap: 10 }}>
                        <button
                            type="button"
                            className="btn"
                            onClick={() => navigate('/admin/proveedores')}
                        >
                            Cancelar
                        </button>

                        <button type="submit" className="btn btn-primary">
                            {id ? 'Actualizar' : 'Crear Proveedor'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default ProveedorForm;