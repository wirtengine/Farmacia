import axios from 'axios';

const API_URL = 'http://localhost:8080/api/devoluciones-proveedor';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

// Listar todas las devoluciones a proveedor (solo admin)
export const listarDevolucionesProveedor = () => {
    return axios.get(API_URL, authHeaders());
};

// Obtener una devolución por ID
export const obtenerDevolucionProveedor = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

// Solicitar nueva devolución a proveedor (solo admin)
export const solicitarDevolucionProveedor = (data) => {
    return axios.post(`${API_URL}/solicitar`, data, authHeaders());
};

// Aprobar o rechazar devolución (solo admin)
export const aprobarDevolucionProveedor = (data) => {
    return axios.put(`${API_URL}/aprobar`, data, authHeaders());
};