import axios from 'axios';
import authService from './authService';

axios.interceptors.request.use(
    config => {
        const token = authService.getToken();
        console.log('Interceptor ejecutado, token:', token); // <-- agregar
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

export default axios;