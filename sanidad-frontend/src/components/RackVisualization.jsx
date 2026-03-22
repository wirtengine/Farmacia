import React from 'react';
import './RackVisualization.css';

export default function RackVisualization({ rack, ubicaciones, onSeleccionarCelda, seleccionActual }) {
    if (!rack) return <div className="empty-state">No hay datos del rack</div>;

    const { ancho, alto, profundidad } = rack;

    // Mapa de ocupación
    const ocupadasMap = new Map();
    if (Array.isArray(ubicaciones)) {
        ubicaciones.forEach(ubic => {
            const key = `${ubic.nivel},${ubic.columna},${ubic.profundidadIndex}`;
            ocupadasMap.set(key, ubic);
        });
    }

    const getTooltip = (ocupada) => {
        if (!ocupada) return 'Posición libre – haz clic para asignar';
        return `💊 ${ocupada.medicamentoNombre || 'Producto'}\n📦 Lote: ${ocupada.loteDetalleId || 'N/A'}\n🔢 Cantidad: ${ocupada.cantidad ?? 'N/A'}`;
    };

    return (
        <div className="pharmacy-rack-card fade-in-row">
            <div className="rack-header-modern">
                <div className="rack-main-info">
                    <div className="rack-icon-bg">📦</div>
                    <div>
                        <h3>{rack.nombre}</h3>
                        <p className="text-muted">
                            {alto} niveles · {ancho} columnas · {profundidad} de fondo
                        </p>
                    </div>
                </div>
                <div className="rack-stats">
                    <span className="badge-blue">
                        Capacidad: {ancho * alto * profundidad}
                    </span>
                </div>
            </div>

            <div className="rack-viewport">
                <div className="shelf-container">
                    {Array.from({ length: alto }).map((_, nIdx) => {
                        // Nivel real (1 = abajo, alto = arriba)
                        const nivelReal = alto - nIdx;
                        const nivelIdx = nIdx;

                        return (
                            <div key={nivelReal} className="shelf-level-row">
                                <div className="level-label">Nivel {nivelReal}</div>

                                <div className="columns-container">
                                    {Array.from({ length: ancho }).map((_, colIdx) => (
                                        <div key={colIdx} className="column-space">
                                            <div className="depth-stack">
                                                {Array.from({ length: profundidad }).map((_, profIdx) => {
                                                    const key = `${nivelIdx},${colIdx},${profIdx}`;
                                                    const ocupada = ocupadasMap.get(key);
                                                    const esSeleccionada =
                                                        seleccionActual?.nivel === nivelIdx &&
                                                        seleccionActual?.columna === colIdx &&
                                                        seleccionActual?.profundidadIndex === profIdx;

                                                    return (
                                                        <div
                                                            key={profIdx}
                                                            className={`cell-unit 
                                                                ${ocupada ? 'is-occupied' : 'is-free'} 
                                                                ${esSeleccionada ? 'is-selected' : ''}`}
                                                            style={{
                                                                '--p-offset': `${profIdx * 5}px`,
                                                                zIndex: profIdx
                                                            }}
                                                            onClick={() => {
                                                                onSeleccionarCelda(nivelIdx, colIdx, profIdx);
                                                            }}
                                                            title={getTooltip(ocupada)}
                                                        >
                                                            {esSeleccionada && <div className="selection-ping"></div>}
                                                            {ocupada && <div className="box-icon">💊</div>}
                                                        </div>
                                                    );
                                                })}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>

            <div className="rack-footer-legend">
                <div className="legend-group">
                    <div className="legend-item"><span className="swatch free"></span> Libre</div>
                    <div className="legend-item"><span className="swatch occupied"></span> Ocupado</div>
                    <div className="legend-item"><span className="swatch selected"></span> Selección</div>
                </div>
                <div className="legend-tip">
                    💡 Haz clic en una celda libre para asignar producto
                </div>
            </div>
        </div>
    );
}