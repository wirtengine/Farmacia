import { useState, useEffect, useCallback } from "react";
import { useAuth } from "../context/AuthContext";
import {
    uploadReceta,
    validarReceta,
    listarPendientes,
    listarPorFarmaceutico,
    listarTodas,
} from "../services/recetas";
import "./Recetas.css";
import {
    alertaConfirmacion,
    alertaExito,
    alertaError
} from "../alertas";

export default function Recetas() {
    const { user } = useAuth();
    const esAdmin = user?.rol === "ADMIN";
    const farmaceuticoId = user?.id;

    const [recetas, setRecetas] = useState([]);
    const [file, setFile] = useState(null);
    const [cargando, setCargando] = useState(false);
    const [mostrarTodas, setMostrarTodas] = useState(false);
    const [codigoMinsa, setCodigoMinsa] = useState("");

    const cargarRecetas = useCallback(async () => {
        try {
            setCargando(true);
            let res;

            if (esAdmin) {
                res = mostrarTodas
                    ? await listarTodas()
                    : await listarPendientes();
            } else {
                res = mostrarTodas
                    ? await listarTodas()
                    : await listarPorFarmaceutico(farmaceuticoId);
            }

            setRecetas(res.data || []);
        } catch (error) {
            console.error("Error al cargar recetas", error);
        } finally {
            setCargando(false);
        }

    }, [esAdmin, mostrarTodas, farmaceuticoId]);

    useEffect(() => {
        cargarRecetas();
    }, [cargarRecetas]);

    const handleUpload = async () => {

        if (!file) {
            alertaError("Selecciona una imagen");
            return;
        }

        if (!codigoMinsa.trim()) {
            alertaError("El Código MINSA es obligatorio");
            return;
        }

        try {

            setCargando(true);

            await uploadReceta(file, farmaceuticoId, codigoMinsa);

            setFile(null);
            setCodigoMinsa("");

            await cargarRecetas();

            alertaExito("Receta subida correctamente");

        } catch (error) {

            console.error(error);

            alertaError("Error al subir la receta");

        } finally {

            setCargando(false);

        }
    };

    const handleValidar = async (recetaId, aprobar) => {

        const result = await alertaConfirmacion({
            titulo: aprobar ? "Aprobar receta" : "Rechazar receta",
            texto: aprobar
                ? "¿Desea aprobar esta receta?"
                : "¿Desea rechazar esta receta?",
            confirmar: aprobar ? "Aprobar" : "Rechazar",
            cancelar: "Cancelar",
            icono: aprobar ? "question" : "warning"
        });

        if (!result.isConfirmed) return;

        try {

            setCargando(true);

            await validarReceta(recetaId, aprobar, farmaceuticoId);

            await cargarRecetas();

            if (aprobar) {
                alertaExito("Receta aprobada correctamente");
            } else {
                alertaExito("Receta rechazada");
            }

        } catch (error) {

            console.error(error);

            alertaError("Error al validar la receta");

        } finally {

            setCargando(false);

        }
    };

    const formatearFecha = (fecha) => {
        if (!fecha) return "";
        return new Date(fecha).toLocaleString();
    };

    return (
        <div className="module-container">

            {/* HEADER */}
            <header className="module-header">
                <div className="header-title">
                    <h1>📄 Gestión de Recetas</h1>
                    <p>Validación y control de recetas médicas</p>
                </div>
            </header>

            {/* UPLOAD */}
            <section className="receta-upload-card">
                <div className="upload-header">
                    <h3>📤 Subir nueva receta</h3>
                    <span className="upload-sub">
                        Formato permitido: JPG, PNG
                    </span>
                </div>

                <div className="upload-body">
                    <label className="file-input-wrapper">
                        <input
                            type="file"
                            accept="image/*"
                            onChange={(e) => setFile(e.target.files[0])}
                        />
                        <span>
                            {file ? file.name : "Seleccionar imagen"}
                        </span>
                    </label>

                    <input
                      type="text"
                      placeholder="Ingrese Código MINSA"
                      value={codigoMinsa}
                      onChange={(e) => setCodigoMinsa(e.target.value)}
                      className="input-minsa"
                    />

                    <button
                        className="btn-upload"
                        onClick={handleUpload}
                        disabled={!file || !codigoMinsa.trim() || cargando}
                    >
                        {cargando ? "Subiendo..." : "Subir Receta"}
                    </button>
                </div>
            </section>

            {/* LISTADO */}
            <section className="receta-list-card">

                <div className="list-header">
                    <div>
                        <h3>
                            {mostrarTodas
                                ? "📋 Historial completo"
                                : esAdmin
                                    ? "🧾 Recetas pendientes"
                                    : "📚 Mis recetas"}
                        </h3>
                    </div>

                    {/* TOGGLE GLOBAL */}
                    <button
                        className={`btn-toggle-historial ${mostrarTodas ? "active" : ""}`}
                        onClick={() => setMostrarTodas(!mostrarTodas)}
                    >
                        {mostrarTodas
                            ? "Ver pendientes / propias"
                            : "Ver todas"}
                    </button>
                </div>

                {/* LOADING */}
                {cargando && (
                    <div className="loading-state">
                        <div className="spinner" />
                        <p>Cargando recetas...</p>
                    </div>
                )}

                {/* EMPTY */}
                {!cargando && recetas.length === 0 && (
                    <div className="empty-state">
                        <p>No se encontraron recetas.</p>
                    </div>
                )}

                {/* GRID */}
                <div className="recetas-grid">
                    {recetas.map((receta) => (
                        <div key={receta.id} className="receta-card">

                            {/* IMG */}
                            <div className="receta-img-wrapper">
                                <img
                                    src={`http://localhost:8080${receta.imagenUrl}`}
                                    alt="Receta"
                                    onError={(e) => {
                                        e.target.src = "/placeholder.png";
                                    }}
                                />
                                <span className={`estado-badge ${receta.estado.toLowerCase()}`}>
                                    {receta.estado}
                                </span>
                            </div>

                            {/* INFO */}
                            <div className="receta-info">
                                <p>
                                    <strong>Farmacéutico:</strong>{" "}
                                    {receta.farmaceuticoUsername}
                                </p>

                                <p>
                                    <strong>Fecha:</strong>{" "}
                                    {formatearFecha(receta.fechaSubida)}
                                </p>

                                <p className="minsa-info">
                                    <strong>Código MINSA:</strong>
                                    <span className="minsa-tag">{receta.codigoMinsa || "N/A"}</span>
                                </p>

                                {receta.ventaId && (
                                    <p className="vinculada">
                                        🔗 Venta #{receta.ventaId}
                                    </p>
                                )}
                            </div>

                            {/* ACCIONES */}
                            {(esAdmin || user?.rol === "FARMACEUTICO") &&
                                receta.estado === "PENDIENTE" && (
                                    <div className="receta-actions">
                                        <button
                                            className="btn-aprobar"
                                            onClick={() => handleValidar(receta.id, true)}
                                            disabled={cargando}
                                        >
                                            ✅ Aprobar
                                        </button>

                                        <button
                                            className="btn-rechazar"
                                            onClick={() => handleValidar(receta.id, false)}
                                            disabled={cargando}
                                        >
                                            ❌ Rechazar
                                        </button>
                                    </div>
                                )}
                        </div>
                    ))}
                </div>

            </section>
        </div>
    );
}