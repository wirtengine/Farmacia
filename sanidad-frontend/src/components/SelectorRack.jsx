import React, { useEffect, useState } from 'react';
import { listarRacks } from '../services/racks';
import './SelectorRack.css';

export default function SelectorRack({ rackSeleccionado, onRackChange }) {
    const [racks, setRacks] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        cargarRacks();
    }, []);

    const cargarRacks = async () => {
        try {
            const res = await listarRacks();
            setRacks(res.data || []);
        } catch (error) {
            console.error('Error cargando racks:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) return (
        <div className="rack-selector-loading">
            <div className="spinner-mini"></div>
            <span>Buscando estantes disponibles...</span>
        </div>
    );

    return (
        <div className="rack-selector-wrapper fade-in-row">
            <div className="selector-label">
                <span className="step-number">1</span>
                <label>Selecciona el Rack de destino</label>
            </div>

            <div className="rack-cards-grid">
                {racks.length > 0 ? (
                    racks.map(rack => (
                        <div
                            key={rack.id}
                            className={`rack-option-card ${rackSeleccionado?.id === rack.id ? 'active' : ''}`}
                            onClick={() => onRackChange(rack)}
                        >
                            <div className="rack-card-icon">
                                {rackSeleccionado?.id === rack.id ? '✅' : '📦'}
                            </div>
                            <div className="rack-card-content">
                                <span className="rack-name">{rack.nombre}</span>
                                <span className="rack-details">
                                    {rack.ancho}×{rack.alto}×{rack.profundidad} celdas
                                </span>
                            </div>
                            {rackSeleccionado?.id === rack.id && <div className="active-indicator" />}
                        </div>
                    ))
                ) : (
                    <div className="no-racks-alert">
                        ⚠️ No hay racks configurados en el sistema.
                    </div>
                )}
            </div>
        </div>
    );
}