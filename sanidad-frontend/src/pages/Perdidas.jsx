import React, { useState, useEffect } from 'react';
import {
    getProductosVencidos,
    getProductosInmoviles,
    getInconsistenciasStock,
    getResumenPerdidas
} from '../services/perdidas';
import { AlertTriangle, Package, XCircle, CheckCircle, BarChart3 } from 'lucide-react';
import './Perdidas.css';

export default function Perdidas() {
    const [vencidos, setVencidos] = useState([]);
    const [inmoviles, setInmoviles] = useState([]);
    const [inconsistencias, setInconsistencias] = useState([]);
    const [resumen, setResumen] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeTab, setActiveTab] = useState('vencidos');

    useEffect(() => {
        cargarDatos();
    }, []);

    const cargarDatos = async () => {
        setLoading(true);
        try {
            const [resVencidos, resInmoviles, resInconsistencias, resResumen] = await Promise.all([
                getProductosVencidos(),
                getProductosInmoviles(),
                getInconsistenciasStock(),
                getResumenPerdidas()
            ]);
            setVencidos(resVencidos.data || []);
            setInmoviles(resInmoviles.data || []);
            setInconsistencias(resInconsistencias.data || []);
            setResumen(resResumen.data);
            setError(null);
        } catch (err) {
            setError('Error al sincronizar el análisis de pérdidas operativas.');
        } finally {
            setLoading(false);
        }
    };

    const formatCurrency = (value) => {
        return new Intl.NumberFormat('es-NI', {
            style: 'currency',
            currency: 'NIO',
        }).format(value || 0).replace('NIO', 'C$');
    };

    if (loading) {
        return (
            <div className="perdidas-section">
                <div className="p-loading-container">
                    <div className="p-spinner"></div>
                    <p>Analizando inventario y discrepancias...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="perdidas-section">
                <div className="p-loading-container">
                    <XCircle size={40} color="#ef4444" />
                    <p>{error}</p>
                    <button onClick={cargarDatos} className="p-tab-btn active" style={{marginTop: '15px'}}>
                        Reintentar
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="perdidas-section">
            <header className="perdidas-header">
                <h1>Control de Pérdidas y Análisis Operativo</h1>
                <p>Visualización centralizada de mermas, stock inmovilizado y discrepancias de inventario.</p>
            </header>

            {/* Panel de Indicadores (KPIs) con prefijo p- para evitar colisión */}
            <div className="p-kpi-grid">
                <div className="p-kpi-card">
                    <div className="p-kpi-icon icon-vencido">
                        <AlertTriangle size={26} />
                    </div>
                    <div className="p-kpi-info">
                        <h3>Pérdidas por Vencimiento</h3>
                        <p className="p-kpi-value">{formatCurrency(resumen?.totalPerdidasVencimiento)}</p>
                        <span className="kpi-sub">{resumen?.cantidadProductosVencidos} productos vencidos</span>
                    </div>
                </div>

                <div className="p-kpi-card">
                    <div className="p-kpi-icon icon-inmovil">
                        <Package size={26} />
                    </div>
                    <div className="p-kpi-info">
                        <h3>Inventario Inmovilizado</h3>
                        <p className="p-kpi-value">{formatCurrency(resumen?.totalInmovilizado)}</p>
                        <span className="kpi-sub">{resumen?.cantidadProductosInmoviles} SKU sin rotación</span>
                    </div>
                </div>

                <div className="p-kpi-card">
                    <div className="p-kpi-icon icon-stock">
                        <BarChart3 size={26} />
                    </div>
                    <div className="p-kpi-info">
                        <h3>Inconsistencias</h3>
                        <p className="p-kpi-value">{resumen?.cantidadInconsistencias}</p>
                        <span className="kpi-sub">Diferencias detectadas en stock</span>
                    </div>
                </div>
            </div>

            {/* Navegación por Chips */}
            <div className="p-tabs-container">
                <button
                    className={`p-tab-btn ${activeTab === 'vencidos' ? 'active' : ''}`}
                    onClick={() => setActiveTab('vencidos')}
                >
                    Productos Vencidos
                </button>
                <button
                    className={`p-tab-btn ${activeTab === 'inmoviles' ? 'active' : ''}`}
                    onClick={() => setActiveTab('inmoviles')}
                >
                    Sin Rotación
                </button>
                <button
                    className={`p-tab-btn ${activeTab === 'inconsistencias' ? 'active' : ''}`}
                    onClick={() => setActiveTab('inconsistencias')}
                >
                    Inconsistencias
                </button>
            </div>

            {/* Contenido Dinámico */}
            <div className="p-table-wrapper">
                {activeTab === 'vencidos' && (
                    vencidos.length === 0 ? (
                        <EmptyState message="No se registran productos vencidos con stock." />
                    ) : (
                        <table className="p-table">
                            <thead>
                            <tr>
                                <th>Lote</th>
                                <th>Medicamento</th>
                                <th>Vencimiento</th>
                                <th>Cantidad</th>
                                <th>Valor Perdido</th>
                            </tr>
                            </thead>
                            <tbody>
                            {vencidos.map(v => (
                                <tr key={`${v.loteId}-${v.medicamentoId}`}>
                                    <td><span className="badge-lote">{v.numeroLote}</span></td>
                                    <td><strong>{v.medicamentoNombre}</strong></td>
                                    <td>{new Date(v.fechaVencimiento).toLocaleDateString()}</td>
                                    <td>{v.cantidadVencida} u.</td>
                                    <td className="price-text text-error">{formatCurrency(v.valorPerdido)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    )
                )}

                {activeTab === 'inmoviles' && (
                    inmoviles.length === 0 ? (
                        <EmptyState message="Todos los productos presentan rotación activa." />
                    ) : (
                        <table className="p-table">
                            <thead>
                            <tr>
                                <th>Medicamento</th>
                                <th>Stock Actual</th>
                                <th>Días sin Movimiento</th>
                                <th>Valor Inmovilizado</th>
                            </tr>
                            </thead>
                            <tbody>
                            {inmoviles.map(p => (
                                <tr key={p.medicamentoId}>
                                    <td><strong>{p.medicamentoNombre}</strong></td>
                                    <td>{p.stockActual} u.</td>
                                    <td>{p.diasSinMovimiento} días</td>
                                    <td className="price-text">{formatCurrency(p.valorInmovilizado)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    )
                )}

                {activeTab === 'inconsistencias' && (
                    inconsistencias.length === 0 ? (
                        <EmptyState message="Integridad de stock verificada." />
                    ) : (
                        <table className="p-table">
                            <thead>
                            <tr>
                                <th>Medicamento</th>
                                <th>Stock Lote</th>
                                <th>Stock Ubicación</th>
                                <th>Diferencia</th>
                            </tr>
                            </thead>
                            <tbody>
                            {inconsistencias.map(inc => (
                                <tr key={inc.loteDetalleId}>
                                    <td><strong>{inc.medicamentoNombre}</strong></td>
                                    <td>{inc.cantidadLote}</td>
                                    <td>{inc.cantidadUbicaciones}</td>
                                    <td className={inc.diferencia > 0 ? 'text-warning' : 'text-error'}>
                                        {inc.diferencia > 0 ? `+${inc.diferencia}` : inc.diferencia}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    )
                )}
            </div>
        </div>
    );
}

function EmptyState({ message }) {
    return (
        <div style={{ padding: '60px', textAlign: 'center', color: '#64748b' }}>
            <CheckCircle size={48} color="#10b981" style={{ marginBottom: '16px' }} />
            <p>{message}</p>
        </div>
    );
}