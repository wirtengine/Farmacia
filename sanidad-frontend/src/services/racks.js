import axios from 'axios';

const API_URL = 'http://localhost:8080/api/racks';

const getToken = () => localStorage.getItem('token');

const authHeaders = () => ({
    headers: { Authorization: `Bearer ${getToken()}` }
});

// Obtener todos los racks activos
export const listarRacks = () => {
    return axios.get(API_URL, authHeaders());
};

// Obtener un rack por ID
export const obtenerRack = (id) => {
    return axios.get(`${API_URL}/${id}`, authHeaders());
};

// Crear un nuevo rack (solo admin)
export const crearRack = (rack) => {
    return axios.post(API_URL, rack, authHeaders());
};

// 🔥 ELIMINAR RACK
export const eliminarRack = (id) => {
    return axios.delete(`${API_URL}/${id}`, authHeaders());
};