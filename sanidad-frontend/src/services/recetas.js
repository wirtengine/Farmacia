import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/recetas`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: {
        Authorization: `Bearer ${getToken()}`
    }
});

export const uploadReceta = (file, farmaceuticoId, codigoMinsa) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('farmaceuticoId', farmaceuticoId);
    formData.append('codigoMinsa', codigoMinsa);
    return axios.post(`${API_URL}/upload`, formData, {
        headers: {
            ...authHeaders().headers,
            'Content-Type': 'multipart/form-data'
        }
    });
};

export const validarReceta = (recetaId, aprobar, farmaceuticoId) => {
    return axios.put(`${API_URL}/${recetaId}/validar`, null, {
        params: { aprobar, farmaceuticoId },
        ...authHeaders()
    });
};

export const obtenerReceta = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

export const listarPendientes = () => {
    return axios.get(`${API_URL}/pendientes`, authHeaders());
};

export const listarPorFarmaceutico = (id) => {
    return axios.get(`${API_URL}/farmaceutico/${id}`, authHeaders());
};

export const listarRecetasDisponibles = () => {
    return axios.get(`${API_URL}/disponibles`, authHeaders());
};

export const listarTodas = () => {
    return axios.get(API_URL, authHeaders());
};