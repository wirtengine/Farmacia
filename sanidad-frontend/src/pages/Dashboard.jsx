import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { obtenerResumenDashboard } from '../services/dashboard';
import { getAlerts } from '../services/alerts';
import { getPendingRecommendations } from '../services/recommendations';
import {
    Bell, RefreshCw, Info, Clock, AlertTriangle, Lightbulb
} from 'lucide-react';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
    PieChart, Pie, Cell, LineChart, Line
} from 'recharts';
import './Dashboard.css';

const formatCurrency = (value) => `C$ ${value.toFixed(2)}`;

const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
        return (
            <div className="custom-tooltip">
                <p className="tooltip-label">{label}</p>
                <p className="tooltip-value">{formatCurrency(payload[0].value)}</p>
            </div>
        );
    }
    return null;
};

export default function Dashboard() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const esAdmin = user?.rol === 'ADMIN';

    const [data, setData] = useState({}); // ← Inicializamos como objeto vacío
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [lastUpdated, setLastUpdated] = useState(null);
    const [pendingAlerts, setPendingAlerts] = useState(0);
    const [pendingRecs, setPendingRecs] = useState(0);

    const cargarDatos = async () => {
        setLoading(true);
        try {
            const [resumenRes, alertsRes, pendingRecsRes] = await Promise.all([
                obtenerResumenDashboard(),
                getAlerts(),
                getPendingRecommendations()
            ]);

            // Si la respuesta es válida, usamos sus datos; si no, dejamos el objeto vacío
            setData(resumenRes.data || {});
            setPendingAlerts(alertsRes.data.filter(alert => alert.status === 'PENDING').length);
            setPendingRecs(pendingRecsRes.data.length);
            setLastUpdated(new Date());
            setError('');
        } catch (err) {
            console.error(err);
            setError('Error al actualizar datos en tiempo real.');
            setData({}); // Asegurar que data no sea null
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        cargarDatos();
    }, []);

    if (loading && Object.keys(data).length === 0) {
        return (
            <div className="dashboard-loading-state">
                <div className="spinner"></div>
                <p>Sincronizando panel de control...</p>
            </div>
        );
    }

    // Preparar datos para gráficas con valores por defecto
    const ventasPorVendedor = (data.rankingVendedores || []).map(v => ({
        name: v.username,
        ventas: v.totalVentas
    }));

    const topProductos = (data.productosMasRentables || []).slice(0, 5).map(p => ({
        name: p.nombre,
        value: p.ingresos
    }));

    const tendenciaMensual = [
        { name: 'Mes Anterior', ventas: data.ventasMesAnterior || 0 },
        { name: 'Mes Actual', ventas: data.ventasMesActual || 0 }
    ];

    const COLORS = ['#10b981', '#3b82f6', '#6366f1', '#f59e0b', '#ef4444'];

    return (
        <div className="module-container dashboard-pro">
            <header className="dashboard-header">
                <div className="header-left">
                    <h1>Dashboard {esAdmin ? 'Ejecutivo' : 'Personal'}</h1>
                    <span className="user-badge">{user?.username} ({user?.rol})</span>
                    {lastUpdated && (
                        <span className="last-updated">
                            Sincronizado: {lastUpdated.toLocaleTimeString()}
                        </span>
                    )}
                </div>
                <button className="btn-refresh" onClick={cargarDatos}>
                    <RefreshCw size={16} className="refresh-icon-svg" /> Actualizar
                </button>
            </header>

            <div className="dashboard-content">
                <div className="kpi-grid">
                    <div className="kpi-card">
                        <h4>Ventas Hoy</h4>
                        <p>{formatCurrency(data.ventasDelDia?.totalVentas || 0)}</p>
                    </div>
                    <div className="kpi-card">
                        <h4>Ventas Mes</h4>
                        <p>{formatCurrency(data.ventasMesActual || 0)}</p>
                    </div>
                    <div className="kpi-card">
                        <h4>Facturas Hoy</h4>
                        <p>{data.ventasDelDia?.cantidadVentas || 0}</p>
                    </div>

                    <div className="kpi-card rec-card" onClick={() => navigate('/recommendations')}>
                        <span className="alert-status-badge" style={{background: '#e0e7ff', color: '#4338ca'}}>Sugerencia</span>
                        <h4>
                            <Lightbulb size={14} style={{marginRight: '8px', color: '#4338ca'}} />
                            Recomendaciones
                        </h4>
                        <p className="rec-number" style={{color: '#4338ca'}}>{pendingRecs}</p>
                        <span className="last-updated">Optimización de stock</span>
                        <div className="alert-icon" style={{color: '#4338ca', opacity: 0.1}}>
                            <Lightbulb size={60} />
                        </div>
                    </div>

                    <div className="kpi-card alert-card" onClick={() => navigate('/alerts')}>
                        <span className="alert-status-badge">Crítico</span>
                        <h4>
                            <span className="pulse-indicator"></span>
                            Alertas Pendientes
                        </h4>
                        <p className="alert-number">{pendingAlerts}</p>
                        <span className="last-updated">Revisar centro de control</span>
                        <div className="alert-icon">
                            <Bell size={60} />
                        </div>
                    </div>
                </div>

                <div className="charts-grid">
                    <div className="chart-card">
                        <h3>Ventas por Vendedor</h3>
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={ventasPorVendedor}>
                                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                                <YAxis tickFormatter={(value) => `C$${value}`} width={60} />
                                <Tooltip content={<CustomTooltip />} />
                                <Bar dataKey="ventas" fill="#10b981" radius={[6, 6, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>

                    <div className="chart-card pie-card">
                        <h3>Productos Más Rentables</h3>
                        <div className="pie-content">
                            <div className="pie-chart">
                                <ResponsiveContainer width="100%" height="100%">
                                    <PieChart>
                                        <Pie
                                            data={topProductos}
                                            dataKey="value"
                                            innerRadius="45%"
                                            outerRadius="80%"
                                            paddingAngle={4}
                                        >
                                            {topProductos.map((_, index) => (
                                                <Cell key={index} fill={COLORS[index % COLORS.length]} />
                                            ))}
                                        </Pie>
                                        <Tooltip content={<CustomTooltip />} />
                                    </PieChart>
                                </ResponsiveContainer>
                            </div>
                            <div className="pie-legend">
                                {topProductos.map((item, index) => (
                                    <div key={index} className="legend-item">
                                        <span className="legend-color" style={{ background: COLORS[index % COLORS.length] }}></span>
                                        <div className="legend-info">
                                            <span className="legend-name">{item.name}</span>
                                            <span className="legend-value">{formatCurrency(item.value)}</span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="chart-card full">
                        <h3>Tendencia de Ventas (Comparativa Mensual)</h3>
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={tendenciaMensual}>
                                <XAxis dataKey="name" />
                                <YAxis tickFormatter={(value) => `C$${value}`} width={60} />
                                <Tooltip content={<CustomTooltip />} />
                                <Line type="monotone" dataKey="ventas" stroke="#10b981" strokeWidth={4} dot={{ r: 6, fill: '#10b981' }} />
                            </LineChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {esAdmin && data && ( // ← Verificamos que data exista
                    <div className="bottom-grid">
                        <div className="table-card">
                            <h3>Ranking de Vendedores</h3>
                            <div className="table-scroll">
                                <table className="ranking-table">
                                    <thead>
                                    <tr>
                                        <th>#</th>
                                        <th>Nombre</th>
                                        <th>Ventas</th>
                                        <th>Total</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {data.rankingVendedores?.map((v, i) => (
                                        <tr key={i}>
                                            <td>{i + 1}</td>
                                            <td>{v.username}</td>
                                            <td>{v.cantidadVentas}</td>
                                            <td>{formatCurrency(v.totalVentas)}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <div className="table-card">
                            <h3>Stock Crítico</h3>
                            <ul className="stock-list">
                                {data.productosBajoStock?.map((p, i) => (
                                    <li key={i}>
                                        <span className="product-name">{p.nombre}</span>
                                        <span className="stock-qty">{p.stockTotal} uds</span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}