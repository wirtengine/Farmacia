import React from 'react';
import './RackVisualization.css';

export default function RackVisualization({ rack, ubicaciones, onSeleccionarCelda, seleccionActual, modoMovimiento, origenMovimiento, onIniciarMovimiento }) {
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

    const getTooltip = (ocupada, esOrigenMovimiento) => {
        if (esOrigenMovimiento) return '🖱️ Click para mover (origen)';
        if (!ocupada) return 'Posición libre – haz clic para asignar';
        return `💊 ${ocupada.medicamentoNombre || 'Producto'}\n📦 Lote: ${ocupada.loteDetalleId || 'N/A'}\n🔢 Cantidad: ${ocupada.cantidad ?? 'N/A'}\n🖱️ Click para mover`;
    };

    const handleCeldaClick = (nivel, columna, profundidadIndex, ocupada, ocupadaData) => {
        if (modoMovimiento) {
            // Si estamos en modo movimiento, solo se permiten clics en destino (libres) o cancelar en origen.
            // La lógica principal se maneja en Ubicaciones, aquí solo llamamos al callback.
            onSeleccionarCelda(nivel, columna, profundidadIndex);
            return;
        }

        // Modo normal: si la celda está ocupada y se tiene permiso, iniciar movimiento directamente
        if (ocupada && onIniciarMovimiento) {
            onIniciarMovimiento(ocupadaData, nivel, columna, profundidadIndex);
            return;
        }

        // Si está libre, seleccionar para asignar (abre drawer)
        onSeleccionarCelda(nivel, columna, profundidadIndex);
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
                        const nivelReal = alto - nIdx; // Visual: nivel 1 abajo
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

                                                    const esOrigenMovimiento = modoMovimiento && origenMovimiento &&
                                                        origenMovimiento.nivel === nivelIdx &&
                                                        origenMovimiento.columna === colIdx &&
                                                        origenMovimiento.profundidadIndex === profIdx;

                                                    return (
                                                        <div
                                                            key={profIdx}
                                                            className={`cell-unit 
                                                                ${ocupada ? 'is-occupied' : 'is-free'} 
                                                                ${esSeleccionada ? 'is-selected' : ''}
                                                                ${esOrigenMovimiento ? 'is-moving-source' : ''}`}
                                                            style={{
                                                                '--p-offset': `${profIdx * 5}px`,
                                                                zIndex: profIdx
                                                            }}
                                                            onClick={() => handleCeldaClick(nivelIdx, colIdx, profIdx, ocupada, ocupada)}
                                                            title={getTooltip(ocupada, esOrigenMovimiento)}
                                                        >
                                                            {esSeleccionada && <div className="selection-ping"></div>}
                                                            {ocupada && !esOrigenMovimiento && <div className="box-icon">💊</div>}
                                                            {esOrigenMovimiento && <div className="move-icon">🖱️</div>}
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
                    💡 Haz clic en una celda libre para asignar producto, o en una ocupada para mover.
                </div>
            </div>
        </div>
    );
}