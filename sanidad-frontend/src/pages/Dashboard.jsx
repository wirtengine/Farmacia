import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { obtenerResumenDashboard } from '../services/dashboard';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
    PieChart, Pie, Cell, LineChart, Line
} from 'recharts';
import './Dashboard.css';

export default function Dashboard() {

    const { user } = useAuth();
    const esAdmin = user?.rol === 'ADMIN';

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        cargarDatos();
    }, []);

    const cargarDatos = async () => {
        try {
            const res = await obtenerResumenDashboard();
            setData(res.data);
        } catch {
            setError('Error al conectar con el servidor.');
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <div className="dashboard-loading-state"><div className="spinner"></div></div>;
    if (error) return <div className="dashboard-error-state">⚠️ {error}</div>;
    if (!data) return null;

    // =========================
    // DATOS PARA GRÁFICAS
    // =========================

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

    // =========================
    // TOOLTIP PERSONALIZADO
    // =========================

    const CustomTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            return (
                <div style={{
                    background: "#fff",
                    padding: "10px",
                    border: "1px solid #e5e7eb",
                    borderRadius: "8px",
                    boxShadow: "0 4px 10px rgba(0,0,0,0.05)"
                }}>
                    <strong>{label}</strong>
                    <p style={{ margin: 0 }}>
                        C${payload[0].value.toFixed(2)}
                    </p>
                </div>
            );
        }
        return null;
    };

    return (
        <div className="module-container dashboard-pro">

            {/* HEADER */}
            <header className="dashboard-header">
                <div>
                    <h1>Dashboard {esAdmin ? 'Ejecutivo' : 'Personal'}</h1>
                    <span>{user?.username} ({user?.rol})</span>
                </div>
                <button onClick={cargarDatos}>Actualizar</button>
            </header>

            <div className="dashboard-content">

                {/* KPIs */}
                <div className="kpi-grid">
                    <div className="kpi-card">
                        <h4>Ventas Hoy</h4>
                        <p>C${data.ventasDelDia.totalVentas.toFixed(2)}</p>
                    </div>

                    <div className="kpi-card">
                        <h4>Ventas Mes</h4>
                        <p>C${data.ventasMesActual.toFixed(2)}</p>
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

                    {/* BARRAS */}
                    <div className="chart-card">
                        <h3>Ventas por Vendedor</h3>
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={ventasPorVendedor}>
                                <XAxis dataKey="name" />
                                <YAxis />
                                <Tooltip content={<CustomTooltip />} />
                                <Bar dataKey="ventas" radius={[6,6,0,0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>

                    {/* PIE */}
                    <div className="chart-card">
                        <h3>Productos Más Rentables</h3>
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={topProductos}
                                    dataKey="value"
                                    nameKey="name"
                                    outerRadius={90}
                                    label={({ name }) => name}
                                >
                                    {topProductos.map((entry, index) => (
                                        <Cell key={index} fill={COLORS[index % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip content={<CustomTooltip />} />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>

                    {/* LINEA */}
                    <div className="chart-card full">
                        <h3>Tendencia de Ventas</h3>
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={tendenciaMensual}>
                                <XAxis dataKey="name" />
                                <YAxis />
                                <Tooltip content={<CustomTooltip />} />
                                <Line type="monotone" dataKey="ventas" stroke="#10b981" strokeWidth={3} />
                            </LineChart>
                        </ResponsiveContainer>
                    </div>

                </div>

                {/* TABLAS */}
                {esAdmin && (
                    <div className="bottom-grid">

                        <div className="table-card">
                            <h3>Ranking Vendedores</h3>
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
                                        <td>{i+1}</td>
                                        <td>{v.username}</td>
                                        <td>{v.cantidadVentas}</td>
                                        <td>C${v.totalVentas.toFixed(2)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>

                        <div className="table-card">
                            <h3>Stock Bajo</h3>
                            <ul>
                                {data.productosBajoStock.map((p, i) => (
                                    <li key={i}>
                                        {p.nombre} - {p.stockTotal} uds
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