import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import {
    listarProveedores,
    crearProveedor,
    actualizarProveedor,
    desactivarProveedor
} from '../services/proveedores';
import './Proveedores.css';

export default function Proveedores() {
    const { user } = useAuth();
    const esAdmin = user?.rol === 'ADMIN';

    // Estados principales
    const [proveedores, setProveedores] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [message, setMessage] = useState({ text: '', type: '' });
    const [loading, setLoading] = useState(false);

    // Estado para el Drawer (Panel Lateral)
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentId, setCurrentId] = useState(null);
    const [formData, setFormData] = useState({
        ruc: '',
        nombre: '',
        telefono: '',
        email: ''
    });

    useEffect(() => {
        cargarProveedores();
    }, []);

    const cargarProveedores = async () => {
        setLoading(true);
        try {
            const response = await listarProveedores();
            setProveedores(response.data);
        } catch (error) {
            setMessage({ text: 'Error al conectar con el servidor', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const proveedoresFiltrados = useMemo(() => {
        return proveedores.filter(p =>
                p.activo && (
                    p.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
                    p.ruc.includes(searchTerm)
                )
        );
    }, [proveedores, searchTerm]);

    const handleNuevo = () => {
        setEditMode(false);
        setFormData({ ruc: '', nombre: '', telefono: '', email: '' });
        setDrawerOpen(true);
    };

    const handleEditar = (prov) => {
        setEditMode(true);
        setCurrentId(prov.id);
        setFormData({
            ruc: prov.ruc,
            nombre: prov.nombre,
            telefono: prov.telefono || '',
            email: prov.email || ''
        });
        setDrawerOpen(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            if (editMode) {
                await actualizarProveedor(currentId, formData);
                setMessage({ text: 'Proveedor actualizado correctamente', type: 'success' });
            } else {
                await crearProveedor(formData);
                setMessage({ text: 'Proveedor registrado correctamente', type: 'success' });
            }
            setDrawerOpen(false);
            cargarProveedores();
        } catch (error) {
            const errorMsg = error.response?.data?.message || 'Error en la operación';
            setMessage({ text: errorMsg, type: 'error' });
        } finally {
            setLoading(false);
            setTimeout(() => setMessage({ text: '', type: '' }), 3000);
        }
    };

    const handleDesactivar = async (id) => {
        if (!window.confirm('¿Desea dar de baja a este proveedor?')) return;
        try {
            await desactivarProveedor(id);
            setMessage({ text: 'Proveedor desactivado', type: 'success' });
            cargarProveedores();
        } catch (error) {
            setMessage({ text: 'Error al desactivar', type: 'error' });
        }
    };

    return (
        <div className="module-container">
            <header className="module-header">
                <div>
                    <h1>Directorio de Proveedores</h1>
                    <p>Gestión de entidades comerciales y suministros</p>
                </div>
                <div className="header-actions">
                    <div className="search-box">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            placeholder="Buscar proveedor..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    {esAdmin && (
                        <button className="btn-primary-compact" onClick={handleNuevo}>
                            <span>+</span> Nuevo Proveedor
                        </button>
                    )}
                </div>
            </header>

            {message.text && (
                <div className={`alert-banner ${message.type}`}>
                    {message.text}
                    <button onClick={() => setMessage({ text: '', type: '' })}>×</button>
                </div>
            )}

            <div className="table-card">
                <table className="custom-table">
                    <thead>
                    <tr>
                        <th>RUC</th>
                        <th>Razón Social / Nombre</th>
                        <th>Teléfono</th>
                        <th>Correo Electrónico</th>
                        <th className="text-center">Acciones</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan="5" className="text-center">Cargando datos...</td></tr>
                    ) : proveedoresFiltrados.map(p => (
                        <tr key={p.id}>
                            <td className="font-mono">{p.ruc}</td>
                            <td><strong className="text-dark">{p.nombre}</strong></td>
                            <td>{p.telefono || '—'}</td>
                            <td>{p.email || '—'}</td>
                            <td className="text-center">
                                {esAdmin ? (
                                    <div className="action-buttons-group">
                                        <button className="btn-edit-icon" onClick={() => handleEditar(p)} title="Editar">
                                            ✏️
                                        </button>
                                        <button className="btn-edit-icon danger-hover" onClick={() => handleDesactivar(p.id)} title="Desactivar">
                                            🗑️
                                        </button>
                                    </div>
                                ) : (
                                    <span className="badge-read">Visualización</span>
                                )}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
                {!loading && proveedoresFiltrados.length === 0 && (
                    <div className="empty-state">No se encontraron proveedores activos.</div>
                )}
            </div>

            {/* PANEL LATERAL (DRAWER) */}
            {drawerOpen && esAdmin && (
                <div className="drawer-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="drawer-panel" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header-compact">
                            <h2>{editMode ? 'Editar Proveedor' : 'Nuevo Proveedor'}</h2>
                            <button className="close-btn-round" onClick={() => setDrawerOpen(false)}>×</button>
                        </div>

                        <form onSubmit={handleSubmit} className="drawer-body-scrollable">
                            <div className="form-content-inner">
                                <h4 className="section-divider">Datos Fiscales</h4>
                                <div className="field-group">
                                    <label>RUC (Identificación Fiscal) *</label>
                                    <input
                                        type="text"
                                        value={formData.ruc}
                                        onChange={(e) => setFormData({...formData, ruc: e.target.value})}
                                        disabled={editMode}
                                        placeholder="Ej: J031000000..."
                                        required
                                    />
                                </div>
                                <div className="field-group">
                                    <label>Nombre o Razón Social *</label>
                                    <input
                                        type="text"
                                        value={formData.nombre}
                                        onChange={(e) => setFormData({...formData, nombre: e.target.value})}
                                        placeholder="Nombre oficial del proveedor"
                                        required
                                    />
                                </div>

                                <h4 className="section-divider">Información de Contacto</h4>
                                <div className="field-group">
                                    <label>Teléfono de Contacto</label>
                                    <input
                                        type="text"
                                        value={formData.telefono}
                                        onChange={(e) => setFormData({...formData, telefono: e.target.value})}
                                        placeholder="+505 0000-0000"
                                    />
                                </div>
                                <div className="field-group">
                                    <label>Correo Electrónico</label>
                                    <input
                                        type="email"
                                        value={formData.email}
                                        onChange={(e) => setFormData({...formData, email: e.target.value})}
                                        placeholder="ejemplo@proveedor.com"
                                    />
                                </div>
                            </div>

                            <div className="drawer-footer">
                                <button type="button" className="btn-sec" onClick={() => setDrawerOpen(false)}>Cancelar</button>
                                <button type="submit" className="btn-primary-compact">
                                    {editMode ? 'Actualizar Datos' : 'Registrar Proveedor'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}