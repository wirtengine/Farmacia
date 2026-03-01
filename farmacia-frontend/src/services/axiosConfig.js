import axios from 'axios';
import authService from './authService';

// Crear una instancia con la URL base del backend
const instance = axios.create({
    baseURL: 'http://localhost:8080' // <-- AGREGADO
});

instance.interceptors.request.use(
    config => {
        const token = authService.getToken();
        console.log('Interceptor ejecutado, token:', token);
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

export default instance; // Exportamos la instancia, no el axios global