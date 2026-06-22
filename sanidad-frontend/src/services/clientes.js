import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/clientes`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const listarClientes = () => {
    return axios.get(API_URL, authHeaders());
};

export const obtenerCliente = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

export const crearCliente = (cliente) => {
    return axios.post(API_URL, cliente, authHeaders());
};

export const actualizarCliente = (id, cliente) => {
    return axios.put(`${API_URL}/${id}`, cliente, authHeaders());
};

export const desactivarCliente = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};