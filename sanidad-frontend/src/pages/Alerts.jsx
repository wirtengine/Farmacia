import React, { useState, useEffect } from 'react';
import { getAlerts, acknowledgeAlert, generateAlerts } from '../services/alerts';
import {
    Bell,
    RefreshCw,
    CheckCircle,
    Clock,
    ShieldAlert,
    Filter
} from 'lucide-react';
import './Alerts.css';

const Alerts = () => {
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [lastUpdated, setLastUpdated] = useState(new Date());
    const [statusFilter, setStatusFilter] = useState('PENDING');

    const fetchAlerts = async () => {
        setLoading(true);
        try {
            const res = await getAlerts();
            setAlerts(res.data);
            setLastUpdated(new Date());
            setError(null);
        } catch (err) {
            console.error('Error fetching alerts:', err);
            setError('Error de conexión con el centro de control.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAlerts();
    }, []);

    const handleAcknowledge = async (id) => {
        try {
            await acknowledgeAlert(id);
            fetchAlerts();
        } catch (err) {
            setError('No se pudo procesar la alerta.');
        }
    };

    const handleGenerate = async () => {
        try {
            await generateAlerts();
            fetchAlerts();
        } catch (err) {
            setError('Error al generar nuevas alertas.');
        }
    };

    // Estadísticas
    const stats = {
        total: alerts.length,
        pending: alerts.filter(a => a.status === 'PENDING').length,
        resolved: alerts.filter(a => a.status === 'ACKNOWLEDGED' || a.status === 'RESOLVED').length,
        high: alerts.filter(a => a.severity === 'ALTA' || a.severity === 'CRITICAL').length
    };

    // Filtrar según estado seleccionado
    const filteredAlerts = alerts.filter(alert => {
        if (statusFilter === 'ALL') return true;
        if (statusFilter === 'PENDING') return alert.status === 'PENDING';
        if (statusFilter === 'RESOLVED') return alert.status === 'ACKNOWLEDGED' || alert.status === 'RESOLVED';
        return true;
    });

    // Obtener texto del estado para mostrar
    const getStatusText = (status) => {
        if (status === 'PENDING') return 'Pendiente';
        if (status === 'ACKNOWLEDGED') return 'Atendida';
        if (status === 'RESOLVED') return 'Resuelta';
        return status;
    };

    if (loading && alerts.length === 0) {
        return (
            <div className="alerts-loading-state">
                <div className="spinner"></div>
                <p>Sincronizando con el servidor...</p>
            </div>
        );
    }

    return (
        <div className="alerts-dashboard">
            {/* HEADER */}
            <header className="alerts-header">
                <div className="header-content">
                    <h1>Centro de Control</h1>
                    <p>Última actualización: {lastUpdated.toLocaleTimeString()}</p>
                </div>

                <div className="header-actions">
                    <button className="btn-main-action" onClick={handleGenerate}>
                        <RefreshCw size={18} className="spin-icon-hover" />
                        <span>Sincronizar Alertas</span>
                    </button>
                </div>
            </header>

            {/* KPI GRID */}
            <div className="alerts-kpi-grid">
                <div className="kpi-panel">
                    <div className="kpi-info">
                        <h3>Total Alertas</h3>
                        <p>{stats.total}</p>
                    </div>
                    <div className="kpi-icon-wrapper info-bg">
                        <Bell size={24} />
                    </div>
                </div>

                <div className="kpi-panel">
                    <div className="kpi-info">
                        <h3>Pendientes</h3>
                        <p className={stats.pending > 0 ? "text-warning" : ""}>{stats.pending}</p>
                    </div>
                    <div className="kpi-icon-wrapper warning-bg">
                        <Clock size={24} />
                    </div>
                </div>

                <div className="kpi-panel">
                    <div className="kpi-info">
                        <h3>Críticas / Altas</h3>
                        <p className="text-critical">{stats.high}</p>
                        <small className="resolved-count">Resueltas: {stats.resolved}</small>
                    </div>
                    <div className="kpi-icon-wrapper critical-bg">
                        <ShieldAlert size={24} />
                    </div>
                </div>
            </div>

            {error && (
                <div className="alert-toast error">
                    <ShieldAlert size={20} />
                    <span>{error}</span>
                </div>
            )}

            {/* ÁREA CENTRAL: TABLA DE ALERTAS */}
            <div className="content-area table-wrapper">
                <div className="area-header">
                    <h3 className="area-title">Listado de Alertas del Sistema</h3>
                    <div className="filter-container">
                        <Filter size={18} />
                        <select
                            value={statusFilter}
                            onChange={(e) => setStatusFilter(e.target.value)}
                            className="status-filter"
                        >
                            <option value="PENDING">Pendientes</option>
                            <option value="RESOLVED">Atendidas / Resueltas</option>
                            <option value="ALL">Todas</option>
                        </select>
                    </div>
                </div>

                <div className="table-scroll-container">
                    {filteredAlerts.length === 0 ? (
                        <div className="empty-state">
                            <CheckCircle size={48} color="var(--success, #10b981)" />
                            <p>
                                {statusFilter === 'PENDING'
                                    ? 'No hay alertas pendientes en este momento.'
                                    : statusFilter === 'RESOLVED'
                                        ? 'No hay alertas atendidas o resueltas.'
                                        : 'No hay alertas activas en este momento.'}
                            </p>
                        </div>
                    ) : (
                        <table className="alerts-table">
                            <thead>
                            <tr>
                                <th>Alerta / Descripción</th>
                                <th>Severidad</th>
                                <th>Estado</th>
                                <th>Fecha</th>
                                <th className="text-right">Acción</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredAlerts.map((alert) => (
                                <tr key={alert.id} className="table-row-hover">
                                    <td>
                                        <div className="alert-info-cell">
                                            <strong className="alert-title">{alert.title}</strong>
                                            <small className="alert-desc">{alert.description}</small>
                                        </div>
                                    </td>
                                    <td>
                                            <span className={`status-pill severity-${alert.severity.toLowerCase()}`}>
                                                {alert.severity}
                                            </span>
                                    </td>
                                    <td>
                                            <span className={`status-pill state-${alert.status.toLowerCase()}`}>
                                                {getStatusText(alert.status)}
                                            </span>
                                    </td>
                                    <td className="date-cell">
                                        {new Date(alert.createdAt).toLocaleDateString()}
                                    </td>
                                    <td className="text-right">
                                        {alert.status === 'PENDING' ? (
                                            <button
                                                className="btn-action-small"
                                                onClick={() => handleAcknowledge(alert.id)}
                                            >
                                                Atender
                                            </button>
                                        ) : (
                                            <div className="resolved-icon" title={getStatusText(alert.status)}>
                                                <CheckCircle size={20} color="var(--success, #10b981)" />
                                            </div>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Alerts;