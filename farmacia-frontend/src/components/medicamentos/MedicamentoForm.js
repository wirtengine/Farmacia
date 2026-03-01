import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate, useParams } from 'react-router-dom';

const MedicamentoForm = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        nombre: '',
        principioActivo: '',
        presentacion: '',
        viaAdministracion: '',
        fabricante: '',
        registroSanitario: '',
        requiereReceta: false,
        tipoVenta: 'Libre', // valor por defecto
        precioVenta: '',
        stockMinimo: '',
        stockMaximo: ''
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (id) {
            fetchMedicamento();
        }
    }, [id]);

    const fetchMedicamento = async () => {
        try {
            setLoading(true);
            const response = await axios.get(`http://localhost:8080/api/medicamentos/${id}`);
            setFormData(response.data);
        } catch (err) {
            setError('Error al cargar medicamento');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        // Convertir precios a número
        const dataToSend = {
            ...formData,
            precioVenta: parseFloat(formData.precioVenta),
            stockMinimo: parseInt(formData.stockMinimo),
            stockMaximo: parseInt(formData.stockMaximo)
        };
        try {
            if (id) {
                await axios.put(`http://localhost:8080/api/medicamentos/${id}`, dataToSend);
            } else {
                await axios.post('http://localhost:8080/api/medicamentos', dataToSend);
            }
            navigate('/admin/medicamentos');
        } catch (err) {
            setError(err.response?.data?.message || 'Error al guardar medicamento');
        }
    };

    if (loading) return <div className="container">Cargando...</div>;

    return (
        <div className="container">
            <div className="card" style={{ maxWidth: '800px', margin: '0 auto' }}>
                <h2>{id ? 'Editar Medicamento' : 'Nuevo Medicamento'}</h2>
                {error && <div style={{ color: 'red', marginBottom: 15 }}>{error}</div>}
                <form onSubmit={handleSubmit}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px' }}>
                        <div>
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
                        <div>
                            <label>Principio Activo *</label>
                            <input
                                type="text"
                                name="principioActivo"
                                value={formData.principioActivo}
                                onChange={handleChange}
                                required
                                className="input-search"
                            />
                        </div>
                        <div>
                            <label>Presentación *</label>
                            <input
                                type="text"
                                name="presentacion"
                                value={formData.presentacion}
                                onChange={handleChange}
                                required
                                className="input-search"
                                placeholder="Ej: Tabletas 500 mg"
                            />
                        </div>
                        <div>
                            <label>Vía de Administración *</label>
                            <select
                                name="viaAdministracion"
                                value={formData.viaAdministracion}
                                onChange={handleChange}
                                required
                                className="input-search"
                            >
                                <option value="">Seleccione...</option>
                                <option value="Oral">Oral</option>
                                <option value="Tópica">Tópica</option>
                                <option value="Intramuscular">Intramuscular</option>
                                <option value="Intravenosa">Intravenosa</option>
                                <option value="Rectal">Rectal</option>
                                <option value="Inhalada">Inhalada</option>
                            </select>
                        </div>
                        <div>
                            <label>Fabricante *</label>
                            <input
                                type="text"
                                name="fabricante"
                                value={formData.fabricante}
                                onChange={handleChange}
                                required
                                className="input-search"
                            />
                        </div>
                        <div>
                            <label>Registro Sanitario *</label>
                            <input
                                type="text"
                                name="registroSanitario"
                                value={formData.registroSanitario}
                                onChange={handleChange}
                                required
                                className="input-search"
                            />
                        </div>
                        <div>
                            <label>Tipo de Venta *</label>
                            <select
                                name="tipoVenta"
                                value={formData.tipoVenta}
                                onChange={handleChange}
                                required
                                className="input-search"
                            >
                                <option value="Libre">Libre</option>
                                <option value="Controlado">Controlado</option>
                                <option value="Psicotrópico">Psicotrópico</option>
                            </select>
                        </div>
                        <div>
                            <label>Precio de Venta (C$) *</label>
                            <input
                                type="number"
                                name="precioVenta"
                                value={formData.precioVenta}
                                onChange={handleChange}
                                required
                                min="0"
                                step="0.01"
                                className="input-search"
                            />
                        </div>
                        <div>
                            <label>Stock Mínimo *</label>
                            <input
                                type="number"
                                name="stockMinimo"
                                value={formData.stockMinimo}
                                onChange={handleChange}
                                required
                                min="0"
                                className="input-search"
                            />
                        </div>
                        <div>
                            <label>Stock Máximo *</label>
                            <input
                                type="number"
                                name="stockMaximo"
                                value={formData.stockMaximo}
                                onChange={handleChange}
                                required
                                min="0"
                                className="input-search"
                            />
                        </div>
                        <div style={{ gridColumn: 'span 2' }}>
                            <label style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <input
                                    type="checkbox"
                                    name="requiereReceta"
                                    checked={formData.requiereReceta}
                                    onChange={handleChange}
                                />
                                Requiere receta médica
                            </label>
                        </div>
                    </div>
                    <div className="flex" style={{ marginTop: 20, justifyContent: 'flex-end' }}>
                        <button
                            type="button"
                            className="btn"
                            onClick={() => navigate('/admin/medicamentos')}
                            style={{ marginRight: 10 }}
                        >
                            Cancelar
                        </button>
                        <button type="submit" className="btn btn-primary">
                            {id ? 'Actualizar' : 'Guardar'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default MedicamentoForm;