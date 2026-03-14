import axios from 'axios';

const API_URL = 'http://localhost:8080/api/usuarios';

// Obtener token del localStorage
const getToken = () => localStorage.getItem('token');

// Configurar headers con token
const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

// Listar usuarios
export const listarUsuarios = () => {
    return axios.get(API_URL, authHeaders());
};

// Crear usuario
export const crearUsuario = (usuario) => {
    return axios.post(API_URL, usuario, authHeaders());
};

// Obtener usuario por ID (para editar)
export const obtenerUsuario = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

// Actualizar usuario
export const actualizarUsuario = (id, usuario) => {
    return axios.put(`${API_URL}/${id}`, usuario, authHeaders());
};