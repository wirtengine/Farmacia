import React from 'react';
import './RackVisualization.css';

export default function RackVisualization({
                                              rack,
                                              ubicaciones,
                                              onSeleccionarCelda,
                                              seleccionActual,
                                              modoMovimiento,
                                              origenMovimiento,
                                              onIniciarMovimiento
                                          }) {
    if (!rack) return null;

    const { ancho, alto, profundidad } = rack;

    // Mapa de ocupación (todas las profundidades)
    const ocupadasMap = new Map();
    if (Array.isArray(ubicaciones)) {
        ubicaciones.forEach(ubic => {
            const key = `${ubic.nivel},${ubic.columna},${ubic.profundidadIndex}`;
            ocupadasMap.set(key, ubic);
        });
    }

    const getTooltip = (ocupada, profIdx) => {
        if (!ocupada) return `Posición libre – fondo ${profIdx + 1}`;
        return `💊 ${ocupada.medicamentoNombre || 'Producto'}\n📦 Lote: ${ocupada.loteDetalleId}\n🔢 Cantidad: ${ocupada.cantidad}\n📌 Fondo: ${profIdx + 1}`;
    };

    const handleClick = (nivel, columna, profIdx, ocupada, data) => {
        if (modoMovimiento) {
            onSeleccionarCelda(nivel, columna, profIdx);
            return;
        }
        if (ocupada && onIniciarMovimiento) {
            onIniciarMovimiento(data, nivel, columna, profIdx);
            return;
        }
        onSeleccionarCelda(nivel, columna, profIdx);
    };

    return (
        <div className="pharmacy-rack-card fade-in-row">
            <div className="rack-header-modern">
                <div className="rack-main-info">
                    <div className="rack-icon-bg">📦</div>
                    <div>
                        <h3>{rack.nombre}</h3>
                        <p className="text-muted">
                            {alto} niveles · {ancho} columnas · {profundidad} fondos
                        </p>
                    </div>
                </div>
                <div className="rack-stats">
                    <span className="badge-blue">
                        Capacidad: {ancho * alto * profundidad}
                    </span>
                </div>
            </div>

            {/* Indicador visual de profundidad (solo informativo) */}
            {profundidad > 1 && (
                <div className="depth-reference">
                    <span className="depth-label">Profundidades:</span>
                    <div className="depth-dots">
                        {Array.from({ length: profundidad }).map((_, i) => (
                            <span key={i} className="depth-dot" title={`Fondo ${i + 1}`}>
                                {i + 1}
                            </span>
                        ))}
                    </div>
                </div>
            )}

            <div className="rack-viewport">
                <div className="shelf-container">
                    {Array.from({ length: alto }).map((_, nIdx) => {
                        const nivelReal = alto - nIdx; // visual: 1 abajo
                        const nivel = nIdx;
                        return (
                            <div key={nivelReal} className="shelf-level-row">
                                <div className="level-label">Nivel {nivelReal}</div>
                                <div className="columns-container">
                                    {Array.from({ length: ancho }).map((_, col) => (
                                        <div key={col} className="column-space">
                                            <div className="depth-stack">
                                                {Array.from({ length: profundidad }).map((_, prof) => {
                                                    const key = `${nivel},${col},${prof}`;
                                                    const ocupada = ocupadasMap.get(key);
                                                    const esSeleccionada =
                                                        seleccionActual?.nivel === nivel &&
                                                        seleccionActual?.columna === col &&
                                                        seleccionActual?.profundidadIndex === prof;
                                                    const esOrigen = modoMovimiento && origenMovimiento &&
                                                        origenMovimiento.nivel === nivel &&
                                                        origenMovimiento.columna === col &&
                                                        origenMovimiento.profundidadIndex === prof;

                                                    return (
                                                        <div
                                                            key={prof}
                                                            className={`cell-unit 
                                                                ${ocupada ? 'is-occupied' : 'is-free'} 
                                                                ${esSeleccionada ? 'is-selected' : ''}
                                                                ${esOrigen ? 'is-moving-source' : ''}`}
                                                            style={{
                                                                '--p-offset': `${prof * 5}px`,
                                                                zIndex: prof
                                                            }}
                                                            onClick={() => handleClick(nivel, col, prof, ocupada, ocupada)}
                                                            title={getTooltip(ocupada, prof)}
                                                        >
                                                            <span className="depth-number">{prof + 1}</span>
                                                            {esSeleccionada && <div className="selection-ping"></div>}
                                                            {ocupada && !esOrigen && <div className="box-icon">💊</div>}
                                                            {esOrigen && <div className="move-icon">🖱️</div>}
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
                    {modoMovimiento && <div className="legend-item"><span className="swatch moving"></span> Origen movimiento</div>}
                </div>
                <div className="legend-tip">
                    💡 Números indican la profundidad (fondo). Haz clic en celda libre para asignar.
                </div>
            </div>
        </div>
    );
}