import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/medicamentos`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const listarMedicamentos = () => {
    return axios.get(API_URL, authHeaders());
};

export const obtenerMedicamento = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

export const crearMedicamento = (medicamento) => {
    return axios.post(API_URL, medicamento, authHeaders());
};

export const actualizarMedicamento = (id, medicamento) => {
    return axios.put(`${API_URL}/${id}`, medicamento, authHeaders());
};

export const desactivarMedicamento = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};

export const reactivarMedicamento = (id) => {
    return axios.patch(`${API_URL}/${id}/reactivar`, {}, authHeaders());
};

export const subirImagenMedicamento = (id, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return axios.post(`${API_URL}/${id}/imagen`, formData, {
        headers: {
            ...authHeaders().headers,
            'Content-Type': 'multipart/form-data'
        }
    });
};