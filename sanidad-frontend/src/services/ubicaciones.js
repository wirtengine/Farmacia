import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/ubicaciones`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const listarUbicacionesPorRack = (rackId) => {
    return axios.get(`${API_URL}/rack/${rackId}`, authHeaders());
};

export const listarTodasUbicaciones = () => {
    return axios.get(API_URL, authHeaders());
};

export const obtenerUbicacion = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

export const asignarUbicacion = (data) => {
    return axios.post(API_URL, data, authHeaders());
};

export const eliminarUbicacion = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};

export const listarUbicacionesPorLoteDetalle = (loteDetalleId) => {
    return axios.get(`${API_URL}/lote-detalle/${loteDetalleId}`, authHeaders());
};