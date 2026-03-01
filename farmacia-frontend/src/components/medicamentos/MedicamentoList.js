import React, { useState, useEffect, useContext } from 'react';
import axios from '../../services/axiosConfig';
import { FiEdit, FiTrash2, FiPlus } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';

const MedicamentoList = () => {
    const { user } = useContext(AuthContext);
    const [medicamentos, setMedicamentos] = useState([]);
    const [filteredMedicamentos, setFilteredMedicamentos] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const navigate = useNavigate();

    useEffect(() => {
        fetchMedicamentos();
    }, []);

    const fetchMedicamentos = async () => {
        try {
            setLoading(true);
            const response = await axios.get('/api/medicamentos');
            setMedicamentos(response.data);
            setFilteredMedicamentos(response.data);
        } catch (err) {
            console.error(err);
            setError('Error al cargar medicamentos');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        const filtered = medicamentos.filter(med =>
            med.nombre?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            med.principioActivo?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            med.registroSanitario?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            med.fabricante?.toLowerCase().includes(searchTerm.toLowerCase())
        );

        setFilteredMedicamentos(filtered);
    }, [searchTerm, medicamentos]);

    const handleDelete = async (id) => {
        if (!window.confirm('¿Estás seguro de desactivar este medicamento?')) return;

        try {
            await axios.delete(`/api/medicamentos/${id}`);
            fetchMedicamentos();
        } catch (err) {
            alert('Error al desactivar medicamento');
        }
    };

    if (loading) return <div className="container">Cargando...</div>;
    if (error) return <div className="container" style={{ color: 'red' }}>{error}</div>;

    return (
        <div className="container">
            <div className="card">

                <div className="flex-between">
                    <h2>Gestión de Medicamentos</h2>

                    {user?.rol === 'ADMIN' && (
                        <button
                            className="btn btn-primary flex"
                            onClick={() => navigate('/admin/medicamentos/nuevo')}
                        >
                            <FiPlus /> Nuevo Medicamento
                        </button>
                    )}
                </div>

                <input
                    type="text"
                    placeholder="Buscar por nombre, principio activo, registro sanitario o fabricante..."
                    className="input-search"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    style={{ marginTop: 20 }}
                />

                <div style={{ overflowX: 'auto' }}>
                    <table className="table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Registro Sanitario</th>
                            <th>Nombre</th>
                            <th>Principio Activo</th>
                            <th>Presentación</th>
                            <th>Vía</th>
                            <th>Fabricante</th>
                            <th>Tipo Venta</th>
                            <th>Precio Venta</th>
                            <th>Stock Min</th>
                            <th>Stock Max</th>
                            <th>¿Receta?</th>
                            {user?.rol === 'ADMIN' && <th>Acciones</th>}
                        </tr>
                        </thead>

                        <tbody>
                        {filteredMedicamentos.map(med => (
                            <tr key={med.id}>
                                <td>{med.id}</td>
                                <td>{med.registroSanitario}</td>
                                <td>{med.nombre}</td>
                                <td>{med.principioActivo}</td>
                                <td>{med.presentacion}</td>
                                <td>{med.viaAdministracion}</td>
                                <td>{med.fabricante}</td>
                                <td>{med.tipoVenta}</td>
                                <td>C${med.precioVenta?.toFixed(2)}</td>
                                <td>{med.stockMinimo}</td>
                                <td>{med.stockMaximo}</td>
                                <td>{med.requiereReceta ? 'Sí' : 'No'}</td>

                                {user?.rol === 'ADMIN' && (
                                    <td>
                                        <div className="flex">
                                            <button
                                                className="btn btn-warning"
                                                style={{ padding: '4px 8px', marginRight: 5 }}
                                                onClick={() =>
                                                    navigate(`/admin/medicamentos/editar/${med.id}`)
                                                }
                                            >
                                                <FiEdit />
                                            </button>

                                            <button
                                                className="btn btn-danger"
                                                style={{ padding: '4px 8px' }}
                                                onClick={() => handleDelete(med.id)}
                                            >
                                                <FiTrash2 />
                                            </button>
                                        </div>
                                    </td>
                                )}
                            </tr>
                        ))}

                        {filteredMedicamentos.length === 0 && (
                            <tr>
                                <td
                                    colSpan={user?.rol === 'ADMIN' ? 13 : 12}
                                    style={{ textAlign: 'center' }}
                                >
                                    No se encontraron medicamentos
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

export default MedicamentoList;