import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
    getRecommendations,
    acceptRecommendation,
    dismissRecommendation,
    generateRecommendations
} from '../services/recommendations';
import {
    TrendingUp,
    ShoppingCart,
    AlertTriangle,
    CheckCircle,
    XCircle,
    RefreshCw,
    Filter,
    Zap
} from 'lucide-react';
import './Recommendations.css';

// Normaliza el estado y unifica ACCEPTED y RESOLVED para filtros/estadísticas
const normalizeStatus = (status) => {
    if (!status) return 'UNKNOWN';
    const s = status.toString().toUpperCase().trim();
    if (s === 'ACKNOWLEDGED' || s === 'RESOLVED') return 'ACCEPTED';
    if (s === 'DISCARDED') return 'DISMISSED';
    return s;
};

// Obtiene el texto legible para el estado en la tabla
const getStatusDisplay = (status) => {
    const s = status?.toString().toUpperCase().trim();
    if (s === 'PENDING') return 'Pendiente';
    if (s === 'ACCEPTED') return 'Aceptada';
    if (s === 'RESOLVED') return 'Resuelta';
    if (s === 'DISMISSED' || s === 'DISCARDED') return 'Descartada';
    return s;
};

const Recommendations = () => {
    const [recommendations, setRecommendations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [lastUpdated, setLastUpdated] = useState(new Date());
    const [statusFilter, setStatusFilter] = useState('PENDING');

    const fetchRecommendations = useCallback(async () => {
        setLoading(true);
        try {
            const res = await getRecommendations();
            setRecommendations(res.data || []);
            setLastUpdated(new Date());
            setError(null);
        } catch (err) {
            console.error('Error fetching recommendations:', err);
            setError('Error de conexión con el motor de recomendaciones.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchRecommendations();
    }, [fetchRecommendations]);

    // Estadísticas basadas en el estado normalizado
    const stats = useMemo(() => {
        const totals = { total: recommendations.length, pending: 0, accepted: 0, dismissed: 0 };
        recommendations.forEach(rec => {
            const norm = normalizeStatus(rec.status);
            if (norm === 'PENDING') totals.pending++;
            else if (norm === 'ACCEPTED') totals.accepted++;
            else if (norm === 'DISMISSED') totals.dismissed++;
        });
        return totals;
    }, [recommendations]);

    // Filtrado robusto
    const filteredRecs = useMemo(() => {
        if (statusFilter === 'ALL') return recommendations;
        const filterNorm = statusFilter.toUpperCase().trim();
        return recommendations.filter(rec => normalizeStatus(rec.status) === filterNorm);
    }, [recommendations, statusFilter]);

    const handleAccept = async (id) => {
        try {
            await acceptRecommendation(id);
            setRecommendations(prev =>
                prev.map(rec => rec.id === id ? { ...rec, status: 'ACCEPTED' } : rec)
            );
        } catch (err) {
            setError('No se pudo aceptar la recomendación.');
        }
    };

    const handleDismiss = async (id) => {
        try {
            await dismissRecommendation(id);
            setRecommendations(prev =>
                prev.map(rec => rec.id === id ? { ...rec, status: 'DISMISSED' } : rec)
            );
        } catch (err) {
            setError('No se pudo descartar la recomendación.');
        }
    };

    const handleGenerate = async () => {
        setLoading(true);
        try {
            await generateRecommendations();
            await fetchRecommendations();
        } catch (err) {
            setError('Error al generar nuevas recomendaciones.');
            setLoading(false);
        }
    };

    const getPriorityLabel = (priority) => {
        const map = { 'HIGH': 'Alta', 'MEDIUM': 'Media', 'LOW': 'Baja' };
        return map[priority?.toUpperCase()] || priority;
    };

    const getTypeIcon = (type) => {
        switch (type?.toUpperCase()) {
            case 'PURCHASE_SUGGESTION': return <ShoppingCart size={18} />;
            case 'AVOID_RESTOCK': return <AlertTriangle size={18} />;
            case 'PRIORITIZE_SALE': return <TrendingUp size={18} />;
            default: return <Zap size={18} />;
        }
    };

    if (loading && recommendations.length === 0) {
        return (
            <div className="recommendations-dashboard loading-center">
                <div className="spinner"></div>
                <p>Analizando inventario...</p>
            </div>
        );
    }

    return (
        <div className="recommendations-dashboard">
            <header className="recommendations-header">
                <div className="header-content">
                    <h1>Motor de Recomendaciones</h1>
                    <p>Inteligencia de inventario • {lastUpdated.toLocaleTimeString()}</p>
                </div>
                <button className="btn-main-action" onClick={handleGenerate} disabled={loading}>
                    <RefreshCw size={18} className={loading ? "spin-icon" : "spin-icon-hover"} />
                    <span>{loading ? 'Analizando...' : 'Actualizar Análisis'}</span>
                </button>
            </header>

            <div className="recommendations-kpi-grid">
                <div className="kpi-panel" onClick={() => setStatusFilter('ALL')} style={{cursor:'pointer'}}>
                    <div className="kpi-info">
                        <h3>Total Analizado</h3>
                        <p>{stats.total}</p>
                    </div>
                    <div className="kpi-icon-wrapper info-bg"><TrendingUp /></div>
                </div>
                <div className="kpi-panel" onClick={() => setStatusFilter('PENDING')} style={{cursor: 'pointer'}}>
                    <div className="kpi-info">
                        <h3>Pendientes</h3>
                        <p className={stats.pending > 0 ? "text-warning" : ""}>{stats.pending}</p>
                    </div>
                    <div className="kpi-icon-wrapper warning-bg"><RefreshCw /></div>
                </div>
                <div className="kpi-panel" onClick={() => setStatusFilter('ACCEPTED')} style={{cursor: 'pointer'}}>
                    <div className="kpi-info">
                        <h3>Aplicadas</h3>
                        <p>{stats.accepted}</p>
                    </div>
                    <div className="kpi-icon-wrapper success-bg"><CheckCircle /></div>
                </div>
                <div className="kpi-panel" onClick={() => setStatusFilter('DISMISSED')} style={{cursor: 'pointer'}}>
                    <div className="kpi-info">
                        <h3>Descartadas</h3>
                        <p>{stats.dismissed}</p>
                    </div>
                    <div className="kpi-icon-wrapper danger-bg"><XCircle /></div>
                </div>
            </div>

            {error && (
                <div className="alert-toast error">
                    <AlertTriangle size={20} />
                    <span>{error}</span>
                    <button onClick={() => setError(null)} className="close-toast">×</button>
                </div>
            )}

            <div className="content-area table-wrapper">
                <div className="area-header">
                    <h3 className="area-title">Sugerencias Optimizadas</h3>
                    <div className="filter-container">
                        <Filter size={16} />
                        <select
                            value={statusFilter}
                            onChange={(e) => setStatusFilter(e.target.value)}
                            className="status-filter"
                        >
                            <option value="PENDING">Pendientes</option>
                            <option value="ACCEPTED">Aplicadas / Resueltas</option>
                            <option value="DISMISSED">Descartadas</option>
                            <option value="ALL">Ver Todas</option>
                        </select>
                    </div>
                </div>

                <div className="table-scroll-container">
                    {filteredRecs.length === 0 ? (
                        <div className="empty-state">
                            <CheckCircle size={48} className="text-muted-icon" />
                            <p>
                                {statusFilter === 'PENDING' && 'No hay recomendaciones pendientes.'}
                                {statusFilter === 'ACCEPTED' && 'No hay recomendaciones aplicadas o resueltas.'}
                                {statusFilter === 'DISMISSED' && 'No hay recomendaciones descartadas.'}
                                {statusFilter === 'ALL' && 'No hay recomendaciones registradas.'}
                            </p>
                        </div>
                    ) : (
                        <table className="recommendations-table">
                            <thead>
                            <tr>
                                <th>Tipo</th>
                                <th>Prioridad</th>
                                <th>Detalle</th>
                                <th>Sugerencia</th>
                                <th>Fecha</th>
                                <th>Estado</th>
                                <th className="text-right">Acciones</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredRecs.map(rec => {
                                const norm = normalizeStatus(rec.status);
                                const displayStatus = getStatusDisplay(rec.status);
                                return (
                                    <tr key={rec.id} className="table-row-hover">
                                        <td>
                                            <div className="type-cell">
                                                <span className={`icon-box ${rec.type?.toLowerCase()}`}>{getTypeIcon(rec.type)}</span>
                                                <span className="type-label">
                                                        {rec.type === 'PURCHASE_SUGGESTION' ? 'Compra' :
                                                            rec.type === 'AVOID_RESTOCK' ? 'Stock' : 'Venta'}
                                                    </span>
                                            </div>
                                        </td>
                                        <td>
                                                <span className={`priority-badge prio-${rec.priority?.toLowerCase()}`}>
                                                    {getPriorityLabel(rec.priority)}
                                                </span>
                                        </td>
                                        <td>
                                            <div className="detail-cell">
                                                <span className="title-text">{rec.title}</span>
                                                <span className="desc-text">{rec.description}</span>
                                            </div>
                                        </td>
                                        <td>
                                            <div className="action-bubble">{rec.suggestedAction}</div>
                                        </td>
                                        <td className="date-cell">
                                            {new Date(rec.createdAt).toLocaleDateString()}
                                        </td>
                                        <td>
                                                <span className={`status-badge status-${norm.toLowerCase()}`}>
                                                    {displayStatus}
                                                </span>
                                        </td>
                                        <td className="text-right">
                                            {norm === 'PENDING' ? (
                                                <div className="action-buttons">
                                                    <button className="btn-accept" onClick={() => handleAccept(rec.id)} title="Aceptar">
                                                        <CheckCircle size={20} />
                                                    </button>
                                                    <button className="btn-dismiss" onClick={() => handleDismiss(rec.id)} title="Ignorar">
                                                        <XCircle size={20} />
                                                    </button>
                                                </div>
                                            ) : (
                                                <div className={`status-icon-final ${norm.toLowerCase()}`}>
                                                    {norm === 'ACCEPTED' ? <CheckCircle size={18} /> : <XCircle size={18} />}
                                                </div>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Recommendations;