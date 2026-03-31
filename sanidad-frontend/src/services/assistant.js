import axios from 'axios';

const API_URL = 'http://localhost:8080/api/assistant';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const askAssistant = (query) => {
    return axios.post(`${API_URL}/ask`, { query }, authHeaders());
};