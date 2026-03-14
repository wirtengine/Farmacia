import { useState, useEffect, useMemo } from 'react';
import { listarLotes, crearLote, actualizarLote, desactivarLote } from '../services/lotes';
import { listarMedicamentos } from '../services/medicamentos';
import { listarProveedores } from '../services/proveedores';
import { useAuth } from '../context/AuthContext';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import QRCode from 'qrcode';
import './Lotes.css';

export default function Lotes() {
    const { user } = useAuth();
    const isAdmin = user?.rol === 'ADMIN';

    // Estados de Datos
    const [lotes, setLotes] = useState([]);
    const [medicamentos, setMedicamentos] = useState([]);
    const [proveedores, setProveedores] = useState([]);

    // Estados de Filtros
    const [searchTerm, setSearchTerm] = useState('');
    const [filtroStock, setFiltroStock] = useState('todos'); // 'todos', 'stock', 'agotado'

    // Estados de UI
    const [message, setMessage] = useState({ text: '', type: '' });
    const [loading, setLoading] = useState(false);
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentId, setCurrentId] = useState(null);

    const [formData, setFormData] = useState({
        fechaFabricacion: '',
        fechaVencimiento: '',
        proveedorId: '',
        factura: '',
        detalles: []
    });

    useEffect(() => {
        cargarDatos();
    }, []);

    const cargarDatos = async () => {
        setLoading(true);
        try {
            const [resLotes, resMeds, resProvs] = await Promise.all([
                listarLotes(),
                listarMedicamentos(),
                listarProveedores()
            ]);
            setLotes(resLotes.data || []);
            setMedicamentos(resMeds.data || []);
            setProveedores(resProvs.data || []);
        } catch (error) {
            setMessage({ text: 'Error al cargar datos del sistema', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    // Lógica de búsqueda y filtrado avanzado
    const lotesFiltrados = useMemo(() => {
        const term = searchTerm.toLowerCase();

        return lotes.filter(l => {
            // 1. Búsqueda por Factura o Proveedor
            const nombreProveedor = proveedores.find(p => p.id === l.proveedorId)?.nombre.toLowerCase() || '';
            const coincideFacturaProv = l.factura?.toLowerCase().includes(term) || nombreProveedor.includes(term);

            // 2. Búsqueda por Nombre de Medicamento dentro del lote
            const tieneMedicamento = l.detalles?.some(d => {
                const nombreMed = medicamentos.find(m => m.id === d.medicamentoId)?.nombre.toLowerCase() || '';
                return nombreMed.includes(term);
            });

            const coincideBusqueda = coincideFacturaProv || tieneMedicamento;

            // 3. Filtro por Estado de Stock (Botones)
            const totalStock = l.detalles?.reduce((acc, d) => acc + d.cantidad, 0) || 0;

            if (filtroStock === 'stock') {
                return coincideBusqueda && totalStock > 0 && l.activo;
            }
            if (filtroStock === 'agotado') {
                return coincideBusqueda && totalStock === 0 && l.activo;
            }

            return coincideBusqueda;
        });
    }, [lotes, searchTerm, proveedores, medicamentos, filtroStock]);

    const handleEliminar = async (id) => {
        if (window.confirm('¿Está seguro de desactivar este lote?')) {
            setLoading(true);
            try {
                await desactivarLote(id);
                setMessage({ text: 'Lote desactivado correctamente', type: 'success' });
                cargarDatos();
            } catch (error) {
                setMessage({ text: 'Error al desactivar el lote', type: 'error' });
            } finally {
                setLoading(false);
                setTimeout(() => setMessage({ text: '', type: '' }), 3000);
            }
        }
    };

    const generarCodigoFactura = () => {
        const fecha = new Date();
        const year = fecha.getFullYear();
        const mes = String(fecha.getMonth() + 1).padStart(2, '0');
        const random = Math.floor(1000 + Math.random() * 9000);
        return `FAC-${year}${mes}-${random}`;
    };

    const handleNuevo = () => {
        setEditMode(false);
        setFormData({
            fechaFabricacion: new Date().toISOString().split('T')[0],
            fechaVencimiento: '',
            proveedorId: '',
            factura: generarCodigoFactura(),
            detalles: [{ medicamentoId: '', cantidad: 1 }]
        });
        setDrawerOpen(true);
    };

    const handleEditar = (lote) => {
        setEditMode(true);
        setCurrentId(lote.id);
        setFormData({
            fechaFabricacion: lote.fechaFabricacion?.split('T')[0] || '',
            fechaVencimiento: lote.fechaVencimiento?.split('T')[0] || '',
            proveedorId: lote.proveedorId || '',
            factura: lote.factura || '',
            detalles: lote.detalles.map(d => ({ medicamentoId: d.medicamentoId, cantidad: d.cantidad }))
        });
        setDrawerOpen(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            if (editMode) await actualizarLote(currentId, formData);
            else await crearLote(formData);
            setDrawerOpen(false);
            cargarDatos();
        } catch (error) {
            alert("Error al guardar lote");
        } finally {
            setLoading(false);
        }
    };

    const imprimirLote = async (lote) => {
        const doc = new jsPDF();
        const prov = proveedores.find(p => p.id === lote.proveedorId)?.nombre || 'N/A';
        doc.text(`Comprobante Lote: ${lote.factura}`, 14, 20);
        const filas = lote.detalles.map(d => [
            medicamentos.find(m => m.id === d.medicamentoId)?.nombre || 'S/N',
            d.cantidad
        ]);
        autoTable(doc, { startY: 30, head: [['Medicamento', 'Cant']], body: filas });
        doc.save(`Lote_${lote.factura}.pdf`);
    };

    return (
        <div className="module-container">
            <header className="module-header">
                <div>
                    <h1>Inventario de Lotes</h1>
                    <p>Filtra por factura, proveedor o medicamento</p>
                </div>

                <div className="header-actions">
                    {/* Filtros de Stock */}
                    <div className="stock-filter-group">
                        <button className={`btn-filter ${filtroStock === 'todos' ? 'active' : ''}`} onClick={() => setFiltroStock('todos')}>Todos</button>
                        <button className={`btn-filter ${filtroStock === 'stock' ? 'active' : ''}`} onClick={() => setFiltroStock('stock')}>En Stock</button>
                        <button className={`btn-filter ${filtroStock === 'agotado' ? 'active' : ''}`} onClick={() => setFiltroStock('agotado')}>Agotados</button>
                    </div>

                    <div className="search-box">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            placeholder="Buscar factura, proveedor o producto..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    {isAdmin && (
                        <button className="btn-primary-compact" onClick={handleNuevo}>＋ Registrar Entrada</button>
                    )}
                </div>
            </header>

            <div className="table-card">
                <table className="custom-table">
                    <thead>
                    <tr>
                        <th>Factura Ref.</th>
                        <th>Proveedor</th>
                        <th>Medicamentos</th>
                        <th>Vencimiento</th>
                        <th>Estado</th>
                        <th className="text-center">Acciones</th>
                    </tr>
                    </thead>
                    <tbody>
                    {lotesFiltrados.map(l => {
                        const totalStock = l.detalles?.reduce((acc, d) => acc + d.cantidad, 0) || 0;
                        const tieneStock = totalStock > 0;
                        return (
                            <tr key={l.id} className={!l.activo ? 'row-inactive' : ''}>
                                <td className="font-bold">{l.factura}</td>
                                <td>{proveedores.find(p => p.id === l.proveedorId)?.nombre || '—'}</td>
                                <td>
                                    <div className="items-chip-container">
                                        {l.detalles?.map((det, idx) => (
                                            <span key={idx} className="med-chip">
                                                    {medicamentos.find(m => m.id === det.medicamentoId)?.nombre || 'S/N'}
                                                <small>x{det.cantidad}</small>
                                                </span>
                                        ))}
                                    </div>
                                </td>
                                <td className={new Date(l.fechaVencimiento) < new Date() ? 'text-danger' : ''}>
                                    {l.fechaVencimiento}
                                </td>
                                <td>
                                        <span className={`status-pill ${l.activo ? (tieneStock ? 'active' : 'agotado') : 'inactive'}`}>
                                            {l.activo ? (tieneStock ? 'En Stock' : 'Agotado') : 'Inactivo'}
                                        </span>
                                </td>
                                <td className="text-center">
                                    <div className="action-buttons-group">
                                        <button className="btn-edit-icon" onClick={() => imprimirLote(l)}>📄</button>
                                        {isAdmin && l.activo && (
                                            <>
                                                <button className="btn-edit-icon" onClick={() => handleEditar(l)}>✏️</button>
                                                <button className="btn-delete-icon" onClick={() => handleEliminar(l.id)}>🗑️</button>
                                            </>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        );
                    })}
                    </tbody>
                </table>
            </div>

            {/* Formulario Lateral (Drawer) */}
            {drawerOpen && (
                <div className="drawer-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="drawer-panel" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header-compact">
                            <h2>{editMode ? 'Editar Lote' : 'Nuevo Lote'}</h2>
                            <button className="close-btn-round" onClick={() => setDrawerOpen(false)}>×</button>
                        </div>
                        <form onSubmit={handleSubmit} className="drawer-body-scrollable">
                            <div className="field-group">
                                <label>Factura</label>
                                <input type="text" value={formData.factura} readOnly className="input-readonly" />
                            </div>
                            <div className="field-group">
                                <label>Proveedor</label>
                                <select value={formData.proveedorId} onChange={e => setFormData({...formData, proveedorId: e.target.value})} required>
                                    <option value="">Seleccione...</option>
                                    {proveedores.map(p => <option key={p.id} value={p.id}>{p.nombre}</option>)}
                                </select>
                            </div>
                            <div className="field-grid-2">
                                <div className="field-group">
                                    <label>Fabricación</label>
                                    <input type="date" value={formData.fechaFabricacion} onChange={e => setFormData({...formData, fechaFabricacion: e.target.value})} />
                                </div>
                                <div className="field-group">
                                    <label>Vencimiento</label>
                                    <input type="date" value={formData.fechaVencimiento} onChange={e => setFormData({...formData, fechaVencimiento: e.target.value})} required />
                                </div>
                            </div>

                            <h4 className="section-divider">Productos</h4>
                            {formData.detalles.map((det, index) => (
                                <div className="item-entry-row" key={index}>
                                    <select className="flex-2" value={det.medicamentoId} onChange={e => {
                                        const newDet = [...formData.detalles];
                                        newDet[index].medicamentoId = e.target.value;
                                        setFormData({...formData, detalles: newDet});
                                    }} required>
                                        <option value="">Medicamento...</option>
                                        {medicamentos.map(m => <option key={m.id} value={m.id}>{m.nombre}</option>)}
                                    </select>
                                    <input type="number" className="flex-1" value={det.cantidad} onChange={e => {
                                        const newDet = [...formData.detalles];
                                        newDet[index].cantidad = parseInt(e.target.value);
                                        setFormData({...formData, detalles: newDet});
                                    }} min="1" required />
                                    <button type="button" className="btn-delete-small" onClick={() => setFormData({...formData, detalles: formData.detalles.filter((_, i) => i !== index)})}>×</button>
                                </div>
                            ))}
                            <button type="button" className="btn-add-item" onClick={() => setFormData({...formData, detalles: [...formData.detalles, {medicamentoId: '', cantidad: 1}]})}>+ Añadir</button>

                            <div className="drawer-footer-fixed">
                                <button type="button" className="btn-cancel" onClick={() => setDrawerOpen(false)}>Cancelar</button>
                                <button type="submit" className="btn-save-final">Guardar Entrada</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}