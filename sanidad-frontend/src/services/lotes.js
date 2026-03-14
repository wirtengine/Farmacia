import axios from 'axios';

const API_URL = 'http://localhost:8080/api/lotes';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const listarLotes = () => {
    return axios.get(API_URL, authHeaders());
};

export const obtenerLote = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

export const crearLote = (lote) => {
    return axios.post(API_URL, lote, authHeaders());
};

export const actualizarLote = (id, lote) => {
    return axios.put(`${API_URL}/${id}`, lote, authHeaders());
};

export const desactivarLote = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};