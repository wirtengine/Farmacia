import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/ventas`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const crearVenta = (venta) => {
    return axios.post(API_URL, venta, authHeaders());
};

export const listarVentas = () => {
    return axios.get(API_URL, authHeaders());
};

export const obtenerVenta = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

export const anularVenta = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};