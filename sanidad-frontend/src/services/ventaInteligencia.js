import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/ventas/inteligencia`;

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