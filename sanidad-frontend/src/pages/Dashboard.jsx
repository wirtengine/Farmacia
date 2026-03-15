import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { obtenerResumenDashboard } from '../services/dashboard';
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
        } catch (err) {
            setError('Error al conectar con el servidor.');
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <div className="dashboard-loading-state"><div className="spinner"></div></div>;
    if (error) return <div className="dashboard-error-state">⚠️ {error}</div>;
    if (!data) return null;

    // LÓGICA DE CÁLCULOS ---------------------------------------------------------

    // 1. Variación del mes (Global)
    const variacionMes = data.ventasMesAnterior > 0
        ? ((data.ventasMesActual - data.ventasMesAnterior) / data.ventasMesAnterior * 100).toFixed(1)
        : data.ventasMesActual > 0 ? 100 : 0;
    const isPositiveTrend = variacionMes >= 0;

    // 2. Buscar info del usuario actual en el ranking
    const miInfo = data.rankingVendedores.find(v => v.username === user?.username) || null;

    // 3. Cálculo de Aporte (Mis ventas vs Total de la farmacia)
    const porcentajeAporte = data.ventasMesActual > 0 && miInfo
        ? ((miInfo.totalVentas / data.ventasMesActual) * 100).toFixed(1)
        : "0.0";

    // 4. Promedio por factura personal
    const promedioVenta = miInfo && miInfo.cantidadVentas > 0
        ? (miInfo.totalVentas / miInfo.cantidadVentas).toFixed(2)
        : "0.00";

    const mesActualNombre = new Date().toLocaleDateString('es-ES', { month: 'long' });

    return (
        <div className="module-container dashboard-compact">
            <header className="dashboard-header-mini">
                <div className="title-group">
                    <h1>Panel {esAdmin ? 'Ejecutivo' : 'Personal'}</h1>
                    <span className="user-welcome">| {user?.username} ({user?.rol})</span>
                </div>
                <button className="btn-refresh-mini" onClick={cargarDatos}>↻ Actualizar</button>
            </header>

            {/* FILA DE KPIs (4 tarjetas superiores) */}
            <div className="kpi-grid-mini">
                <div className="kpi-card-mini">
                    <span className="kpi-icon-mini blue">💰</span>
                    <div className="kpi-content-mini">
                        <label>Ventas Hoy (Local)</label>
                        <p>C${data.ventasDelDia.totalVentas.toFixed(2)}</p>
                    </div>
                </div>

                <div className="kpi-card-mini">
                    <span className="kpi-icon-mini green">📅</span>
                    <div className="kpi-content-mini">
                        <label>Ventas Mes (Total)</label>
                        <p>C${data.ventasMesActual.toFixed(2)}
                            <small className={isPositiveTrend ? 'text-pos' : 'text-neg'}>
                                ({isPositiveTrend ? '↑' : '↓'}{Math.abs(variacionMes)}%)
                            </small>
                        </p>
                    </div>
                </div>

                {esAdmin ? (
                    <>
                        <div className="kpi-card-mini">
                            <span className="kpi-icon-mini purple">🏆</span>
                            <div className="kpi-content-mini">
                                <label>Producto Líder</label>
                                <p title={data.productosMasRentables[0]?.nombre}>
                                    {data.productosMasRentables[0]?.nombre || 'N/A'}
                                </p>
                            </div>
                        </div>
                        <div className="kpi-card-mini">
                            <span className="kpi-icon-mini red">⚠️</span>
                            <div className="kpi-content-mini">
                                <label>Stock Crítico</label>
                                <p className="text-danger">
                                    {data.productosBajoStock[0]?.nombre || 'Sin alertas'}
                                </p>
                            </div>
                        </div>
                    </>
                ) : (
                    <>
                        <div className="kpi-card-mini highlight-personal">
                            <span className="kpi-icon-mini purple">📊</span>
                            <div className="kpi-content-mini">
                                <label>Mis Facturas</label>
                                <p>{miInfo?.cantidadVentas || 0} Registradas</p>
                            </div>
                        </div>
                        <div className="kpi-card-mini highlight-personal">
                            <span className="kpi-icon-mini orange">✨</span>
                            <div className="kpi-content-mini">
                                <label>Mi Total</label>
                                <p>C${miInfo?.totalVentas.toFixed(2) || "0.00"}</p>
                            </div>
                        </div>
                    </>
                )}
            </div>

            {/* CUERPO PRINCIPAL */}
            {esAdmin ? (
                <div className="dashboard-main-grid">
                    <section className="section-card-mini">
                        <div className="section-title-mini">Ranking Vendedores (Top 5)</div>
                        <table className="table-mini">
                            <thead>
                            <tr>
                                <th className="text-center">#</th>
                                <th>Vendedor</th>
                                <th className="text-center">Ventas</th>
                                <th className="text-right">Total</th>
                            </tr>
                            </thead>
                            <tbody>
                            {data.rankingVendedores.slice(0, 5).map((v, idx) => (
                                <tr key={idx} className={v.username === user?.username ? 'row-current-user' : ''}>
                                    <td className="rank-cell-mini">{idx === 0 ? '🥇' : idx === 1 ? '🥈' : idx === 2 ? '🥉' : idx + 1}</td>
                                    <td className="font-600">{v.username} {v.username === user?.username && '(Tú)'}</td>
                                    <td className="text-center">{v.cantidadVentas}</td>
                                    <td className="text-right font-600 text-blue">C${v.totalVentas.toFixed(2)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </section>

                    <div className="side-column-mini">
                        <section className="section-card-mini">
                            <div className="section-title-mini">Productos Rentables</div>
                            <div className="list-mini">
                                {data.productosMasRentables.slice(0, 4).map((p, idx) => (
                                    <div className="list-item-mini" key={idx}>
                                        <span className="truncate-text">{p.nombre}</span>
                                        <span className="amount-tag-mini">C${p.ingresos.toFixed(0)}</span>
                                    </div>
                                ))}
                            </div>
                        </section>

                        <section className="section-card-mini">
                            <div className="section-title-mini">Stock Crítico</div>
                            <div className="list-mini">
                                {data.productosBajoStock.slice(0, 4).map((p, idx) => (
                                    <div className="list-item-mini" key={idx}>
                                        <span className="text-danger truncate-text">{p.nombre}</span>
                                        <span className="stock-badge-mini">{p.stockTotal} uds</span>
                                    </div>
                                ))}
                            </div>
                        </section>
                    </div>
                </div>
            ) : (
                <div className="vendedor-personal-section">
                    <section className="section-card-mini">
                        <div className="section-title-mini">Resumen de Actividad - {mesActualNombre}</div>
                        <div className="personal-welcome-box">
                            <div className="welcome-text">
                                <h2>¡Hola, {user?.username}! 👋</h2>
                                <p>Has realizado un gran trabajo este mes manejando el inventario y las ventas.</p>
                            </div>

                            <div className="personal-stats-grid">
                                <div className="stat-item">
                                    <span className="stat-label">Tu aporte a la Farmacia</span>
                                    <div className="stat-progress-bar">
                                        <div className="progress-fill" style={{ width: `${porcentajeAporte}%` }}></div>
                                    </div>
                                    <span className="stat-value">{porcentajeAporte}% del total mensual</span>
                                </div>

                                <div className="stat-item">
                                    <span className="stat-label">Promedio de Venta Personal</span>
                                    <span className="stat-value-large">C${promedioVenta}</span>
                                    <span className="stat-subtext">por cada factura emitida</span>
                                </div>
                            </div>

                            {(!miInfo || miInfo.cantidadVentas === 0) && (
                                <div className="motivation-text">
                                    Aún no registras ventas en {mesActualNombre}. ¡Vamos a por la primera del día!
                                </div>
                            )}
                        </div>
                    </section>
                </div>
            )}
        </div>
    );
}