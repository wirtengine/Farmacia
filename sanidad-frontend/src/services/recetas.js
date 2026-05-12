import axios from 'axios';

const API_URL = 'http://localhost:8080/api/recetas';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: {
        Authorization: `Bearer ${getToken()}`
    }
});

// =========================================
// SUBIR RECETA (multipart/form-data)
// =========================================
export const uploadReceta = (file, farmaceuticoId, codigoMinsa) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('farmaceuticoId', farmaceuticoId);
    formData.append('codigoMinsa', codigoMinsa);

    return axios.post(`${API_URL}/upload`, formData, {
        headers: {
            ...authHeaders().headers,
            'Content-Type': 'multipart/form-data'
        }
    });
};

// =========================================
// VALIDAR / RECHAZAR RECETA
// =========================================
export const validarReceta = (recetaId, aprobar, farmaceuticoId) => {
    return axios.put(`${API_URL}/${recetaId}/validar`, null, {
        params: { aprobar, farmaceuticoId },
        ...authHeaders()
    });
};

// =========================================
// OBTENER UNA RECETA POR ID
// =========================================
export const obtenerReceta = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

// =========================================
// LISTADOS
// =========================================

// Recetas pendientes (admin)
export const listarPendientes = () => {
    return axios.get(`${API_URL}/pendientes`, authHeaders());
};

// Recetas por farmacéutico
export const listarPorFarmaceutico = (id) => {
    return axios.get(`${API_URL}/farmaceutico/${id}`, authHeaders());
};

// ✅ NUEVO: Recetas disponibles (validadas y no usadas en ventas)
export const listarRecetasDisponibles = () => {
    return axios.get(`${API_URL}/disponibles`, authHeaders());
};

// ✅ NUEVO: Historial completo (admin y farmacéutico con toggle)
export const listarTodas = () => {
    return axios.get(API_URL, authHeaders()); // GET /api/recetas
};