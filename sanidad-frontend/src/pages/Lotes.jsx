import { useState, useEffect, useMemo } from 'react';
import { listarLotes, crearLote, desactivarLote } from '../services/lotes';
import { listarMedicamentos } from '../services/medicamentos';
import { listarProveedores } from '../services/proveedores';
import { listarRacks } from '../services/racks';
import { listarUbicacionesPorRack } from '../services/ubicaciones';
import { useAuth } from '../context/AuthContext';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import RackVisualization from '../components/RackVisualization';
import './Lotes.css';

export default function Lotes() {
    const { user } = useAuth();
    const isAdmin = user?.rol === 'ADMIN';

    // Estados de Datos
    const [lotes, setLotes] = useState([]);
    const [medicamentos, setMedicamentos] = useState([]);
    const [proveedores, setProveedores] = useState([]);
    const [racks, setRacks] = useState([]);

    // Estados de Filtros
    const [searchTerm, setSearchTerm] = useState('');
    const [filtroStock, setFiltroStock] = useState('todos');

    // Estados de UI
    const [message, setMessage] = useState({ text: '', type: '' });
    const [loading, setLoading] = useState(false);
    const [drawerOpen, setDrawerOpen] = useState(false);

    // Paginación
    const [currentPage, setCurrentPage] = useState(1);
    const rowsPerPage = 15;

    // Formulario nuevo lote
    const [formData, setFormData] = useState({
        fechaFabricacion: '',
        fechaVencimiento: '',
        proveedorId: '',
        factura: '',
        detalles: []
    });

    // Selector de ubicación
    const [locationSelectorOpen, setLocationSelectorOpen] = useState(false);
    const [currentDetailIndex, setCurrentDetailIndex] = useState(null);
    const [selectedRackForSelector, setSelectedRackForSelector] = useState(null);
    const [ubicacionesForSelector, setUbicacionesForSelector] = useState([]);
    const [loadingUbicaciones, setLoadingUbicaciones] = useState(false);

    useEffect(() => {
        cargarRacks();
        cargarDatos();
    }, []);

    const cargarRacks = async () => {
        try {
            const res = await listarRacks();
            setRacks(res.data);
        } catch (error) {
            console.error('Error cargando racks', error);
        }
    };

    const cargarDatos = async () => {
        setLoading(true);
        try {
            const [resLotes, resMeds, resProvs] = await Promise.all([
                listarLotes(),
                listarMedicamentos(),
                listarProveedores()
            ]);
            const sortedLotes = (resLotes.data || []).sort((a, b) => b.id - a.id);
            setLotes(sortedLotes);
            setMedicamentos(resMeds.data || []);
            setProveedores(resProvs.data || []);
        } catch (error) {
            setMessage({ text: 'Error al cargar datos del sistema', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const lotesFiltrados = useMemo(() => {
        const term = searchTerm.toLowerCase().trim();
        return lotes.filter(l => {
            if (!isAdmin && !l.activo) return false;
            const proveedor = proveedores.find(p => p.id === l.proveedorId);
            const nombreProveedor = proveedor?.nombre.toLowerCase() || '';
            const facturaMatch = l.factura?.toLowerCase().includes(term);
            const proveedorMatch = nombreProveedor.includes(term);
            const medicamentoMatch = l.detalles?.some(d => {
                const med = medicamentos.find(m => m.id === d.medicamentoId);
                return med?.nombre.toLowerCase().includes(term);
            });
            const coincideBusqueda = !term || facturaMatch || proveedorMatch || medicamentoMatch;
            const totalStock = l.detalles?.reduce((acc, d) => acc + (d.cantidad || 0), 0) || 0;
            if (filtroStock === 'stock') return coincideBusqueda && totalStock > 0 && l.activo;
            if (filtroStock === 'agotado') return coincideBusqueda && totalStock === 0 && l.activo;
            return coincideBusqueda && l.activo;
        });
    }, [lotes, searchTerm, proveedores, medicamentos, filtroStock, isAdmin]);

    useEffect(() => setCurrentPage(1), [searchTerm, filtroStock]);
    const totalPages = Math.ceil(lotesFiltrados.length / rowsPerPage);
    const paginatedLotes = useMemo(() => {
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        return lotesFiltrados.slice(start, end);
    }, [lotesFiltrados, currentPage]);

    const goToPage = (page) => { if (page >= 1 && page <= totalPages) setCurrentPage(page); };

    const renderPageNumbers = () => {
        const pages = [];
        for (let i = 1; i <= totalPages; i++) {
            pages.push(
                <button key={i} className={`pagination-number ${currentPage === i ? 'active' : ''}`} onClick={() => goToPage(i)}>
                    {i}
                </button>
            );
        }
        return pages;
    };

    const handleDesactivar = async (id) => {
        if (!window.confirm('¿Está seguro de desactivar este lote?')) return;
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
    };

    const generarCodigoFactura = () => {
        const fecha = new Date();
        const year = fecha.getFullYear();
        const mes = String(fecha.getMonth() + 1).padStart(2, '0');
        const random = Math.floor(1000 + Math.random() * 9000);
        return `FAC-${year}${mes}-${random}`;
    };

    const handleNuevo = () => {
        setFormData({
            fechaFabricacion: new Date().toISOString().split('T')[0],
            fechaVencimiento: '',
            proveedorId: '',
            factura: generarCodigoFactura(),
            detalles: [{ medicamentoId: '', cantidad: 1, rackId: '', nivel: 0, columna: 0, profundidadIndex: 0 }]
        });
        setDrawerOpen(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!formData.proveedorId || !formData.fechaVencimiento) {
            alert('Por favor complete los campos obligatorios');
            return;
        }
        setLoading(true);
        try {
            await crearLote(formData);
            setDrawerOpen(false);
            setMessage({ text: 'Lote registrado correctamente', type: 'success' });
            cargarDatos();
        } catch (error) {
            alert('Error al guardar lote');
        } finally {
            setLoading(false);
        }
    };

    const imprimirLote = async (lote) => {
        const doc = new jsPDF();
        const proveedor = proveedores.find(p => p.id === lote.proveedorId)?.nombre || 'Desconocido';
        doc.setFontSize(16);
        doc.text(`Comprobante de Lote: ${lote.factura}`, 14, 20);
        const filas = lote.detalles.map(d => {
            const med = medicamentos.find(m => m.id === d.medicamentoId);
            return [med?.nombre || 'S/N', d.cantidad];
        });
        autoTable(doc, {
            startY: 45,
            head: [['Medicamento', 'Cantidad']],
            body: filas,
            theme: 'striped'
        });
        doc.save(`Lote_${lote.factura}.pdf`);
    };

    const openLocationSelector = (index) => {
        setCurrentDetailIndex(index);
        setLocationSelectorOpen(true);
        const detail = formData.detalles[index];
        if (detail.rackId) {
            const rack = racks.find(r => r.id === detail.rackId);
            if (rack) {
                setSelectedRackForSelector(rack);
                loadUbicacionesForRack(rack.id);
            }
        }
    };

    const loadUbicacionesForRack = async (rackId) => {
        setLoadingUbicaciones(true);
        try {
            const res = await listarUbicacionesPorRack(rackId);
            setUbicacionesForSelector(res.data || []);
        } catch (error) {
            console.error(error);
        } finally {
            setLoadingUbicaciones(false);
        }
    };

    const handleSelectRack = async (rack) => {
        setSelectedRackForSelector(rack);
        await loadUbicacionesForRack(rack.id);
    };

    const handleSelectCelda = (nivel, columna, profundidadIndex) => {
        const newDetalles = [...formData.detalles];
        newDetalles[currentDetailIndex] = {
            ...newDetalles[currentDetailIndex],
            rackId: selectedRackForSelector.id,
            nivel, columna, profundidadIndex
        };
        setFormData({ ...formData, detalles: newDetalles });
        closeLocationSelector();
    };

    const closeLocationSelector = () => {
        setLocationSelectorOpen(false);
        setSelectedRackForSelector(null);
        setCurrentDetailIndex(null);
    };

    return (
        <div className="module-container">
            <header className="module-header">
                <div className="header-title">
                    <h1>Inventario de Lotes</h1>
                    <p>Filtra por factura, proveedor o medicamento</p>
                </div>
                <div className="header-actions-row">
                    <div className="stock-filter-group">
                        <button className={`btn-filter ${filtroStock === 'todos' ? 'active' : ''}`} onClick={() => setFiltroStock('todos')}>Todos</button>
                        <button className={`btn-filter ${filtroStock === 'stock' ? 'active' : ''}`} onClick={() => setFiltroStock('stock')}>En Stock</button>
                        <button className={`btn-filter ${filtroStock === 'agotado' ? 'active' : ''}`} onClick={() => setFiltroStock('agotado')}>Agotados</button>
                    </div>
                    <div className="search-box">
                        <span className="search-icon">🔍</span>
                        <input type="text" placeholder="Buscar factura..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
                    </div>
                    {isAdmin && <button className="btn-primary-compact" onClick={handleNuevo}>＋ Registrar Entrada</button>}
                </div>
            </header>

            {message.text && (
                <div className={`alert-banner ${message.type}`}>
                    {message.text}
                    <button onClick={() => setMessage({ text: '', type: '' })}>×</button>
                </div>
            )}

            <div className="table-card">
                <div className="table-responsive">
                    <table className="custom-table">
                        <thead>
                        <tr>
                            <th>Imagen</th>
                            <th>Factura Ref.</th>
                            <th>Proveedor</th>
                            <th>Medicamentos</th>
                            <th>Vencimiento</th>
                            <th>Estado</th>
                            <th className="text-center">Acciones</th>
                        </tr>
                        </thead>
                        <tbody>
                        {loading ? (
                            Array.from({ length: 5 }).map((_, idx) => (
                                <tr key={idx} className="skeleton-row">
                                    {Array.from({ length: 7 }).map((_, i) => <td key={i}><div className="skeleton-cell" /></td>)}
                                </tr>
                            ))
                        ) : (
                            paginatedLotes.map((l, idx) => {
                                const totalStock = l.detalles?.reduce((acc, d) => acc + (d.cantidad || 0), 0) || 0;
                                const tieneStock = totalStock > 0;
                                const vencido = new Date(l.fechaVencimiento) < new Date();

                                return (
                                    <tr key={l.id} className={`fade-in-row ${!l.activo ? 'row-inactive' : ''}`} style={{ animationDelay: `${idx * 0.05}s` }}>
                                        {/* Columna de Imagen (Primer item) */}
                                        <td>
                                            {(() => {
                                                const med = medicamentos.find(m => m.id === l.detalles?.[0]?.medicamentoId);
                                                return med?.imagen ? (
                                                    <img
                                                        src={`http://localhost:8080/${med.imagen.replace(/\\/g, "/")}`}
                                                        alt="med"
                                                        style={{ width: '40px', height: '40px', objectFit: 'cover', borderRadius: '6px' }}
                                                    />
                                                ) : '—';
                                            })()}
                                        </td>
                                        <td className="font-bold">{l.factura}</td>
                                        <td>{proveedores.find(p => p.id === l.proveedorId)?.nombre || '—'}</td>
                                        {/* Columna de Medicamentos (Opción PRO) */}
                                        <td>
                                            <div className="items-chip-container">
                                                {l.detalles?.map((det, i) => {
                                                    const med = medicamentos.find(m => m.id === det.medicamentoId);
                                                    return (
                                                        <div key={i} className="med-chip-with-img">
                                                            {med?.imagen && (
                                                                <img src={`http://localhost:8080/${med.imagen.replace(/\\/g, "/")}`} alt="thumb" />
                                                            )}
                                                            <span>{med?.nombre || 'S/N'} <small>x{det.cantidad}</small></span>
                                                        </div>
                                                    );
                                                })}
                                            </div>
                                        </td>
                                        <td>
                                            <span className="vencimiento-wrapper">
                                                {l.fechaVencimiento}
                                                {vencido && <span className="vencido-badge">Vencido</span>}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`status-pill ${l.activo ? (tieneStock ? 'active' : 'agotado') : 'inactive'}`}>
                                                {l.activo ? (tieneStock ? 'En Stock' : 'Agotado') : 'Inactivo'}
                                            </span>
                                        </td>
                                        <td className="text-center">
                                            <div className="action-buttons-group">
                                                <button className="btn-edit-icon" title="Imprimir" onClick={() => imprimirLote(l)}>📄</button>
                                                {isAdmin && l.activo && <button className="btn-delete-icon" onClick={() => handleDesactivar(l.id)}>🗑️</button>}
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })
                        )}
                        </tbody>
                    </table>
                </div>
                {!loading && lotesFiltrados.length > 0 && (
                    <div className="pagination-container">
                        <button className="pagination-btn" onClick={() => goToPage(currentPage - 1)} disabled={currentPage === 1}>← Anterior</button>
                        <div className="pagination-pages">{renderPageNumbers()}</div>
                        <button className="pagination-btn" onClick={() => goToPage(currentPage + 1)} disabled={currentPage === totalPages}>Siguiente →</button>
                    </div>
                )}
            </div>

            {/* DRAWER para nuevo lote */}
            {drawerOpen && (
                <div className="drawer-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="drawer-panel" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header-compact">
                            <h2>Nuevo Lote</h2>
                            <button className="close-btn-round" onClick={() => setDrawerOpen(false)}>×</button>
                        </div>
                        <form onSubmit={handleSubmit} className="drawer-body-scrollable">
                            <div className="field-group">
                                <label>Factura</label>
                                <input type="text" value={formData.factura} readOnly className="input-readonly" />
                            </div>
                            <div className="field-group">
                                <label>Proveedor *</label>
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
                                    <label>Vencimiento *</label>
                                    <input type="date" value={formData.fechaVencimiento} onChange={e => setFormData({...formData, fechaVencimiento: e.target.value})} required />
                                </div>
                            </div>

                            <h4 className="section-divider">Productos</h4>
                            {formData.detalles.map((det, index) => (
                                <div key={index} className="item-entry-row">
                                    <select
                                        className="flex-2"
                                        value={det.medicamentoId}
                                        onChange={e => {
                                            const newDet = [...formData.detalles];
                                            newDet[index].medicamentoId = e.target.value;
                                            setFormData({...formData, detalles: newDet});
                                        }}
                                        required
                                    >
                                        <option value="">Medicamento...</option>
                                        {medicamentos.map(m => <option key={m.id} value={m.id}>{m.nombre}</option>)}
                                    </select>
                                    <input
                                        type="number"
                                        className="flex-1"
                                        value={det.cantidad}
                                        onChange={e => {
                                            const newDet = [...formData.detalles];
                                            newDet[index].cantidad = parseInt(e.target.value) || 0;
                                            setFormData({...formData, detalles: newDet});
                                        }}
                                        min="1"
                                        required
                                    />
                                    <button
                                        type="button"
                                        className="btn-delete-small"
                                        onClick={() => setFormData({...formData, detalles: formData.detalles.filter((_, i) => i !== index)})}
                                    >
                                        ×
                                    </button>

                                    <div className="location-selector-button">
                                        <button type="button" className="btn-select-location" onClick={() => openLocationSelector(index)}>
                                            {det.rackId ? `📍 ${racks.find(r => r.id === det.rackId)?.nombre || '?'} N${det.nivel+1} C${det.columna+1}` : '📌 Ubicación'}
                                        </button>
                                    </div>
                                </div>
                            ))}
                            <button
                                type="button"
                                className="btn-add-item"
                                onClick={() => setFormData({
                                    ...formData,
                                    detalles: [...formData.detalles, { medicamentoId: '', cantidad: 1, rackId: '', nivel: 0, columna: 0, profundidadIndex: 0 }]
                                })}
                            >
                                + Añadir Medicamento
                            </button>

                            <div className="drawer-footer-fixed">
                                <button type="button" className="btn-cancel" onClick={() => setDrawerOpen(false)}>Cancelar</button>
                                <button type="submit" className="btn-save-final" disabled={loading}>
                                    {loading ? 'Guardando...' : 'Guardar Entrada'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Modal selector de ubicación */}
            {locationSelectorOpen && (
                <div className="drawer-overlay" onClick={closeLocationSelector}>
                    <div className="location-selector-modal" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Seleccionar ubicación</h3>
                            <button className="close-btn" onClick={closeLocationSelector}>×</button>
                        </div>
                        <div className="modal-body">
                            {!selectedRackForSelector ? (
                                <div className="racks-list-modal">
                                    {racks.map(rack => (
                                        <div key={rack.id} className="rack-card-modal" onClick={() => handleSelectRack(rack)}>
                                            <strong>{rack.nombre}</strong>
                                            <small>{rack.ancho}x{rack.alto}</small>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div>
                                    <div className="selected-rack-info">
                                        <strong>{selectedRackForSelector.nombre}</strong>
                                        <button onClick={() => setSelectedRackForSelector(null)}>Cambiar estante</button>
                                    </div>
                                    {loadingUbicaciones ? (
                                        <div className="loading-spinner">Cargando...</div>
                                    ) : (
                                        <RackVisualization
                                            rack={selectedRackForSelector}
                                            ubicaciones={ubicacionesForSelector}
                                            onSeleccionarCelda={handleSelectCelda}
                                            seleccionActual={null}
                                            modoMovimiento={false}
                                            origenMovimiento={null}
                                            onIniciarMovimiento={() => {}}
                                        />
                                    )}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}