import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/dashboard`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const obtenerResumenDashboard = () => {
    return axios.get(`${API_URL}/resumen`, authHeaders());
};