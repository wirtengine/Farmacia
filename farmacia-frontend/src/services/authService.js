import axios from 'axios';

const API_URL = 'http://localhost:8080/api/auth';

class AuthService {
    login(username, password) {
        return axios.post(API_URL + '/login', { username, password });
    }

    register(username, password, nombre, apellido, rol) {
        return axios.post(API_URL + '/register', { username, password, nombre, apellido, rol });
    }

    setAuthData(token, rol, nombre) {
        localStorage.setItem('token', token);
        localStorage.setItem('rol', rol);
        localStorage.setItem('nombre', nombre);
    }

    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('rol');
        localStorage.removeItem('nombre');
    }

    getToken() {
        return localStorage.getItem('token');
    }

    getRol() {
        return localStorage.getItem('rol');
    }

    getNombre() {
        return localStorage.getItem('nombre');
    }

    isAuthenticated() {
        return !!this.getToken();
    }
}

export default new AuthService();