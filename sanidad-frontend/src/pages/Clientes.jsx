import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import {
    listarClientes,
    crearCliente,
    actualizarCliente,
    desactivarCliente
} from '../services/clientes';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import './Clientes.css';

export default function Clientes() {

    const { user } = useAuth();
    const tienePermiso = user?.rol === 'ADMIN' || user?.rol === 'VENDEDOR';

    const [clientes, setClientes] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [message, setMessage] = useState({ text: '', type: '' });
    const [loading, setLoading] = useState(false);

    const [drawerOpen, setDrawerOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentId, setCurrentId] = useState(null);

    const [formData, setFormData] = useState({
        cedula: '',
        nombre: '',
        telefono: '',
        email: '',
        saldo: 0
    });

    useEffect(() => {
        cargarClientes();
    }, []);

    const cargarClientes = async () => {
        setLoading(true);
        try {
            const response = await listarClientes();
            setClientes(response.data);
        } catch (error) {
            setMessage({ text: 'Error al cargar clientes', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const clientesFiltrados = useMemo(() => {
        const term = searchTerm.toLowerCase();
        return clientes.filter(c =>
            c.nombre.toLowerCase().includes(term) ||
            c.cedula.includes(term)
        );
    }, [clientes, searchTerm]);

    const handleNuevo = () => {
        setEditMode(false);
        setFormData({
            cedula: '',
            nombre: '',
            telefono: '',
            email: '',
            saldo: 0
        });
        setDrawerOpen(true);
    };

    const handleEditar = (cliente) => {
        setEditMode(true);
        setCurrentId(cliente.id);

        setFormData({
            cedula: cliente.cedula,
            nombre: cliente.nombre,
            telefono: cliente.telefono || '',
            email: cliente.email || '',
            saldo: Number(cliente.saldo) || 0
        });

        setDrawerOpen(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            if (editMode) {
                await actualizarCliente(currentId, formData);
                setMessage({ text: 'Cliente actualizado con éxito', type: 'success' });
            } else {
                await crearCliente(formData);
                setMessage({ text: 'Cliente registrado con éxito', type: 'success' });
            }

            setDrawerOpen(false);
            cargarClientes();

        } catch (error) {
            setMessage({ text: 'Error en la operación', type: 'error' });
        } finally {
            setLoading(false);
            setTimeout(() => setMessage({ text: '', type: '' }), 3000);
        }
    };

    const handleDesactivar = async (id) => {
        if (!window.confirm('¿Está seguro de desactivar este cliente?')) return;

        try {
            await desactivarCliente(id);
            setMessage({ text: 'Cliente desactivado (Soft Delete)', type: 'success' });
            cargarClientes();
        } catch (error) {
            setMessage({ text: 'Error al desactivar', type: 'error' });
        }
    };

    const imprimirReporteCliente = (cliente) => {

        const doc = new jsPDF();

        doc.setFontSize(18);
        doc.text('REPORTE DE CLIENTE', 14, 20);

        doc.setFontSize(12);
        doc.text(`Cédula: ${cliente.cedula}`, 14, 35);
        doc.text(`Nombre: ${cliente.nombre}`, 14, 45);
        doc.text(`Teléfono: ${cliente.telefono || 'N/A'}`, 14, 55);
        doc.text(`Email: ${cliente.email || 'N/A'}`, 14, 65);
        doc.text(`Saldo Actual: $${cliente.saldo?.toFixed(2)}`, 14, 75);
        doc.text(`Estado: ${cliente.activo ? 'ACTIVO' : 'INACTIVO'}`, 14, 85);

        doc.save(`Reporte_${cliente.cedula}.pdf`);
    };

    const imprimirTodosLosClientes = () => {

        const doc = new jsPDF();

        doc.text('LISTADO GENERAL DE CLIENTES', 14, 15);

        const filas = clientesFiltrados.map(c => [
            c.cedula,
            c.nombre,
            c.telefono || '—',
            `$${c.saldo?.toFixed(2)}`,
            c.activo ? 'Activo' : 'Inactivo'
        ]);

        autoTable(doc, {
            startY: 25,
            head: [['Cédula', 'Nombre', 'Teléfono', 'Saldo', 'Estado']],
            body: filas
        });

        doc.save('Listado_Clientes.pdf');
    };

    return (
        <div className="module-container">

            <header className="module-header">
                <div>
                    <h1>Gestión de Clientes</h1>
                    <p>Directorio de clientes y estados de cuenta</p>
                </div>

                <div className="header-actions">
                    <button
                        className="btn-edit-icon"
                        onClick={imprimirTodosLosClientes}
                        title="Reporte General"
                    >
                        🖨️ PDF
                    </button>

                    {tienePermiso && (
                        <button
                            className="btn-primary-compact"
                            onClick={handleNuevo}
                        >
                            + Nuevo Cliente
                        </button>
                    )}
                </div>
            </header>

            <div className="toolbar-clientes">

                <div className="search-box">
                    <span className="search-icon">🔍</span>

                    <input
                        type="text"
                        placeholder="Buscar por cédula o nombre..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                </div>

            </div>

            {message.text && (
                <div className={`alert-banner ${message.type}`}>
                    {message.text}
                </div>
            )}

            <div className="table-card">

                <table className="custom-table">

                    <thead>
                    <tr>
                        <th>Cédula</th>
                        <th>Nombre</th>
                        <th>Teléfono</th>
                        <th>Saldo</th>
                        <th>Estado</th>
                        <th className="text-center">Acciones</th>
                    </tr>
                    </thead>

                    <tbody>

                    {loading ? (

                        <tr>
                            <td colSpan="6" className="text-center">
                                Cargando datos...
                            </td>
                        </tr>

                    ) : clientesFiltrados.map(c => (

                        <tr key={c.id} className={!c.activo ? 'row-inactive' : ''}>

                            <td className="font-bold">{c.cedula}</td>

                            <td>{c.nombre}</td>

                            <td>{c.telefono || '—'}</td>

                            <td className="font-semibold">
                                ${c.saldo?.toFixed(2)}
                            </td>

                            <td>
                                <span className={`status-pill ${c.activo ? 'active' : 'inactive'}`}>
                                    {c.activo ? 'Activo' : 'Inactivo'}
                                </span>
                            </td>

                            <td className="text-center">

                                <div className="action-buttons-group">

                                    <button
                                        className="btn-edit-icon"
                                        onClick={() => imprimirReporteCliente(c)}
                                        title="Ficha Cliente"
                                    >
                                        📄
                                    </button>

                                    {tienePermiso && c.activo && (
                                        <>
                                            <button
                                                className="btn-edit-icon"
                                                onClick={() => handleEditar(c)}
                                                title="Editar"
                                            >
                                                ✏️
                                            </button>

                                            <button
                                                className="btn-delete-icon"
                                                onClick={() => handleDesactivar(c.id)}
                                                title="Desactivar"
                                            >
                                                🗑️
                                            </button>
                                        </>
                                    )}

                                </div>

                            </td>

                        </tr>

                    ))}

                    </tbody>

                </table>

            </div>

            {drawerOpen && (

                <div
                    className="drawer-overlay"
                    onClick={() => setDrawerOpen(false)}
                >

                    <div
                        className="drawer-panel"
                        onClick={e => e.stopPropagation()}
                    >

                        <div className="drawer-header-compact">

                            <h2>
                                {editMode ? 'Actualizar Cliente' : 'Registro de Cliente'}
                            </h2>

                            <button
                                className="close-btn-round"
                                onClick={() => setDrawerOpen(false)}
                            >
                                ×
                            </button>

                        </div>

                        <form
                            onSubmit={handleSubmit}
                            className="drawer-body-scrollable"
                        >

                            <div className="form-content-inner">

                                <h4 className="section-divider">
                                    Datos Personales
                                </h4>

                                <div className="field-group">

                                    <label>Cédula / Identificación *</label>

                                    <input
                                        type="text"
                                        value={formData.cedula}
                                        onChange={e =>
                                            setFormData({
                                                ...formData,
                                                cedula: e.target.value
                                            })
                                        }
                                        disabled={editMode}
                                        required
                                    />

                                </div>

                                <div className="field-group">

                                    <label>Nombre Completo *</label>

                                    <input
                                        type="text"
                                        value={formData.nombre}
                                        onChange={e =>
                                            setFormData({
                                                ...formData,
                                                nombre: e.target.value
                                            })
                                        }
                                        required
                                    />

                                </div>

                                <h4 className="section-divider">
                                    Contacto y Cuenta
                                </h4>

                                <div className="field-grid-2">

                                    <div className="field-group">

                                        <label>Teléfono</label>

                                        <input
                                            type="text"
                                            value={formData.telefono}
                                            onChange={e =>
                                                setFormData({
                                                    ...formData,
                                                    telefono: e.target.value
                                                })
                                            }
                                        />

                                    </div>

                                    <div className="field-group">

                                        <label>Saldo Inicial ($)</label>

                                        <input
                                            type="number"
                                            step="0.01"
                                            value={formData.saldo}
                                            onChange={e =>
                                                setFormData({
                                                    ...formData,
                                                    saldo: parseFloat(e.target.value) || 0
                                                })
                                            }
                                        />

                                    </div>

                                </div>

                                <div className="field-group">

                                    <label>Correo Electrónico</label>

                                    <input
                                        type="email"
                                        value={formData.email}
                                        onChange={e =>
                                            setFormData({
                                                ...formData,
                                                email: e.target.value
                                            })
                                        }
                                    />

                                </div>

                            </div>

                            <div className="drawer-footer-fixed">

                                <button
                                    type="button"
                                    className="btn-cancel"
                                    onClick={() => setDrawerOpen(false)}
                                >
                                    Cancelar
                                </button>

                                <button
                                    type="submit"
                                    className="btn-save-final"
                                    disabled={loading}
                                >
                                    {loading
                                        ? 'Guardando...'
                                        : (editMode
                                            ? 'Guardar Cambios'
                                            : 'Registrar Cliente')}
                                </button>

                            </div>

                        </form>

                    </div>

                </div>

            )}

        </div>
    );
}
