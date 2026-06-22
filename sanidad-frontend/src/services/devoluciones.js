import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/devoluciones`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const listarDevoluciones = () => {
    return axios.get(API_URL, authHeaders());
};

export const obtenerDevolucion = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

export const solicitarDevolucion = (data) => {
    return axios.post(`${API_URL}/solicitar`, data, authHeaders());
};

export const aprobarDevolucion = (data) => {
    return axios.put(`${API_URL}/aprobar`, data, authHeaders());
};