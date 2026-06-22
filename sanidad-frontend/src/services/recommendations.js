import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/recommendations`;

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

export const getRecommendations = () => {
    return axios.get(API_URL, authHeaders());
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