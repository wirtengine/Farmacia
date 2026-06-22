import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/racks`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const listarRacks = () => {
    return axios.get(API_URL, authHeaders());
};

export const obtenerRack = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

export const crearRack = (rack) => {
    return axios.post(API_URL, rack, authHeaders());
};

export const eliminarRack = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};