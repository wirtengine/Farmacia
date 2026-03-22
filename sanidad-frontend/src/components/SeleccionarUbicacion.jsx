import React, { useState, useEffect } from 'react';
import { listarUbicacionesPorRack } from '../services/ubicaciones';
import './SeleccionarUbicacion.css';

export default function SeleccionarUbicacion({ rack, onUbicacionSeleccionada }) {
    const [ubicaciones, setUbicaciones] = useState([]);
    const [loading, setLoading] = useState(false);
    const [seleccion, setSeleccion] = useState(null);

    useEffect(() => {
        if (rack) {
            cargarUbicaciones();
            setSeleccion(null);
        }
    }, [rack]);

    const cargarUbicaciones = async () => {
        setLoading(true);
        try {
            const res = await listarUbicacionesPorRack(rack.id);
            setUbicaciones(res.data || []);
        } catch (error) {
            console.error('Error cargando ubicaciones:', error);
        } finally {
            setLoading(false);
        }
    };

    const ocupadasMap = new Map();
    ubicaciones.forEach(ubic => {
        const key = `${ubic.nivel},${ubic.columna},${ubic.profundidadIndex}`;
        ocupadasMap.set(key, true);
    });

    const handleClickCelda = (nivel, columna, profundidadIndex) => {
        const nuevaSeleccion = { nivel, columna, profundidadIndex };
        setSeleccion(nuevaSeleccion);
        onUbicacionSeleccionada(nuevaSeleccion);
    };

    if (!rack) return null;

    return (
        <div className="select-location-card fade-in-row">
            <div className="select-location-header">
                <div className="step-badge">Paso 2</div>
                <h4>Ubicación en {rack.nombre}</h4>
            </div>

            <p className="location-instruction">
                <span className="icon-pointer">🖱️</span>
                Selecciona un espacio libre en el estante
            </p>

            <div className="mini-rack-viewport">
                {loading ? (
                    <div className="mini-loader">Actualizando mapa...</div>
                ) : (
                    <div className="mini-shelf-structure">
                        {Array.from({ length: rack.alto }).map((_, nIdx) => {
                            const nivelIdx = rack.alto - 1 - nIdx;
                            return (
                                <div key={nivelIdx} className="mini-level-row">
                                    <div className="mini-label">N{nivelIdx + 1}</div>
                                    <div className="mini-columns">
                                        {Array.from({ length: rack.ancho }).map((_, colIdx) => (
                                            <div key={colIdx} className="mini-depth-stack">
                                                {Array.from({ length: rack.profundidad }).map((_, pIdx) => {
                                                    const profIdx = pIdx;
                                                    const key = `${nivelIdx},${colIdx},${profIdx}`;
                                                    const estaOcupada = ocupadasMap.has(key);
                                                    const esSeleccionada = seleccion &&
                                                        seleccion.nivel === nivelIdx &&
                                                        seleccion.columna === colIdx &&
                                                        seleccion.profundidadIndex === profIdx;

                                                    return (
                                                        <button
                                                            key={profIdx}
                                                            type="button"
                                                            className={`mini-cell 
                                                                ${estaOcupada ? 'is-blocked' : 'is-available'} 
                                                                ${esSeleccionada ? 'is-picked' : ''}`}
                                                            style={{
                                                                '--mini-p': `${profIdx * 3}px`,
                                                                zIndex: profIdx
                                                            }}
                                                            disabled={estaOcupada}
                                                            onClick={() => handleClickCelda(nivelIdx, colIdx, profIdx)}
                                                            title={estaOcupada ? 'Espacio Ocupado' : `Nivel ${nivelIdx+1}, Col ${colIdx+1}, Prof ${profIdx+1}`}
                                                        >
                                                            {esSeleccionada && <span className="check-icon">✓</span>}
                                                            {estaOcupada && <span className="lock-icon">🔒</span>}
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            <div className="selection-summary">
                {seleccion ? (
                    <div className="coord-badge">
                        Ubicación elegida: <strong>N{seleccion.nivel + 1}-C{seleccion.columna + 1}-P{seleccion.profundidadIndex + 1}</strong>
                    </div>
                ) : (
                    <div className="coord-badge empty">Esperando selección...</div>
                )}
            </div>

            <div className="mini-legend-modern">
                <div className="l-item"><span className="l-dot free"></span> Libre</div>
                <div className="l-item"><span className="l-dot busy"></span> Lleno</div>
                <div className="l-item"><span className="l-dot active"></span> Tu elección</div>
            </div>
        </div>
    );
}