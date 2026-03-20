import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { obtenerResumenDashboard } from '../services/dashboard';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
    PieChart, Pie, Cell, LineChart, Line
} from 'recharts';
import './Dashboard.css';

// Función para formatear montos en Córdobas
const formatCurrency = (value) => `C$ ${value.toFixed(2)}`;

// Tooltip personalizado (reutilizable)
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
    const esAdmin = user?.rol === 'ADMIN';

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [lastUpdated, setLastUpdated] = useState(null);

    const cargarDatos = async () => {
        setLoading(true);
        try {
            const res = await obtenerResumenDashboard();
            setData(res.data);
            setLastUpdated(new Date());
        } catch {
            setError('Error al conectar con el servidor.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        cargarDatos();
    }, []);

    if (loading) return (
        <div className="dashboard-loading-state">
            <div className="spinner"></div>
            <p>Cargando indicadores...</p>
        </div>
    );

    if (error) return (
        <div className="dashboard-error-state">
            ⚠️ {error}
        </div>
    );

    if (!data) return null;

    // Preparación de datos para gráficas
    const ventasPorVendedor = data.rankingVendedores.map(v => ({
        name: v.username,
        ventas: v.totalVentas
    }));

    const topProductos = data.productosMasRentables.slice(0, 5).map(p => ({
        name: p.nombre,
        value: p.ingresos
    }));

    const tendenciaMensual = [
        { name: 'Mes Anterior', ventas: data.ventasMesAnterior },
        { name: 'Mes Actual', ventas: data.ventasMesActual }
    ];

    const COLORS = ['#10b981', '#3b82f6', '#6366f1', '#f59e0b', '#ef4444'];

    return (
        <div className="module-container dashboard-pro">
            {/* HEADER */}
            <header className="dashboard-header">
                <div className="header-left">
                    <h1>Dashboard {esAdmin ? 'Ejecutivo' : 'Personal'}</h1>
                    <span className="user-badge">{user?.username} ({user?.rol})</span>
                    {lastUpdated && (
                        <span className="last-updated">
                            Última actualización: {lastUpdated.toLocaleTimeString()}
                        </span>
                    )}
                </div>
                <button className="btn-refresh" onClick={cargarDatos}>
                    <span className="refresh-icon">↻</span> Actualizar
                </button>
            </header>

            <div className="dashboard-content">
                {/* KPIs */}
                <div className="kpi-grid">
                    <div className="kpi-card">
                        <h4>Ventas Hoy</h4>
                        <p>{formatCurrency(data.ventasDelDia.totalVentas)}</p>
                    </div>
                    <div className="kpi-card">
                        <h4>Ventas Mes</h4>
                        <p>{formatCurrency(data.ventasMesActual)}</p>
                    </div>
                    <div className="kpi-card">
                        <h4>Facturas Hoy</h4>
                        <p>{data.ventasDelDia.cantidadVentas}</p>
                    </div>
                    <div className="kpi-card">
                        <h4>Stock Bajo</h4>
                        <p>{data.productosBajoStock.length}</p>
                    </div>
                </div>

                {/* GRÁFICAS */}
                <div className="charts-grid">
                    {/* Gráfico de Barras */}
                    <div className="chart-card">
                        <h3>Ventas por Vendedor</h3>
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={ventasPorVendedor} margin={{ top: 10, right: 10, left: 0, bottom: 20 }}>
                                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                                <YAxis tickFormatter={(value) => `C$${value}`} width={60} />
                                <Tooltip content={<CustomTooltip />} />
                                <Bar dataKey="ventas" fill="#10b981" radius={[6, 6, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>

                    {/* Gráfico de Pastel con Leyenda Personalizada */}
                    <div className="chart-card pie-card">
                        <h3>Productos Más Rentables</h3>
                        <div className="pie-content">
                            <div className="pie-chart">
                                <ResponsiveContainer width="100%" height="100%">
                                    <PieChart>
                                        <Pie
                                            data={topProductos}
                                            dataKey="value"
                                            nameKey="name"
                                            outerRadius="80%"
                                            innerRadius="40%"
                                            paddingAngle={2}
                                        >
                                            {topProductos.map((entry, index) => (
                                                <Cell key={index} fill={COLORS[index % COLORS.length]} stroke="none" />
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

                    {/* Gráfico de Línea (Tendencia) */}
                    <div className="chart-card full">
                        <h3>Tendencia de Ventas</h3>
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={tendenciaMensual} margin={{ top: 10, right: 10, left: 0, bottom: 20 }}>
                                <XAxis dataKey="name" />
                                <YAxis tickFormatter={(value) => `C$${value}`} width={60} />
                                <Tooltip content={<CustomTooltip />} />
                                <Line type="monotone" dataKey="ventas" stroke="#10b981" strokeWidth={3} dot={{ r: 6 }} />
                            </LineChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* TABLAS ADICIONALES (solo admin) */}
                {esAdmin && (
                    <div className="bottom-grid">
                        <div className="table-card">
                            <h3>Ranking de Vendedores</h3>
                            <div className="table-scroll">
                                <table>
                                    <thead>
                                    <tr>
                                        <th>#</th>
                                        <th>Nombre</th>
                                        <th>Ventas</th>
                                        <th>Total</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {data.rankingVendedores.map((v, i) => (
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
                            <h3>Productos con Stock Bajo</h3>
                            <ul className="stock-list">
                                {data.productosBajoStock.map((p, i) => (
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