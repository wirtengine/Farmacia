import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/proveedores`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const listarProveedores = () => {
    return axios.get(API_URL, authHeaders());
};

export const crearProveedor = (proveedor) => {
    return axios.post(API_URL, proveedor, authHeaders());
};

export const actualizarProveedor = (id, proveedor) => {
    return axios.put(`${API_URL}/${id}`, proveedor, authHeaders());
};

export const desactivarProveedor = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};