import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import {
    listarMedicamentos,
    crearMedicamento,
    actualizarMedicamento,
    desactivarMedicamento,
    reactivarMedicamento
} from '../services/medicamentos';
import './Medicamentos.css';

// Importaciones para PDF
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

export default function Medicamentos() {
    const { user } = useAuth();
    const [medicamentos, setMedicamentos] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [message, setMessage] = useState({ text: '', type: '' });
    const [loading, setLoading] = useState(false);
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentId, setCurrentId] = useState(null);

    const [formData, setFormData] = useState({
        registroSanitario: '', nombre: '', presentacion: '',
        via: '', fabricante: '', tipoVenta: 'LIBRE',
        precioUnitario: '', receta: false, activo: true
    });

    useEffect(() => { cargarMedicamentos(); }, []);

    const cargarMedicamentos = async () => {
        setLoading(true);
        try {
            const response = await listarMedicamentos();
            setMedicamentos(response.data);
        } catch (error) {
            setMessage({ text: 'Error al conectar con el servidor', type: 'error' });
        } finally { setLoading(false); }
    };

    // FUNCIÓN PDF CORREGIDA
    const generarPDF = () => {
        const doc = new jsPDF();
        const fecha = new Date().toLocaleDateString();

        // Intentar obtener el nombre del usuario de varias formas comunes
        const nombreUsuario = user?.nombre || user?.username || user?.sub || 'Usuario del Sistema';

        doc.setFontSize(18);
        doc.text('Catálogo de Medicamentos - Sanidad App', 14, 20);
        doc.setFontSize(10);
        doc.text(`Fecha de reporte: ${fecha}`, 14, 28);
        doc.text(`Generado por: ${nombreUsuario}`, 14, 33);

        const columnas = ["Medicamento", "Reg. Sanitario", "Fabricante", "Presentación", "Vía", "Precio", "Estado"];

        const filas = medicamentosFiltrados.map(m => [
            m.nombre,
            m.registroSanitario,
            m.fabricante || 'N/A',
            m.presentacion,
            m.via,
            `C$ ${parseFloat(m.precioUnitario).toFixed(2)}`,
            m.activo ? 'Activo' : 'Inactivo'
        ]);

        autoTable(doc, {
            head: [columnas],
            body: filas,
            startY: 40,
            theme: 'striped',
            headStyles: { fillColor: [41, 128, 185], textColor: 255 },
            styles: { fontSize: 9 }
        });

        doc.save(`Reporte_Medicamentos_${fecha}.pdf`);
    };

    const medicamentosFiltrados = medicamentos.filter(m => {
        const esActivo = m.activo === true || String(m.activo) === 'true';
        // Vendedor solo ve activos, Admin ve todos
        if (user?.rol !== 'ADMIN' && !esActivo) return false;

        const term = searchTerm.toLowerCase();
        return m.nombre.toLowerCase().includes(term) ||
            m.fabricante?.toLowerCase().includes(term) ||
            m.registroSanitario.toLowerCase().includes(term);
    });

    const handleNuevo = () => {
        setEditMode(false);
        setFormData({
            registroSanitario: '', nombre: '', presentacion: '', via: '',
            fabricante: '', tipoVenta: 'LIBRE', precioUnitario: '', receta: false, activo: true
        });
        setDrawerOpen(true);
    };

    const handleEditar = (med) => {
        setEditMode(true);
        setCurrentId(med.id);
        setFormData({ ...med, activo: med.activo === true || String(med.activo) === 'true' });
        setDrawerOpen(true);
    };

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        let finalValue = type === 'checkbox' ? checked : value;
        if (name === 'activo') finalValue = value === 'true';
        setFormData(prev => ({ ...prev, [name]: finalValue }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            if (editMode) {
                await actualizarMedicamento(currentId, formData);
                setMessage({ text: 'Actualizado correctamente', type: 'success' });
            } else {
                await crearMedicamento(formData);
                setMessage({ text: 'Registrado correctamente', type: 'success' });
            }
            setDrawerOpen(false);
            await cargarMedicamentos();
        } catch (error) {
            const errorMsg = error.response?.data?.message || 'Error: El registro sanitario ya existe o hay un problema de conexión';
            setMessage({ text: errorMsg, type: 'error' });
        } finally { setLoading(false); }
    };

    const handleDesactivar = async (id) => {
        if (!window.confirm('¿Desactivar este medicamento? No aparecerá en el módulo de ventas.')) return;
        try {
            await desactivarMedicamento(id);
            setMessage({ text: 'Medicamento desactivado', type: 'success' });
            cargarMedicamentos();
        } catch (error) {
            setMessage({ text: 'Error al desactivar', type: 'error' });
        }
    };

    const handleReactivar = async (id) => {
        try {
            await reactivarMedicamento(id);
            setMessage({ text: 'Medicamento reactivado', type: 'success' });
            cargarMedicamentos();
        } catch (error) {
            setMessage({ text: 'Error al reactivar', type: 'error' });
        }
    };

    return (
        <div className="module-container">
            <header className="module-header">
                <div>
                    <h1>Catálogo Farmacéutico</h1>
                    <p>Gestión de Medicamentos e Insumos</p>
                </div>
                <div className="header-actions">
                    <div className="search-box">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            placeholder="Buscar medicamento..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    <button className="btn-print" onClick={generarPDF} title="Generar Reporte PDF">
                        🖨️ Imprimir PDF
                    </button>

                    {/* Solo Admin ve botón de Nuevo */}
                    {user?.rol === 'ADMIN' && (
                        <button className="btn-primary-compact" onClick={handleNuevo}>
                            <span>+</span> Nuevo Medicamento
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
                        <th>Medicamento</th>
                        <th>Registro</th>
                        <th>Fabricante</th>
                        <th>Presentación</th>
                        <th>Vía</th>
                        <th>Precio</th>
                        <th>Estado</th>
                        {/* Solo Admin ve columna de Acciones */}
                        {user?.rol === 'ADMIN' && <th className="text-center">Acciones</th>}
                    </tr>
                    </thead>
                    <tbody>
                    {medicamentosFiltrados.map(m => (
                        <tr key={m.id}>
                            <td className="font-bold">{m.nombre}</td>
                            <td className="text-muted">{m.registroSanitario}</td>
                            <td>{m.fabricante || '-'}</td>
                            <td><span className="badge-gray">{m.presentacion}</span></td>
                            <td><span className="badge-blue">{m.via}</span></td>
                            <td className="price-text">C$ {parseFloat(m.precioUnitario).toFixed(2)}</td>
                            <td>
                                <span className={`status-pill ${m.activo ? 'active' : 'inactive'}`}>
                                    {m.activo ? 'Activo' : 'Inactivo'}
                                </span>
                            </td>
                            {/* Solo Admin ve botones de acción */}
                            {user?.rol === 'ADMIN' && (
                                <td className="text-center">
                                    <div className="action-buttons-group">
                                        <button className="btn-edit-icon" title="Editar" onClick={() => handleEditar(m)}>✏️</button>
                                        {m.activo ? (
                                            <button className="btn-delete-icon" title="Desactivar" onClick={() => handleDesactivar(m.id)}>🗑️</button>
                                        ) : (
                                            <button className="btn-restore-icon" title="Reactivar" onClick={() => handleReactivar(m.id)}>↩️</button>
                                        )}
                                    </div>
                                </td>
                            )}
                        </tr>
                    ))}
                    </tbody>
                </table>
                {medicamentosFiltrados.length === 0 && (
                    <div className="empty-state">No hay resultados para mostrar.</div>
                )}
            </div>

            {/* El Drawer solo es accesible por Admin ya que el botón handleNuevo/Editar están protegidos */}
            {drawerOpen && user?.rol === 'ADMIN' && (
                <div className="drawer-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="drawer-panel" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header-compact">
                            <h2>{editMode ? 'Editar' : 'Nuevo'} Medicamento</h2>
                            <button className="close-btn-round" onClick={() => setDrawerOpen(false)}>×</button>
                        </div>

                        <form onSubmit={handleSubmit} className="drawer-body-scrollable">
                            <div className="form-content-inner">
                                <h4 className="section-divider">Información General</h4>
                                <div className="field-group">
                                    <label>Nombre Comercial *</label>
                                    <input type="text" name="nombre" value={formData.nombre} onChange={handleChange} required />
                                </div>
                                <div className="field-grid-2">
                                    <div className="field-group">
                                        <label>Reg. Sanitario *</label>
                                        <input type="text" name="registroSanitario" value={formData.registroSanitario} onChange={handleChange} required disabled={editMode} />
                                    </div>
                                    <div className="field-group">
                                        <label>Fabricante *</label>
                                        <input type="text" name="fabricante" value={formData.fabricante} onChange={handleChange} required />
                                    </div>
                                </div>

                                <h4 className="section-divider">Detalles Técnicos</h4>
                                <div className="field-grid-2">
                                    <div className="field-group">
                                        <label>Presentación *</label>
                                        <select name="presentacion" value={formData.presentacion} onChange={handleChange} required>
                                            <option value="">Seleccione...</option>
                                            <optgroup label="Sólidas 💊">
                                                <option value="Tableta">Tableta</option>
                                                <option value="Cápsula">Cápsula</option>
                                                <option value="Gragea">Gragea</option>
                                                <option value="Polvo">Polvo</option>
                                            </optgroup>
                                            <optgroup label="Líquidas 🧴">
                                                <option value="Jarabe">Jarabe</option>
                                                <option value="Solución">Solución</option>
                                                <option value="Suspensión">Suspensión</option>
                                                <option value="Gotas">Gotas</option>
                                            </optgroup>
                                            <optgroup label="Semisólidas 🧴">
                                                <option value="Crema">Crema</option>
                                                <option value="Pomada">Pomada</option>
                                                <option value="Gel">Gel</option>
                                            </optgroup>
                                            <optgroup label="Especiales 💉">
                                                <option value="Inyección">Inyección</option>
                                                <option value="Supositorio">Supositorio</option>
                                                <option value="Óvulo">Óvulo</option>
                                                <option value="Inhalador">Inhalador</option>
                                            </optgroup>
                                        </select>
                                    </div>
                                    <div className="field-group">
                                        <label>Vía de Admón. *</label>
                                        <select name="via" value={formData.via} onChange={handleChange} required>
                                            <option value="">Seleccione...</option>
                                            <option value="ORAL">👄 Oral</option>
                                            <option value="SUBLINGUAL">👅 Sublingual</option>
                                            <option value="TOPICA">🧴 Tópica</option>
                                            <option value="INHALATORIA">🌬️ Inhalatoria</option>
                                            <option value="RECTAL">🚽 Rectal</option>
                                            <option value="VAGINAL">🚺 Vaginal</option>
                                            <option value="PARENTERAL">💉 Parenteral</option>
                                        </select>
                                    </div>
                                </div>

                                <div className="field-grid-2">
                                    <div className="field-group">
                                        <label>Tipo de Venta *</label>
                                        <select name="tipoVenta" value={formData.tipoVenta} onChange={handleChange} required>
                                            <option value="LIBRE">🟢 Libre</option>
                                            <option value="CONTROLADO">🟡 Controlado</option>
                                            <option value="HOSPITALARIO">🔴 Hospitalario</option>
                                        </select>
                                    </div>
                                    <div className="field-group">
                                        <label>Precio (C$) *</label>
                                        <input type="number" step="0.01" name="precioUnitario" value={formData.precioUnitario} onChange={handleChange} required />
                                    </div>
                                </div>

                                {editMode && (
                                    <div className="field-group">
                                        <label>Estado del Producto</label>
                                        <select name="activo" value={formData.activo} onChange={handleChange}>
                                            <option value={true}>Activo (Visible)</option>
                                            <option value={false}>Inactivo (Oculto)</option>
                                        </select>
                                    </div>
                                )}

                                <div className="receta-warning-card">
                                    <input type="checkbox" id="receta" name="receta" checked={formData.receta} onChange={handleChange} />
                                    <label htmlFor="receta">Requiere receta médica obligatoria</label>
                                </div>
                                <div style={{ height: '80px' }}></div>
                            </div>
                            <div className="drawer-footer-fixed">
                                <button type="button" className="btn-cancel" onClick={() => setDrawerOpen(false)}>Cancelar</button>
                                <button type="submit" className="btn-save-final" disabled={loading}>
                                    {loading ? '...' : editMode ? 'Actualizar' : 'Guardar'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}