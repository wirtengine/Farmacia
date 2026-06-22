import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/assistant`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const askAssistant = (query) => {
    return axios.post(`${API_URL}/ask`, { query }, authHeaders());
};