import { useState, useEffect } from 'react';
import { listarUsuarios, crearUsuario, actualizarUsuario } from '../services/usuarios';
import './Empleados.css';

export default function Empleados() {
    // --- ESTADOS ---
    const [usuarios, setUsuarios] = useState([]);
    const [message, setMessage] = useState({ text: '', type: '' });
    const [loading, setLoading] = useState(false);

    // Estado para el Drawer (Panel Lateral)
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState(null);
    const [formData, setFormData] = useState({
        username: '',
        password: '',
        rol: 'VENDEDOR'
    });

    useEffect(() => {
        cargarUsuarios();
    }, []);

    const cargarUsuarios = async () => {
        setLoading(true);
        try {
            const response = await listarUsuarios();
            setUsuarios(response.data);
            if (response.data.length === 0) {
                setMessage({ text: 'No hay usuarios registrados', type: 'info' });
            }
        } catch (error) {
            setMessage({ text: 'Error al conectar con el servidor', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const handleOpenCreate = () => {
        setEditMode(false);
        setSelectedUserId(null);
        setFormData({ username: '', password: '', rol: 'VENDEDOR' });
        setIsModalOpen(true);
    };

    const handleOpenEdit = (usuario) => {
        setEditMode(true);
        setSelectedUserId(usuario.id);
        setFormData({
            username: usuario.username,
            password: '',
            rol: usuario.rol
        });
        setIsModalOpen(true);
    };

    const handleSave = async (e) => {
        e.preventDefault();
        try {
            if (editMode) {
                const dataToUpdate = {
                    rol: formData.rol,
                    password: formData.password || undefined
                };
                await actualizarUsuario(selectedUserId, dataToUpdate);
                setMessage({ text: 'Usuario actualizado con éxito', type: 'success' });
            } else {
                await crearUsuario(formData);
                setMessage({ text: 'Usuario creado con éxito', type: 'success' });
            }

            setIsModalOpen(false);
            cargarUsuarios();
        } catch (error) {
            setMessage({ text: 'Error al procesar la solicitud', type: 'error' });
        }
    };

    return (
        <div className="module-container">
            {/* Cabecera del Módulo */}
            <header className="module-header">
                <div>
                    <h1>Gestión de Personal</h1>
                    <p>Administra los accesos y roles de la farmacia</p>
                </div>
                <button className="btn-primary" onClick={handleOpenCreate}>
                    <span className="plus-icon">+</span> Nuevo Usuario
                </button>
            </header>

            {/* Alertas dinámicas */}
            {message.text && (
                <div className={`alert-banner ${message.type}`}>
                    <span>{message.text}</span>
                    <button className="close-alert" onClick={() => setMessage({ text: '', type: '' })}>×</button>
                </div>
            )}

            {/* Tabla de Datos */}
            <div className="table-card">
                {loading ? (
                    <div className="loading-state">
                        <div className="spinner"></div>
                        <p>Cargando personal...</p>
                    </div>
                ) : (
                    <table className="custom-table">
                        <thead>
                        <tr>
                            <th>Usuario</th>
                            <th>Rol / Permisos</th>
                            <th>Estado</th>
                            <th className="text-center">Acciones</th>
                        </tr>
                        </thead>
                        <tbody>
                        {usuarios.map((u) => (
                            <tr key={u.id}>
                                <td className="user-td">
                                    <div className="avatar-small">
                                        {u.username.charAt(0).toUpperCase()}
                                    </div>
                                    <span className="username-text">{u.username}</span>
                                </td>
                                <td>
                                        <span className={`role-tag ${u.rol.toLowerCase()}`}>
                                            {u.rol}
                                        </span>
                                </td>
                                <td>
                                        <span className="status-badge active">
                                            <span className="dot"></span> Activo
                                        </span>
                                </td>
                                <td className="text-center">
                                    <button className="btn-text-edit" onClick={() => handleOpenEdit(u)}>
                                        ✏️ Editar
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* --- DRAWER (PANEL LATERAL) --- */}
            {isModalOpen && (
                <div className="drawer-overlay" onClick={() => setIsModalOpen(false)}>
                    <div className="drawer-content" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header">
                            <h2>{editMode ? 'Editar Perfil' : 'Nuevo Integrante'}</h2>
                            <button className="close-drawer" onClick={() => setIsModalOpen(false)}>×</button>
                        </div>

                        <form onSubmit={handleSave} className="drawer-form">
                            <div className="form-group">
                                <label>Nombre de Usuario</label>
                                <input
                                    type="text"
                                    value={formData.username}
                                    onChange={(e) => setFormData({...formData, username: e.target.value})}
                                    disabled={editMode}
                                    className={editMode ? "input-disabled" : ""}
                                    placeholder="Ej. mgarcia"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>
                                    {editMode ? 'Nueva Contraseña (opcional)' : 'Contraseña de Acceso'}
                                </label>
                                <input
                                    type="password"
                                    value={formData.password}
                                    onChange={(e) => setFormData({...formData, password: e.target.value})}
                                    placeholder={editMode ? "Dejar vacío para mantener" : "Mínimo 6 caracteres"}
                                    required={!editMode}
                                />
                            </div>

                            <div className="form-group">
                                <label>Rol Asignado</label>
                                <div className="role-selector">
                                    {['VENDEDOR', 'ADMIN'].map(role => (
                                        <button
                                            key={role}
                                            type="button"
                                            className={formData.rol === role ? 'active' : ''}
                                            onClick={() => setFormData({...formData, rol: role})}
                                        >
                                            {role}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div className="drawer-footer">
                                <button type="button" className="btn-sec" onClick={() => setIsModalOpen(false)}>
                                    Cancelar
                                </button>
                                <button type="submit" className="btn-primary">
                                    {editMode ? 'Actualizar' : 'Guardar Usuario'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}