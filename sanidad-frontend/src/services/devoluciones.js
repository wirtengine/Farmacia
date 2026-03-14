import axios from 'axios';

const API_URL = 'http://localhost:8080/api/devoluciones';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

// Listar devoluciones (filtradas por rol automáticamente en backend)
export const listarDevoluciones = () => {
    return axios.get(API_URL, authHeaders());
};

// Obtener una devolución por ID
export const obtenerDevolucion = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

// Solicitar nueva devolución (vendedor/admin)
export const solicitarDevolucion = (data) => {
    return axios.post(`${API_URL}/solicitar`, data, authHeaders());
};

// Aprobar o rechazar devolución (solo admin)
export const aprobarDevolucion = (data) => {
    return axios.put(`${API_URL}/aprobar`, data, authHeaders());
};