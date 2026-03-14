import axios from 'axios';

const API_URL = 'http://localhost:8080/api/medicamentos';

// Obtener token del localStorage
const getToken = () => localStorage.getItem('token');

// Configurar headers con token
const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

// Listar todos los medicamentos activos (accesible para ADMIN y VENDEDOR)
export const listarMedicamentos = () => {
    return axios.get(API_URL, authHeaders());
};

// Obtener un medicamento por ID
export const obtenerMedicamento = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

// Crear un nuevo medicamento (solo ADMIN)
export const crearMedicamento = (medicamento) => {
    return axios.post(API_URL, medicamento, authHeaders());
};

// Actualizar un medicamento (solo ADMIN)
export const actualizarMedicamento = (id, medicamento) => {
    return axios.put(`${API_URL}/${id}`, medicamento, authHeaders());
};

// Desactivar (eliminación lógica) un medicamento (solo ADMIN)
export const desactivarMedicamento = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};

// Reactivar un medicamento (solo ADMIN) - opcional
export const reactivarMedicamento = (id) => {
    return axios.patch(`${API_URL}/${id}/reactivar`, {}, authHeaders());
};