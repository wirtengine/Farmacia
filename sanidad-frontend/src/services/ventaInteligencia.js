import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/usuarios`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const listarUsuarios = () => {
    return axios.get(API_URL, authHeaders());
};

export const crearUsuario = (usuario) => {
    return axios.post(API_URL, usuario, authHeaders());
};

export const obtenerUsuario = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

export const actualizarUsuario = (id, usuario) => {
    return axios.put(`${API_URL}/${id}`, usuario, authHeaders());
};