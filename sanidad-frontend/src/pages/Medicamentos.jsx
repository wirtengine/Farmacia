import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import {
    listarMedicamentos,
    crearMedicamento,
    actualizarMedicamento,
    desactivarMedicamento,
    reactivarMedicamento,
    subirImagenMedicamento
} from '../services/medicamentos';

import './Medicamentos.css';

import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

import {
    alertaConfirmacion,
    alertaExito,
    alertaError
} from '../alertas';

export default function Medicamentos() {

    const { user } = useAuth();

    const [medicamentos, setMedicamentos] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [loading, setLoading] = useState(false);

    const [drawerOpen, setDrawerOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);

    const [currentId, setCurrentId] = useState(null);

    const [imagenFile, setImagenFile] = useState(null);

    const [originalData, setOriginalData] = useState(null);

    // PAGINACIÓN
    const [currentPage, setCurrentPage] = useState(1);
    const rowsPerPage = 15;

    const [formData, setFormData] = useState({
        registroSanitario: '',
        nombre: '',
        presentacion: '',
        via: '',
        fabricante: '',
        tipoVenta: 'LIBRE',
        precioUnitario: '',
        receta: false,
        activo: true
    });

    // =========================
    // DEBOUNCE
    // =========================

    useEffect(() => {

        const timer = setTimeout(() => {
            setDebouncedSearch(searchTerm);
        }, 300);

        return () => clearTimeout(timer);

    }, [searchTerm]);

    // =========================
    // CARGA INICIAL
    // =========================

    useEffect(() => {
        cargarMedicamentos();
    }, []);

    // =========================
    // CARGAR MEDICAMENTOS
    // =========================

    const cargarMedicamentos = async () => {

        setLoading(true);

        try {

            const response = await listarMedicamentos();

            const sorted = response.data.sort((a, b) => b.id - a.id);

            setMedicamentos(sorted);

        } catch (error) {

            alertaError('Error al conectar con el servidor');

        } finally {

            setLoading(false);

        }
    };

    // =========================
    // GENERAR PDF
    // =========================

    const generarPDF = () => {

        const doc = new jsPDF();

        const fechaActual = new Date();

        const fechaStr = fechaActual.toLocaleDateString('es-ES', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });

        const horaStr = fechaActual.toLocaleTimeString('es-ES');

        const nombreUsuario =
            user?.nombre ||
            user?.username ||
            user?.sub ||
            'Usuario del Sistema';

        doc.setFontSize(18);
        doc.setTextColor(41, 128, 185);

        doc.text(
            'Catálogo de Medicamentos - Sanidad App',
            14,
            20
        );

        doc.setFontSize(10);

        doc.setTextColor(100);

        doc.text(
            `Generado: ${fechaStr} - ${horaStr}`,
            14,
            28
        );

        doc.text(
            `Usuario: ${nombreUsuario}`,
            14,
            33
        );

        const columnas = [
            'Medicamento',
            'Reg. Sanitario',
            'Fabricante',
            'Presentación',
            'Vía',
            'Precio (C$)',
            'Estado'
        ];

        const filas = medicamentosFiltrados.map(m => [
            m.nombre,
            m.registroSanitario,
            m.fabricante || 'N/A',
            m.presentacion,
            m.via,
            parseFloat(m.precioUnitario).toFixed(2),
            m.activo ? 'Activo' : 'Inactivo'
        ]);

        autoTable(doc, {
            head: [columnas],
            body: filas,
            startY: 40,
            theme: 'striped',

            headStyles: {
                fillColor: [41, 128, 185],
                textColor: 255,
                fontStyle: 'bold',
                halign: 'center'
            },

            bodyStyles: {
                fontSize: 9,
                cellPadding: 3
            },

            columnStyles: {
                5: { halign: 'right' },
                6: { halign: 'center' }
            },

            margin: {
                top: 40,
                bottom: 30
            },

            didDrawPage: (data) => {

                const pageCount = doc.getNumberOfPages();

                doc.setFontSize(8);

                doc.setTextColor(150);

                doc.text(
                    `Página ${data.pageNumber} de ${pageCount}`,
                    doc.internal.pageSize.getWidth() - 30,
                    doc.internal.pageSize.getHeight() - 10
                );

                doc.text(
                    `Total de medicamentos: ${medicamentosFiltrados.length}`,
                    14,
                    doc.internal.pageSize.getHeight() - 10
                );
            }
        });

        doc.save(
            `Reporte_Medicamentos_${fechaActual.toISOString().slice(0, 10)}.pdf`
        );
    };

    // =========================
    // FILTROS
    // =========================

    const medicamentosFiltrados = useMemo(() => {

        return medicamentos.filter(m => {

            const esActivo =
                m.activo === true ||
                String(m.activo) === 'true';

            if (user?.rol !== 'ADMIN' && !esActivo) {
                return false;
            }

            const term = debouncedSearch.toLowerCase();

            return (
                m.nombre.toLowerCase().includes(term) ||
                m.fabricante?.toLowerCase().includes(term) ||
                m.registroSanitario.toLowerCase().includes(term)
            );
        });

    }, [medicamentos, debouncedSearch, user]);

    // =========================
    // PAGINACIÓN
    // =========================

    useEffect(() => {
        setCurrentPage(1);
    }, [debouncedSearch]);

    const totalPages = Math.ceil(
        medicamentosFiltrados.length / rowsPerPage
    );

    const paginatedMedicamentos = useMemo(() => {

        const startIndex =
            (currentPage - 1) * rowsPerPage;

        return medicamentosFiltrados.slice(
            startIndex,
            startIndex + rowsPerPage
        );

    }, [medicamentosFiltrados, currentPage]);

    const goToPage = (page) => {

        if (page >= 1 && page <= totalPages) {
            setCurrentPage(page);
        }
    };

    const renderPageNumbers = () => {

        const pages = [];

        const maxVisible = 5;

        const sidePages = Math.floor(maxVisible / 2);

        let startPage = Math.max(
            1,
            currentPage - sidePages
        );

        let endPage = Math.min(
            totalPages,
            currentPage + sidePages
        );

        if (currentPage - sidePages <= 1) {
            endPage = Math.min(totalPages, maxVisible);
        }

        if (currentPage + sidePages >= totalPages) {
            startPage = Math.max(
                1,
                totalPages - maxVisible + 1
            );
        }

        for (let i = startPage; i <= endPage; i++) {

            pages.push(
                <button
                    key={i}
                    className={`pagination-number ${
                        currentPage === i ? 'active' : ''
                    }`}
                    onClick={() => goToPage(i)}
                >
                    {i}
                </button>
            );
        }

        return pages;
    };

    // =========================
    // NUEVO
    // =========================

    const handleNuevo = () => {

        setEditMode(false);

        setFormData({
            registroSanitario: '',
            nombre: '',
            presentacion: '',
            via: '',
            fabricante: '',
            tipoVenta: 'LIBRE',
            precioUnitario: '',
            receta: false,
            activo: true
        });

        setOriginalData(null);

        setImagenFile(null);

        setDrawerOpen(true);
    };

    // =========================
    // EDITAR
    // =========================

    const handleEditar = (med) => {

        setEditMode(true);

        setCurrentId(med.id);

        const data = {
            ...med,
            activo:
                med.activo === true ||
                String(med.activo) === 'true'
        };

        setFormData(data);

        setOriginalData(data);

        setImagenFile(null);

        setDrawerOpen(true);
    };

    // =========================
    // CHANGE INPUTS
    // =========================

    const handleChange = (e) => {

        const {
            name,
            value,
            type,
            checked
        } = e.target;

        let finalValue =
            type === 'checkbox'
                ? checked
                : value;

        if (name === 'activo') {
            finalValue = value === 'true';
        }

        setFormData(prev => ({
            ...prev,
            [name]: finalValue
        }));
    };

    // =========================
    // SUBMIT
    // =========================

    const handleSubmit = async (e) => {

        e.preventDefault();

        setLoading(true);

        const payload = {
            ...formData,
            precioUnitario:
                formData.precioUnitario
                    ? Number(formData.precioUnitario)
                    : 0
        };


        const sinCambios =
            editMode &&
            originalData &&
            !imagenFile &&

            String(payload.nombre) ===
            String(originalData.nombre) &&

            String(payload.presentacion) ===
            String(originalData.presentacion) &&

            String(payload.via) ===
            String(originalData.via) &&

            String(payload.fabricante) ===
            String(originalData.fabricante) &&

            String(payload.tipoVenta) ===
            String(originalData.tipoVenta) &&

            Number(payload.precioUnitario) ===
            Number(originalData.precioUnitario) &&

            Boolean(payload.receta) ===
            Boolean(originalData.receta) &&

            Boolean(payload.activo) ===
            Boolean(originalData.activo);

        if (sinCambios) {

            setLoading(false);

            return;
        }

        try {

            let targetId;

            // =========================
            // EDITAR
            // =========================

            if (editMode) {

                await actualizarMedicamento(
                    currentId,
                    payload
                );

                targetId = currentId;

                alertaExito(
                    'Medicamento actualizado correctamente'
                );

            } else {

                // =========================
                // CREAR
                // =========================

                const response =
                    await crearMedicamento(payload);

                targetId = response.data.id;

                alertaExito(
                    'Medicamento registrado correctamente'
                );
            }

            // =========================
            // SUBIR IMAGEN
            // =========================

            if (imagenFile && targetId) {

                try {

                    await subirImagenMedicamento(
                        targetId,
                        imagenFile
                    );

                } catch (imgError) {

                    console.error(
                        'Error al subir la imagen:',
                        imgError
                    );

                    alertaError(
                        'Guardado, pero hubo un problema con la imagen'
                    );
                }
            }

            setDrawerOpen(false);

            setImagenFile(null);

            await cargarMedicamentos();

        } catch (error) {

            const errorMsg =
                error.response?.data?.message ||
                'Error en el proceso';

            alertaError(errorMsg);

        } finally {

            setLoading(false);

        }
    };

    // =========================
    // DESACTIVAR
    // =========================

    const handleDesactivar = async (id) => {

        const result =
            await alertaConfirmacion({
                titulo: '¿Desactivar medicamento?',
                texto: 'El medicamento dejará de estar disponible.',
                confirmar: 'Sí, desactivar',
                cancelar: 'Cancelar',
                icono: 'warning'
            });

        if (!result.isConfirmed) return;

        try {

            await desactivarMedicamento(id);

            alertaExito(
                'Medicamento desactivado'
            );

            cargarMedicamentos();

        } catch (error) {

            alertaError(
                'Error al desactivar'
            );
        }
    };

    // =========================
    // REACTIVAR
    // =========================

    const handleReactivar = async (id) => {

        const result =
            await alertaConfirmacion({
                titulo: '¿Reactivar medicamento?',
                texto: 'El medicamento volverá a estar disponible.',
                confirmar: 'Sí, reactivar',
                cancelar: 'Cancelar',
                icono: 'question'
            });

        if (!result.isConfirmed) return;

        try {

            await reactivarMedicamento(id);

            alertaExito(
                'Medicamento reactivado'
            );

            cargarMedicamentos();

        } catch (error) {

            alertaError(
                'Error al reactivar'
            );
        }
    };

    return (
        <div className="module-container">
            <header className="module-header">
                <div className="header-title">
                    <h1>Catálogo Farmacéutico</h1>
                    <p>Gestión de Medicamentos e Insumos</p>
                </div>

                <div className="header-actions-row">
                    <div className="search-box">
                        <span className="search-icon">🔍</span>
                        <input
                            type="text"
                            placeholder="Buscar medicamento..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    <button className="btn-print" onClick={generarPDF}>
                        🖨️ PDF
                    </button>

                    {user?.rol === 'ADMIN' && (
                        <button className="btn-primary-compact" onClick={handleNuevo}>
                            <span>+</span> Nuevo
                        </button>
                    )}
                </div>
            </header>

            <div className="table-card">
                <div className="table-responsive">
                    <table className="custom-table">
                        <thead>
                        <tr>
                            <th>Imagen</th>
                            <th>Medicamento</th>
                            <th>Registro</th>
                            <th>Fabricante</th>
                            <th>Presentación</th>
                            <th>Vía</th>
                            <th>Precio</th>
                            <th>Estado</th>
                            {user?.rol === 'ADMIN' && <th>Acciones</th>}
                        </tr>
                        </thead>

                        <tbody>
                        {loading ? (
                            Array.from({ length: 5 }).map((_, index) => (
                                <tr key={index} className="skeleton-row">
                                    {Array.from({ length: 9 }).map((_, i) => (
                                        <td key={i}>
                                            <div className="skeleton-cell" />
                                        </td>
                                    ))}
                                </tr>
                            ))
                        ) : (
                            paginatedMedicamentos.map((m, idx) => (
                                <tr
                                    key={m.id}
                                    className="fade-in-row"
                                    style={{ animationDelay: `${idx * 0.05}s` }}
                                >
                                    <td>
                                        {m.imagen ? (
                                            <img
                                                src={`http://localhost:8080/${m.imagen.replace(/\\/g, '/')}`}
                                                alt="med"
                                                style={{
                                                    width: '40px',
                                                    height: '40px',
                                                    objectFit: 'cover',
                                                    borderRadius: '6px'
                                                }}
                                            />
                                        ) : (
                                            '—'
                                        )}
                                    </td>

                                    <td className="font-bold">{m.nombre}</td>
                                    <td className="text-muted">{m.registroSanitario}</td>
                                    <td>{m.fabricante || '-'}</td>
                                    <td><span className="badge-gray">{m.presentacion}</span></td>
                                    <td><span className="badge-blue">{m.via}</span></td>
                                    <td className="price-text">
                                        C$ {parseFloat(m.precioUnitario).toFixed(2)}
                                    </td>
                                    <td>
                                            <span className={`status-pill ${m.activo ? 'active' : 'inactive'}`}>
                                                {m.activo ? 'Activo' : 'Inactivo'}
                                            </span>
                                    </td>

                                    {user?.rol === 'ADMIN' && (
                                        <td>
                                            <div className="action-buttons-group">
                                                <button
                                                    className="btn-edit-icon"
                                                    onClick={() => handleEditar(m)}
                                                >
                                                    ✏️
                                                </button>

                                                {m.activo ? (
                                                    <button
                                                        className="btn-delete-icon"
                                                        onClick={() => handleDesactivar(m.id)}
                                                    >
                                                        🗑️
                                                    </button>
                                                ) : (
                                                    <button
                                                        className="btn-restore-icon"
                                                        onClick={() => handleReactivar(m.id)}
                                                    >
                                                        ↩️
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    )}
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>

                <div className="pagination-container">
                    <button
                        className="pagination-btn"
                        onClick={() => goToPage(currentPage - 1)}
                        disabled={currentPage === 1}
                    >
                        ← Anterior
                    </button>

                    <div className="pagination-pages">
                        {renderPageNumbers()}
                    </div>

                    <button
                        className="pagination-btn"
                        onClick={() => goToPage(currentPage + 1)}
                        disabled={currentPage === totalPages}
                    >
                        Siguiente →
                    </button>
                </div>
            </div>

            {drawerOpen && user?.rol === 'ADMIN' && (
                <div className="drawer-overlay" onClick={() => setDrawerOpen(false)}>
                    <div className="drawer-panel" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header-compact">
                            <h2>{editMode ? 'Editar' : 'Nuevo'} Medicamento</h2>
                            <button
                                className="close-btn-round"
                                onClick={() => setDrawerOpen(false)}
                            >
                                ×
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="drawer-body-scrollable">
                            <div className="form-content-inner">
                                <h4 className="section-divider">Información General</h4>

                                <div className="field-group">
                                    <label>Nombre Comercial *</label>
                                    <input
                                        type="text"
                                        name="nombre"
                                        value={formData.nombre}
                                        onChange={handleChange}
                                        required
                                    />
                                </div>

                                <div className="field-grid-2">
                                    <div className="field-group">
                                        <label>Reg. Sanitario *</label>
                                        <input
                                            type="text"
                                            name="registroSanitario"
                                            value={formData.registroSanitario}
                                            onChange={handleChange}
                                            required
                                            disabled={editMode}
                                        />
                                    </div>

                                    <div className="field-group">
                                        <label>Fabricante *</label>
                                        <input
                                            type="text"
                                            name="fabricante"
                                            value={formData.fabricante}
                                            onChange={handleChange}
                                            required
                                        />
                                    </div>
                                </div>

                                <h4 className="section-divider">Detalles Técnicos</h4>

                                <div className="field-grid-2">
                                    <div className="field-group">
                                        <label>Presentación *</label>
                                        <select
                                            name="presentacion"
                                            value={formData.presentacion}
                                            onChange={handleChange}
                                            required
                                        >
                                            <option value="">Seleccione...</option>
                                            <option value="Tableta">Tableta</option>
                                            <option value="Cápsula">Cápsula</option>
                                            <option value="Jarabe">Jarabe</option>
                                            <option value="Inyección">Inyección</option>
                                            <option value="Crema">Crema</option>
                                        </select>
                                    </div>

                                    <div className="field-group">
                                        <label>Vía de Admón. *</label>
                                        <select
                                            name="via"
                                            value={formData.via}
                                            onChange={handleChange}
                                            required
                                        >
                                            <option value="">Seleccione...</option>
                                            <option value="ORAL">Oral</option>
                                            <option value="TOPICA">Tópica</option>
                                            <option value="PARENTERAL">Parenteral</option>
                                        </select>
                                    </div>
                                </div>

                                <div className="field-grid-2">
                                    <div className="field-group">
                                        <label>Tipo de Venta *</label>
                                        <select
                                            name="tipoVenta"
                                            value={formData.tipoVenta}
                                            onChange={handleChange}
                                            required
                                        >
                                            <option value="LIBRE">Libre</option>
                                            <option value="CONTROLADO">Controlado</option>
                                        </select>
                                    </div>

                                    <div className="field-group">
                                        <label>Precio (C$) *</label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            name="precioUnitario"
                                            value={formData.precioUnitario}
                                            onChange={handleChange}
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="field-group">
                                    <label>Imagen del Medicamento (Max 2MB)</label>
                                    <input
                                        type="file"
                                        accept="image/*"
                                        onChange={(e) => setImagenFile(e.target.files[0])}
                                    />
                                </div>

                                <div className="receta-warning-card">
                                    <input
                                        type="checkbox"
                                        id="receta"
                                        name="receta"
                                        checked={formData.receta}
                                        onChange={handleChange}
                                    />
                                    <label htmlFor="receta">
                                        Requiere receta médica obligatoria
                                    </label>
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
                                    {loading ? '...' : 'Guardar'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}