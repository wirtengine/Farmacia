import axios from 'axios';

const API_URL = 'http://localhost:8080/api/recommendations';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const getRecommendations = () => {
    return axios.get(`${API_URL}`, authHeaders());
};

export const getPendingRecommendations = () => {
    return axios.get(`${API_URL}/pending`, authHeaders());
};

export const acceptRecommendation = (id) => {
    return axios.post(`${API_URL}/${id}/accept`, {}, authHeaders());
};

export const dismissRecommendation = (id) => {
    return axios.post(`${API_URL}/${id}/dismiss`, {}, authHeaders());
};

export const generateRecommendations = () => {
    return axios.post(`${API_URL}/generate`, {}, authHeaders());
};