import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/alerts`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const getAlerts = () => {
    return axios.get(API_URL, authHeaders());
};

export const acknowledgeAlert = (id) => {
    return axios.post(`${API_URL}/${id}/acknowledge`, {}, authHeaders());
};

export const generateAlerts = () => {
    return axios.post(`${API_URL}/generate`, {}, authHeaders());
};