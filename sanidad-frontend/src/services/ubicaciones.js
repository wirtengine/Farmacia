import axios from 'axios';

const API_URL = 'http://localhost:8080/api/ubicaciones';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

// Obtener todas las ubicaciones de un rack
export const listarUbicacionesPorRack = (rackId) => {
    return axios.get(`${API_URL}/rack/${rackId}`, authHeaders());
};

// Obtener todas las ubicaciones de todos los racks (para cálculo global)
export const listarTodasUbicaciones = () => {
    return axios.get(API_URL, authHeaders()); // Asumiendo que el backend tenga un endpoint GET sin filtro
};

// Obtener una ubicación por ID
export const obtenerUbicacion = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

// Asignar un lote a una ubicación (crear nueva ubicación)
export const asignarUbicacion = (data) => {
    return axios.post(API_URL, data, authHeaders());
};

// Eliminar (desactivar) una ubicación
export const eliminarUbicacion = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};

// Obtener ubicaciones de un lote específico
export const listarUbicacionesPorLoteDetalle = (loteDetalleId) => {
    return axios.get(
        `${API_URL}/lote-detalle/${loteDetalleId}`,
        authHeaders()
    );
};