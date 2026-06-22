import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/perdidas`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const getProductosVencidos = () => axios.get(`${API_URL}/vencidos`, authHeaders());
export const getProductosInmoviles = () => axios.get(`${API_URL}/inmoviles`, authHeaders());
export const getInconsistenciasStock = () => axios.get(`${API_URL}/inconsistencias`, authHeaders());
export const getResumenPerdidas = () => axios.get(`${API_URL}/resumen`, authHeaders());