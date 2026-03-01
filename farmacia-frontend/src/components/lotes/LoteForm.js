import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import axios from '../../services/axiosConfig';
import { useNavigate, useParams } from 'react-router-dom';

const LoteForm = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const { user } = useContext(AuthContext);
    const basePath = user?.rol === 'ADMIN' ? '/admin' : '/vendedor';

    const [formData, setFormData] = useState({
        medicamentoId: '',
        numeroLote: '',
        fechaFabricacion: '',
        fechaVencimiento: '',
        cantidadInicial: '',
        fabricante: '', // se autocompletará
        proveedorId: '' // usaremos ID de proveedor
    });

    const [medicamentos, setMedicamentos] = useState([]);
    const [proveedores, setProveedores] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    // Generar número de lote automático (solo para creación)
    const generarNumeroLote = () => {
        const ahora = new Date();
        const año = ahora.getFullYear();
        const mes = String(ahora.getMonth() + 1).padStart(2, '0');
        const dia = String(ahora.getDate()).padStart(2, '0');
        const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
        return `LOTE-${año}${mes}${dia}-${random}`;
    };

    // Cargar datos iniciales
    useEffect(() => {
        const fetchData = async () => {
            try {
                const [medsRes, provsRes] = await Promise.all([
                    axios.get('/api/medicamentos'),
                    axios.get('/api/proveedores')
                ]);
                setMedicamentos(medsRes.data);
                setProveedores(provsRes.data);
            } catch (err) {
                console.error('Error cargando datos:', err);
            }
        };
        fetchData();
        if (id) fetchLote();
        else {
            // Solo en creación, generar número de lote
            setFormData(prev => ({ ...prev, numeroLote: generarNumeroLote() }));
        }
    }, [id]);

    const fetchLote = async () => {
        try {
            setLoading(true);
            const response = await axios.get(`/api/lotes/${id}`);
            const lote = response.data;
            // Buscar el proveedor por nombre para obtener su ID (asumiendo que el backend devuelve nombre)
            const proveedor = proveedores.find(p => p.nombre === lote.proveedor);
            setFormData({
                medicamentoId: lote.medicamentoId,
                numeroLote: lote.numeroLote,
                fechaFabricacion: lote.fechaFabricacion.split('T')[0],
                fechaVencimiento: lote.fechaVencimiento.split('T')[0],
                cantidadInicial: lote.cantidadInicial,
                fabricante: lote.fabricante,
                proveedorId: proveedor ? proveedor.id : ''
            });
        } catch (err) {
            setError('Error al cargar lote');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));

        // Si se cambia el medicamento, autocompletar fabricante
        if (name === 'medicamentoId') {
            const medicamento = medicamentos.find(m => m.id === parseInt(value));
            if (medicamento) {
                setFormData(prev => ({ ...prev, fabricante: medicamento.fabricante }));
            } else {
                setFormData(prev => ({ ...prev, fabricante: '' }));
            }
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        // Buscar el nombre del proveedor seleccionado
        const proveedor = proveedores.find(p => p.id === parseInt(formData.proveedorId));
        if (!proveedor) {
            setError('Debe seleccionar un proveedor');
            return;
        }

        const payload = {
            ...formData,
            proveedor: proveedor.nombre // enviamos el nombre, no el ID, según el backend espera String
        };

        try {
            if (id) {
                await axios.put(`/api/lotes/${id}`, payload);
                setSuccess('Lote actualizado correctamente');
                setTimeout(() => navigate(`${basePath}/lotes`), 1500);
            } else {
                await axios.post('/api/lotes', payload);
                setSuccess('Lote creado correctamente');
                setTimeout(() => navigate(`${basePath}/lotes`), 1500);
            }
        } catch (err) {
            console.error(err);
            if (err.response?.data) {
                if (err.response.data.message) {
                    setError(err.response.data.message);
                } else if (typeof err.response.data === 'string') {
                    setError(err.response.data);
                } else if (err.response.data.errors) {
                    const mensajes = Object.values(err.response.data.errors).join(', ');
                    setError(mensajes);
                } else {
                    setError('Error al guardar lote');
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
                <h2>{id ? 'Editar Lote' : 'Nuevo Lote'}</h2>

                {error && <div style={{ color: 'red', marginBottom: '15px' }}>{error}</div>}
                {success && <div style={{ color: 'green', marginBottom: '15px' }}>{success}</div>}

                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '15px' }}>
                        <label>Medicamento *</label>
                        <select
                            name="medicamentoId"
                            value={formData.medicamentoId}
                            onChange={handleChange}
                            required
                            className="input-search"
                            disabled={!!id}
                        >
                            <option value="">Seleccione un medicamento</option>
                            {medicamentos.map(med => (
                                <option key={med.id} value={med.id}>
                                    {med.nombre} - {med.presentacion} ({med.fabricante})
                                </option>
                            ))}
                        </select>
                        {id && <small style={{ color: '#666' }}>El medicamento no se puede cambiar</small>}
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Número de Lote *</label>
                        <input
                            type="text"
                            name="numeroLote"
                            value={formData.numeroLote}
                            onChange={handleChange}
                            required
                            className="input-search"
                            disabled={!!id}
                            placeholder="Generado automáticamente"
                        />
                        {!id && <small style={{ color: '#666' }}>Puede modificarlo si es necesario</small>}
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px' }}>
                        <div>
                            <label>Fecha Fabricación *</label>
                            <input
                                type="date"
                                name="fechaFabricacion"
                                value={formData.fechaFabricacion}
                                onChange={handleChange}
                                required
                                className="input-search"
                            />
                        </div>
                        <div>
                            <label>Fecha Vencimiento *</label>
                            <input
                                type="date"
                                name="fechaVencimiento"
                                value={formData.fechaVencimiento}
                                onChange={handleChange}
                                required
                                className="input-search"
                            />
                        </div>
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Cantidad Inicial *</label>
                        <input
                            type="number"
                            name="cantidadInicial"
                            value={formData.cantidadInicial}
                            onChange={handleChange}
                            required
                            min="1"
                            className="input-search"
                            disabled={!!id}
                        />
                        {id && <small style={{ color: '#666' }}>La cantidad inicial no se puede modificar.</small>}
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Fabricante (del medicamento)</label>
                        <input
                            type="text"
                            name="fabricante"
                            value={formData.fabricante}
                            className="input-search"
                            readOnly
                            disabled
                            style={{ backgroundColor: '#f0f0f0' }}
                        />
                        <small style={{ color: '#666' }}>Se obtiene automáticamente del medicamento seleccionado</small>
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>Proveedor *</label>
                        <select
                            name="proveedorId"
                            value={formData.proveedorId}
                            onChange={handleChange}
                            required
                            className="input-search"
                        >
                            <option value="">Seleccione un proveedor</option>
                            {proveedores.map(prov => (
                                <option key={prov.id} value={prov.id}>
                                    {prov.nombre} - {prov.ruc}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="flex" style={{ justifyContent: 'flex-end', gap: '10px' }}>
                        <button type="button" className="btn" onClick={() => navigate(`${basePath}/lotes`)}>
                            Cancelar
                        </button>
                        <button type="submit" className="btn btn-primary">
                            {id ? 'Actualizar' : 'Crear Lote'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default LoteForm;