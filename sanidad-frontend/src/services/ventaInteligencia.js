import axios from 'axios';

const API_URL = 'http://localhost:8080/api/ventas/inteligencia';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const getLoteFIFO = (medicamentoId) => {
    return axios.get(`${API_URL}/fifo`, { params: { medicamentoId }, ...authHeaders() });
};

export const getComplementarios = (medicamentoId) => {
    return axios.get(`${API_URL}/complementarios`, { params: { medicamentoId }, ...authHeaders() });
};

export const getContextoCliente = (clienteId) => {
    return axios.get(`${API_URL}/contexto-cliente`, { params: { clienteId }, ...authHeaders() });
};

export const getVentaGuiada = (clienteId, medicamentoId) => {
    return axios.get(`${API_URL}/venta-guiada`, { params: { clienteId, medicamentoId }, ...authHeaders() });
};