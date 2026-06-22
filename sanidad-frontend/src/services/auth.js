import axios from 'axios';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/auth`;

export const login = (username, password) => {
    return axios.post(`${API_URL}/login`, { username, password });
};