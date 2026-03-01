import React, { useState, useEffect } from 'react';
import axios from '../../services/axiosConfig';
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
        tipoVenta: 'Libre',
        precioVenta: '',
        stockMinimo: '',
        stockMaximo: ''
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (id) fetchMedicamento();
    }, [id]);

    const fetchMedicamento = async () => {
        try {
            setLoading(true);
            const response = await axios.get(`/api/medicamentos/${id}`);
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

        const dataToSend = {
            ...formData,
            precioVenta: parseFloat(formData.precioVenta),
            stockMinimo: parseInt(formData.stockMinimo),
            stockMaximo: parseInt(formData.stockMaximo)
        };

        try {
            if (id) {
                await axios.put(`/api/medicamentos/${id}`, dataToSend);
            } else {
                await axios.post('/api/medicamentos', dataToSend);
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
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr',
                        gap: '15px'
                    }}>

                        <div>
                            <label>Nombre *</label>
                            <input
                                type="text"
                                name="nombre"
                                className="input-search"
                                required
                                value={formData.nombre}
                                onChange={handleChange}
                            />
                        </div>

                        <div>
                            <label>Principio Activo *</label>
                            <input
                                type="text"
                                name="principioActivo"
                                className="input-search"
                                required
                                value={formData.principioActivo}
                                onChange={handleChange}
                            />
                        </div>

                        <div>
                            <label>Presentación *</label>
                            <input
                                type="text"
                                name="presentacion"
                                className="input-search"
                                required
                                placeholder="Ej: Tabletas 500 mg"
                                value={formData.presentacion}
                                onChange={handleChange}
                            />
                        </div>

                        <div>
                            <label>Vía de Administración *</label>
                            <select
                                name="viaAdministracion"
                                className="input-search"
                                required
                                value={formData.viaAdministracion}
                                onChange={handleChange}
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
                                className="input-search"
                                required
                                value={formData.fabricante}
                                onChange={handleChange}
                            />
                        </div>

                        <div>
                            <label>Registro Sanitario *</label>
                            <input
                                type="text"
                                name="registroSanitario"
                                className="input-search"
                                required
                                value={formData.registroSanitario}
                                onChange={handleChange}
                            />
                        </div>

                        <div>
                            <label>Tipo de Venta *</label>
                            <select
                                name="tipoVenta"
                                className="input-search"
                                required
                                value={formData.tipoVenta}
                                onChange={handleChange}
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
                                min="0"
                                step="0.01"
                                className="input-search"
                                required
                                value={formData.precioVenta}
                                onChange={handleChange}
                            />
                        </div>

                        <div>
                            <label>Stock Mínimo *</label>
                            <input
                                type="number"
                                name="stockMinimo"
                                min="0"
                                className="input-search"
                                required
                                value={formData.stockMinimo}
                                onChange={handleChange}
                            />
                        </div>

                        <div>
                            <label>Stock Máximo *</label>
                            <input
                                type="number"
                                name="stockMaximo"
                                min="0"
                                className="input-search"
                                required
                                value={formData.stockMaximo}
                                onChange={handleChange}
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
                            style={{ marginRight: 10 }}
                            onClick={() => navigate('/admin/medicamentos')}
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