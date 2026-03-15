import axios from 'axios';

const API_URL = 'http://localhost:8080/api/dashboard';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const obtenerResumenDashboard = () => {
    return axios.get(`${API_URL}/resumen`, authHeaders());
};