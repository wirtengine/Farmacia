import axios from 'axios';

const API_URL = 'http://localhost:8080/api/alerts';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const getAlerts = () => {
    return axios.get(`${API_URL}`, authHeaders());
};

export const acknowledgeAlert = (id) => {
    return axios.post(`${API_URL}/${id}/acknowledge`, {}, authHeaders());
};

export const generateAlerts = () => {
    return axios.post(`${API_URL}/generate`, {}, authHeaders());
};